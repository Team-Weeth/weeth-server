package com.weeth.domain.user.domain.service

import com.weeth.domain.user.application.exception.UserNotFoundException
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.util.Optional

class UserGetServiceTest :
    DescribeSpec({

        val userRepository = mockk<UserRepository>()
        val userGetService = UserGetService(userRepository)

        describe("find(Long)") {
            context("존재하지 않는 유저일 때") {
                it("예외를 던진다") {
                    val userId = 1L
                    every { userRepository.findById(userId) } returns Optional.empty()

                    shouldThrow<UserNotFoundException> {
                        userGetService.find(userId)
                    }
                }
            }
        }

        describe("find(String)") {
            context("존재하지 않는 유저일 때") {
                it("예외를 던진다") {
                    val email = "test@test.com"
                    every { userRepository.findByEmail(email) } returns Optional.empty()

                    shouldThrow<UserNotFoundException> {
                        userGetService.find(email)
                    }
                }
            }
        }

        describe("findAll(Pageable)") {
            context("빈 슬라이스 반환 시") {
                it("유저 예외를 던진다") {
                    val pageable = PageRequest.of(0, 10)
                    val emptySlice = SliceImpl<User>(listOf(), pageable, false)

                    every {
                        userRepository.findAllByStatusOrderedByCardinalAndName(any(), eq(pageable))
                    } returns emptySlice

                    shouldThrow<UserNotFoundException> {
                        userGetService.findAll(pageable)
                    }
                }
            }
        }
    })
