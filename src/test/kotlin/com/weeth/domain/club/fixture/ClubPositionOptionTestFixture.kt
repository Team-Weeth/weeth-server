package com.weeth.domain.club.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubPositionOption
import com.weeth.domain.club.domain.enums.PositionColor
import org.springframework.test.util.ReflectionTestUtils

object ClubPositionOptionTestFixture {
    fun createOption(
        id: Long = 0L,
        club: Club = ClubTestFixture.createClub(),
        name: String = "백엔드",
        color: PositionColor = PositionColor.PRIMARY,
        displayOrder: Int = 0,
    ): ClubPositionOption {
        val option =
            ClubPositionOption.create(
                club = club,
                name = name,
                color = color,
                displayOrder = displayOrder,
            )
        if (id != 0L) ReflectionTestUtils.setField(option, "id", id)
        return option
    }
}
