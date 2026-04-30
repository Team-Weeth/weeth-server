package com.weeth.domain.user.domain.converter

import com.weeth.domain.user.domain.vo.Email
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class EmailConverter : AttributeConverter<Email, String> {
    override fun convertToDatabaseColumn(attribute: Email?): String = attribute?.value ?: ""

    override fun convertToEntityAttribute(dbData: String?): Email = Email.from(dbData ?: "")
}
