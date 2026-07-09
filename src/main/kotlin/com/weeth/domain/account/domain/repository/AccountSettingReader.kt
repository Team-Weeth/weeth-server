package com.weeth.domain.account.domain.repository

/**
 * 회비 기능 노출 여부의 타 도메인 조회용 Reader.
 * 설정 행이 없으면 미공개(false)로 간주한다.
 */
interface AccountSettingReader {
    fun isVisibleToMembers(clubId: Long): Boolean
}
