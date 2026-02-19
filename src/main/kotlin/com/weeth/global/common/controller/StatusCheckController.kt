package com.weeth.global.common.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class StatusCheckController {
    @GetMapping("/health-check")
    fun checkHealthStatus(): ResponseEntity<Void> = ResponseEntity.ok().build()
}
