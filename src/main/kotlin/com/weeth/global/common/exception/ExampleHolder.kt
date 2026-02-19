package com.weeth.global.common.exception

import io.swagger.v3.oas.models.examples.Example

data class ExampleHolder(
    val holder: Example,
    val name: String,
    val code: Int,
)
