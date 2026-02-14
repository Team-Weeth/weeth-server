package leets.weeth.global.config;

import leets.weeth.global.sas.application.converter.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;

@Configuration
@EnableRedisRepositories(enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP) // redis index ttl 설정
public class RedisConfig {

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();

        redisConfiguration.setHostName(host);
        redisConfiguration.setPort(port);
        redisConfiguration.setPassword(password);

        return new LettuceConnectionFactory(redisConfiguration);
    }

    @Bean
    public RedisCustomConversions redisCustomConversions() {
        return new RedisCustomConversions(
                Arrays.asList(
                new UsernamePasswordAuthenticationTokenToBytesConverter(),
                new BytesToUsernamePasswordAuthenticationTokenConverter(),
                new OAuth2AuthorizationRequestToBytesConverter(),
                new BytesToOAuth2AuthorizationRequestConverter(),
                new ClaimsHolderToBytesConverter(),
                new BytesToClaimsHolderConverter()));
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        return redisTemplate;
    }

}
