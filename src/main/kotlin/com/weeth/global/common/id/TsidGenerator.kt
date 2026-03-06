package com.weeth.global.common.id

import io.hypersistence.tsid.TSID

/**
 * TSID (Time-Sorted Unique Identifier) 생성 유틸리티.
 * 참고: 애플리케이션 단에서 ID를 할당하므로, 생성된 ID는 테스트에서 ReflectionTestUtils로 덮어씌울 수 있음.
 */
object TsidGenerator {
    /**
     * 새로운 TSID를 생성하여 Long 값으로 반환합니다.
     */
    fun nextId(): Long = TSID.Factory.getTsid().toLong()
}
