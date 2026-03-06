package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubCodePolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 관리 유스케이스
 * 생성은 누구나 가능하지만 그 외 작업은 관리자만 가능
 */
@Service
class ManageClubUseCase(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMapper: ClubMapper,
) {
    /**
     * 새로운 동아리를 생성
     * 생성자는 자동으로 LEAD 권한 설정
     * 동아리 생성은 관리자 권한이 필요 없음
     * todo: 기수 관련 설정 필수 처리
     */
    @Transactional
    fun create(
        userId: Long,
        request: ClubCreateRequest,
    ) {
        val user =
            userReader.getById(userId)

        val code = ClubCodePolicy.generateCode()
        val clubContact =
            ClubContact.from(
                email = request.contactEmail,
                phoneNumber = request.contactPhoneNumber,
            )

        val club =
            Club.create(
                name = request.name,
                code = code,
                schoolName = request.schoolName,
                clubContact = clubContact,
                description = request.description,
                profileImageUrl = request.profileImageUrl,
                backgroundImageUrl = request.backgroundImageUrl,
            )

        clubRepository.save(club)

        val leadMember =
            ClubMember
                .create(
                    club = club,
                    user = user,
                    memberRole = MemberRole.LEAD,
                ).apply {
                    accept()
                }

        clubMemberRepository.save(leadMember)
    }

    @Transactional
    fun update(
        clubId: Long,
        userId: Long,
        request: ClubUpdateRequest,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)

        club.update(
            name = request.name ?: club.name,
            description = request.description,
        )

        club.updateContact(
            email = request.contactEmail,
            phoneNumber = request.contactPhoneNumber,
        )

        club.updateImages(
            request.profileImageUrl,
            request.backgroundImageUrl,
        )
    }

    @Transactional
    fun regenerateCode(
        clubId: Long,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)
        val newCode = ClubCodePolicy.generateCode()
        club.regenerateCode(newCode)
    }
}
