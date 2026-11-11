package com.weeth.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 왁뿌볼 실시간 서버 설정. 상태가 메모리에만 있는 단일 인스턴스 전제라 한도는 인스턴스 크기에 맞춰 환경별로 정한다.
 * 기본값은 로컬 프로토타입 값이다.
 */
@ConfigurationProperties(prefix = "weeth.wax")
data class WaxProperties(
    /** false면 운영 앱에 `/ws/wax`를 등록하지 않는다. */
    val enabled: Boolean = false,
    // 브라우저 Origin 허용 패턴. 하위 도메인 와일드카드(https://*.v4.weeth.kr) 형식도 쓸 수 있다.
    val allowedOrigins: List<String> =
        listOf("http://localhost:3103", "http://127.0.0.1:3103", "http://localhost:3000", "http://127.0.0.1:3000"),
    val maxConnections: Int = 240,
    val maxWorlds: Int = 20,
    val maxPlayersPerWorld: Int = 24,
    /** 기본은 제한 없음(로컬 프로토타입). 운영 프로필에서 정한다. */
    val maxConnectionsPerIp: Int = Int.MAX_VALUE,
    /** 상태 브로드캐스트 주기. 게임 tick(50ms)보다 길게 잡아 전송량을 줄인다. */
    val broadcastIntervalMs: Long = 50,
    /**
     * 프록시(Caddy)가 넣은 `X-Forwarded-For` 첫 값을 클라이언트 IP로 믿을지. 앱 포트가 프록시에만 열려 있을 때만 켠다.
     * Caddy는 trusted_proxies 설정이 없으면 들어온 헤더를 버리고 실제 접속 IP로 다시 채운다.
     */
    val trustForwardedFor: Boolean = false,
    /**
     * 이 시간 동안 플레이 입력(이동·착석·누르기·표현·`active`)이 없으면 교실에서 내보낸다. heartbeat는 입력으로 치지 않는다.
     * 열어만 둔 탭이 자리와 송신량(연결당 약 20KB/s)을 계속 차지하지 않게 한다.
     */
    val idleTimeoutMs: Long = 300_000,
)
