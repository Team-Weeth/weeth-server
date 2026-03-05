package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class ClubMemberNotFoundException : BaseException(ClubErrorCode.CLUB_MEMBER_NOT_FOUND)
