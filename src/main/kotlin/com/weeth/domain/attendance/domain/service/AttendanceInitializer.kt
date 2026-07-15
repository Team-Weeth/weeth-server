package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service

@Service
class AttendanceInitializer(
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
) {
    fun initializeForMemberCardinals(
        clubId: Long,
        member: ClubMember,
        cardinals: List<Cardinal>,
    ) {
        val cardinalNumbers = cardinals.map { it.cardinalNumber }.distinct()
        if (cardinalNumbers.isEmpty()) return

        val sessions = sessionReader.findAllByClubIdAndCardinalIn(clubId, cardinalNumbers)
        if (sessions.isEmpty()) return

        attendanceRepository.saveAll(sessions.map { Attendance.create(session = it, clubMember = member) })
    }
}
