package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
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
    private val cardinalRepository: CardinalRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    /**
     * 새로운 동아리를 생성
     * 생성자는 자동으로 LEAD 권한 설정
     * 1기부터 currentCardinal기까지 Cardinal을 자동 생성하고, LEAD를 최신 기수에 배정
     */
    @Transactional
    fun create(
        userId: Long,
        request: ClubCreateRequest,
    ) {
        clubMemberPolicy.validateCreateLimit(userId)

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

        // 1기 - currentCardinal기까지 Cardinal 자동 생성
        val cardinals =
            (1..request.currentCardinal).map { number ->
                Cardinal.create(
                    club = club,
                    cardinalNumber = number,
                    status = if (number == request.currentCardinal) CardinalStatus.IN_PROGRESS else CardinalStatus.DONE,
                )
            }

        cardinalRepository.saveAll(cardinals)

        // LEAD 멤버를 최신 기수에 배정
        clubMemberCardinalRepository.save(ClubMemberCardinal.create(leadMember, cardinals.last()))
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
            name = request.name,
            schoolName = request.schoolName,
            description = request.description,
            contactEmail = request.contactEmail,
            contactPhoneNumber = request.contactPhoneNumber,
            profileImageUrl = request.profileImageUrl,
            backgroundImageUrl = request.backgroundImageUrl,
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

    @Transactional
    fun deleteProfileImage(
        clubId: Long,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)
        club.removeProfileImage()
    }

    @Transactional
    fun deleteBackgroundImage(
        clubId: Long,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)
        club.removeBackgroundImage()
    }
}
