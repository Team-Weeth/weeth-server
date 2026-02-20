package com.weeth.domain.user.domain.converter

import com.weeth.domain.user.domain.vo.PhoneNumber
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class PhoneNumberConverter : AttributeConverter<PhoneNumber, String> {
    override fun convertToDatabaseColumn(attribute: PhoneNumber?): String = attribute?.value ?: ""

    override fun convertToEntityAttribute(dbData: String?): PhoneNumber = PhoneNumber.from(dbData ?: "")
}
