package com.weeth.domain.university.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.application.exception.CareerNetApiException
import com.weeth.domain.university.domain.port.CareerNetPort
import com.weeth.global.config.properties.CareerNetProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class CareerNetAdapter(
    private val properties: CareerNetProperties,
    restClientBuilder: RestClient.Builder,
) : CareerNetPort {
    private val restClient =
        restClientBuilder
            .baseUrl(properties.baseUrl)
            .build()

    companion object {
        private const val SVC_TYPE = "api"
        private const val GUBUN = "univ_list"
        private const val CONTENT_TYPE = "json"
        private const val PER_PAGE = 100
        private val log = LoggerFactory.getLogger(CareerNetAdapter::class.java)
    }

    override fun getSchools(): List<SchoolResponse> {
        val firstPage = fetchSchoolPage(1)
        val totalCount = firstPage.firstOrNull()?.totalCount?.toIntOrNull() ?: 0
        val totalPages = ((totalCount + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)

        val all = firstPage.toMutableList()
        for (page in 2..totalPages) {
            all.addAll(fetchSchoolPage(page))
        }
        return all.map { SchoolResponse(it.schoolName, it.region) }
    }

    override fun getMajors(): List<MajorResponse> {
        val firstPage = fetchMajorPage(1)
        val totalCount = firstPage.firstOrNull()?.totalCount?.toIntOrNull() ?: 0
        val totalPages = ((totalCount + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)

        val all = firstPage.toMutableList()
        for (page in 2..totalPages) {
            all.addAll(fetchMajorPage(page))
        }
        return all.map { MajorResponse(it.mClass, it.lClass) }
    }

    private fun fetchSchoolPage(page: Int): List<CareerNetSchoolItem> =
        runCatching {
            restClient
                .get()
                .uri { builder ->
                    builder
                        .queryParam("apiKey", properties.key)
                        .queryParam("contentType", CONTENT_TYPE)
                        .queryParam("svcType", SVC_TYPE)
                        .queryParam("svcCode", "SCHOOL")
                        .queryParam("gubun", GUBUN)
                        .queryParam("thisPage", page)
                        .queryParam("perPage", PER_PAGE)
                        .build()
                }.retrieve()
                .body(object : ParameterizedTypeReference<CareerNetResponse<CareerNetSchoolItem>>() {})
                ?.dataSearch
                ?.content
                ?: emptyList()
        }.getOrElse { e ->
            log.error("커리어넷 학교 목록 조회 실패", e)
            throw CareerNetApiException()
        }

    private fun fetchMajorPage(page: Int): List<CareerNetMajorItem> =
        runCatching {
            restClient
                .get()
                .uri { builder ->
                    builder
                        .queryParam("apiKey", properties.key)
                        .queryParam("contentType", CONTENT_TYPE)
                        .queryParam("svcType", SVC_TYPE)
                        .queryParam("svcCode", "MAJOR")
                        .queryParam("gubun", GUBUN)
                        .queryParam("thisPage", page)
                        .queryParam("perPage", PER_PAGE)
                        .build()
                }.retrieve()
                .body(object : ParameterizedTypeReference<CareerNetResponse<CareerNetMajorItem>>() {})
                ?.dataSearch
                ?.content
                ?: emptyList()
        }.getOrElse { e ->
            log.error("커리어넷 학과 목록 조회 실패", e)
            throw CareerNetApiException()
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CareerNetResponse<T>(
    val dataSearch: DataSearch<T>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class DataSearch<T>(
    val content: List<T>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CareerNetSchoolItem(
    val schoolName: String,
    val region: String,
    val totalCount: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CareerNetMajorItem(
    val lClass: String,
    val mClass: String,
    val totalCount: String,
)
