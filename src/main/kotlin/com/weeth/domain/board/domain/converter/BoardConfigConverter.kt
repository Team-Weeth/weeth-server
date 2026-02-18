package com.weeth.domain.board.domain.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.global.common.converter.JsonConverter
import jakarta.persistence.Converter

@Converter
class BoardConfigConverter : JsonConverter<BoardConfig>(object : TypeReference<BoardConfig>() {})
