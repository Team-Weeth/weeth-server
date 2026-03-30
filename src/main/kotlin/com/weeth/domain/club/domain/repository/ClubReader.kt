package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.Club

interface ClubReader {
    fun getClubById(clubId: Long): Club

    fun getClubByIdForUpdate(clubId: Long): Club

    fun findByIdOrNull(clubId: Long): Club?

    fun findClubByCode(code: String): Club?

    fun findClubByName(name: String): Club?
}
