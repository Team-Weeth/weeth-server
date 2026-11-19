package com.weeth.domain.minigame.domain.vo

import kotlin.random.Random

/**
 * 교실에서 보이는 이름. 직접 입력은 이름표 폭에 맞춰 2~8자의 한글(완성형)·영문·숫자만 받는다.
 * 서버가 추천한 후보는 공백이 있고 더 길 수 있지만, 모달에서 "랜덤 그대로"를 고르면 그대로 돌아오므로 허용한다.
 */
@JvmInline
value class WaxNickname(
    val value: String,
) {
    init {
        require(value in CANDIDATES || (value.length in MIN_LENGTH..MAX_LENGTH && ALLOWED.matches(value))) {
            "닉네임은 ${MIN_LENGTH}~${MAX_LENGTH}자의 한글·영문·숫자로 정해주세요."
        }
    }

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 8
        private val ALLOWED = Regex("^[가-힣a-zA-Z0-9]+$")

        /** 학교·강의실 콘셉트의 추천 닉네임 후보. */
        val CANDIDATES =
            listOf(
                "졸고 있는 새내기",
                "당당한 지각생",
                "수상한 복학생",
                "용감한 청강생",
                "배고픈 대학원생",
                "억울한 재수강생",
                "딴짓하는 장학생",
                "조용한 수강생",
                "뻔뻔한 지각생",
                "은밀한 지각생",
                "대담한 새내기",
                "들킬 뻔한 복학생",
                "긴장한 장학생",
                "수상한 뒷자리생",
                "눈치 빠른 신입생",
                "몰래 노는 과대표",
                "겁 없는 재수강생",
                "학점 잃은 새내기",
                "출석만 하는 복학생",
                "과제 밀린 복학생",
                "종강 찾는 대학생",
                "학식 먹는 지각생",
                "출튀하는 복학생",
                "밤샌 대학원생",
                "학점 줍는 재수강생",
                "은밀한 복학생",
                "수상한 새내기",
                "대담한 지각생",
            )

        fun random(random: Random = Random.Default) = WaxNickname(CANDIDATES[random.nextInt(CANDIDATES.size)])
    }
}
