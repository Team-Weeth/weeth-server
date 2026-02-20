package com.weeth.global.auth.apple

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.weeth.global.auth.apple.dto.ApplePublicKey
import com.weeth.global.auth.apple.dto.ApplePublicKeys
import com.weeth.global.auth.apple.dto.AppleTokenResponse
import com.weeth.global.auth.apple.dto.AppleUserInfo
import com.weeth.global.auth.apple.exception.AppleAuthenticationException
import com.weeth.global.config.properties.OAuthProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

@Service
class AppleAuthService(
    oAuthProperties: OAuthProperties,
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) {
    private data class CachedKeys(
        val keys: ApplePublicKeys,
        val expiresAt: Instant,
    )

    private val log = LoggerFactory.getLogger(javaClass)

    private val appleProperties = oAuthProperties.apple
    private val restClient = restClientBuilder.build()
    private val publicKeysTtl: Duration = Duration.ofHours(1)

    @Volatile private var cached: CachedKeys? = null
    private val privateKey: PrivateKey by lazy { loadPrivateKey() }

    fun getAppleToken(authCode: String): AppleTokenResponse {
        val clientSecret = generateClientSecret()

        val body =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", appleProperties.clientId)
                add("client_secret", clientSecret)
                add("code", authCode)
                add("redirect_uri", appleProperties.redirectUri)
            }

        return requireNotNull(
            restClient
                .post()
                .uri(appleProperties.tokenUri)
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body<AppleTokenResponse>(),
        )
    }

    fun verifyAndDecodeIdToken(idToken: String): AppleUserInfo {
        try {
            val tokenParts = idToken.split(".")
            if (tokenParts.size < 2) {
                throw AppleAuthenticationException()
            }
            val header = decodeBase64Url(tokenParts[0])
            val headerJson = parseJson(header)
            val kid = headerJson["kid"]?.asText()?.takeIf { it.isNotBlank() } ?: throw AppleAuthenticationException()
            val alg = headerJson["alg"]?.asText()
            if (alg != "RS256") {
                throw AppleAuthenticationException()
            }

            val publicKeys = getApplePublicKeys()

            val matchedKey =
                publicKeys.keys
                    .firstOrNull { key -> key.kid == kid }
                    ?: throw AppleAuthenticationException()

            val publicKey = generatePublicKey(matchedKey)
            val claims =
                Jwts
                    .parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .payload

            validateClaims(claims)

            val appleId = claims.subject
            val email = claims.get("email", String::class.java)
            val emailVerified = parseEmailVerified(claims["email_verified"])
            val name = claims.get("name", String::class.java)

            return AppleUserInfo(
                appleId = appleId,
                email = email,
                emailVerified = emailVerified,
                name = name,
            )
        } catch (e: AppleAuthenticationException) {
            throw e
        } catch (e: Exception) {
            log.error("애플 ID Token 검증 실패", e)
            throw AppleAuthenticationException()
        }
    }

    private fun generateClientSecret(): String {
        try {
            val now = Instant.now(clock)
            val expiration = now.plus(Duration.ofDays(150)) // Apple limit is <= 6 months.

            return Jwts
                .builder()
                .header()
                .keyId(appleProperties.keyId)
                .and()
                .issuer(appleProperties.teamId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .audience()
                .add("https://appleid.apple.com")
                .and()
                .subject(appleProperties.clientId)
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact()
        } catch (e: Exception) {
            log.error("애플 Client Secret 생성 실패", e)
            throw AppleAuthenticationException()
        }
    }

    private fun loadPrivateKey(): PrivateKey =
        try {
            getInputStream(appleProperties.privateKeyPath).use { inputStream ->
                var privateKeyContent = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                privateKeyContent =
                    privateKeyContent
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replace("\\s".toRegex(), "")

                val keyBytes = Base64.getDecoder().decode(privateKeyContent)
                val keyFactory = KeyFactory.getInstance("EC")
                keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
            }
        } catch (e: Exception) {
            log.error("애플 개인키 로드 실패", e)
            throw AppleAuthenticationException()
        }

    @Throws(IOException::class)
    private fun getInputStream(path: String): InputStream =
        if (path.startsWith("/") || path.matches(Regex("^[A-Za-z]:.*"))) {
            FileInputStream(path)
        } else {
            ClassPathResource(path).inputStream
        }

    private fun generatePublicKey(applePublicKey: ApplePublicKey): PublicKey =
        try {
            val nBytes = Base64.getUrlDecoder().decode(applePublicKey.n)
            val eBytes = Base64.getUrlDecoder().decode(applePublicKey.e)

            val n = BigInteger(1, nBytes)
            val e = BigInteger(1, eBytes)

            val publicKeySpec = RSAPublicKeySpec(n, e)
            val keyFactory = KeyFactory.getInstance("RSA")

            keyFactory.generatePublic(publicKeySpec)
        } catch (ex: Exception) {
            log.error("애플 공개키 생성 실패", ex)
            throw AppleAuthenticationException()
        }

    private fun validateClaims(claims: Claims) {
        val iss = claims.issuer
        val audiences = claims.audience
        val expiration = claims.expiration
        val now = Date.from(Instant.now(clock))

        when {
            iss != "https://appleid.apple.com" -> {
                log.warn("유효하지 않은 발급자: {}", iss)
                throw AppleAuthenticationException()
            }

            audiences.isEmpty() || !audiences.contains(appleProperties.clientId) -> {
                log.warn("유효하지 않은 audience: {}. 기대값: {}", audiences, appleProperties.clientId)
                throw AppleAuthenticationException()
            }

            expiration.before(now) -> {
                log.warn("만료된 ID Token")
                throw AppleAuthenticationException()
            }

            claims.subject.isNullOrBlank() -> {
                log.warn("유효하지 않은 subject")
                throw AppleAuthenticationException()
            }
        }
    }

    private fun getApplePublicKeys(): ApplePublicKeys {
        val now = Instant.now(clock)
        cached?.let {
            if (now.isBefore(it.expiresAt)) {
                return it.keys
            }
        }

        val fetched =
            requireNotNull(
                restClient
                    .get()
                    .uri(appleProperties.keysUri)
                    .retrieve()
                    .body<ApplePublicKeys>(),
            )

        cached = CachedKeys(fetched, now.plus(publicKeysTtl))
        return fetched
    }

    private fun parseJson(json: String): ObjectNode =
        try {
            objectMapper.readTree(json) as? ObjectNode ?: throw AppleAuthenticationException()
        } catch (e: Exception) {
            throw AppleAuthenticationException()
        }

    private fun decodeBase64Url(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun parseEmailVerified(raw: Any?): Boolean =
        when (raw) {
            is Boolean -> raw
            is String -> raw.toBooleanStrictOrNull() ?: false
            else -> false
        }
}
