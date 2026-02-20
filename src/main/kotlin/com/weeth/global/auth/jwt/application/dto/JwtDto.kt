package com.weeth.global.auth.jwt.application.dto

data class JwtDto(
    val accessToken: String,
    val refreshToken: String,
)
