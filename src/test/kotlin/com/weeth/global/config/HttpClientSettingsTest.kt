package com.weeth.global.config

import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.Executors

/**
 * application.yml의 `spring.http.client.*` 설정이 자동 구성된 RestClient.Builder에 적용되는지 검증한다.
 * 외부 연동 어댑터는 모두 이 Builder를 주입받으므로 여기서 한 번만 확인한다.
 */
class HttpClientSettingsTest :
    DescribeSpec({
        val server =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                executor = Executors.newCachedThreadPool()
                createContext("/slow") { exchange ->
                    Thread.sleep(2_000)
                    exchange.sendResponseHeaders(200, -1)
                    exchange.close()
                }
                start()
            }
        afterSpec { server.stop(0) }

        val contextRunner =
            ApplicationContextRunner()
                .withInitializer { context ->
                    YamlPropertySourceLoader()
                        .load("application", ClassPathResource("application.yml"))
                        .forEach { context.environment.propertySources.addLast(it) }
                }.withConfiguration(
                    AutoConfigurations.of(
                        HttpClientAutoConfiguration::class.java,
                        HttpMessageConvertersAutoConfiguration::class.java,
                        RestClientAutoConfiguration::class.java,
                    ),
                )

        describe("application.yml의 HTTP 클라이언트 설정") {
            it("Apache HttpClient 5에 연결 3초·읽기 5초 타임아웃을 적용한다") {
                contextRunner.run { context ->
                    context
                        .getBean(ClientHttpRequestFactoryBuilder::class.java)
                        .shouldBeInstanceOf<HttpComponentsClientHttpRequestFactoryBuilder>()

                    val settings = context.getBean(ClientHttpRequestFactorySettings::class.java)
                    settings.connectTimeout() shouldBe Duration.ofSeconds(3)
                    settings.readTimeout() shouldBe Duration.ofSeconds(5)
                }
            }
        }

        describe("자동 구성된 RestClient.Builder") {
            it("읽기 타임아웃을 넘긴 응답은 ResourceAccessException으로 끊는다") {
                contextRunner
                    .withPropertyValues("spring.http.client.read-timeout=300ms")
                    .run { context ->
                        val restClient = context.getBean(RestClient.Builder::class.java).build()

                        shouldThrow<ResourceAccessException> {
                            restClient
                                .get()
                                .uri("http://127.0.0.1:${server.address.port}/slow")
                                .retrieve()
                                .toBodilessEntity()
                        }
                    }
            }
        }
    })
