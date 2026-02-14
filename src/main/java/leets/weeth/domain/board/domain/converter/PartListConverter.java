package leets.weeth.domain.board.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import leets.weeth.domain.board.domain.entity.enums.Part;

@Converter
public class PartListConverter implements AttributeConverter<List<Part>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<Part> parts) {

        return parts.stream()
                .map(Part::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<Part> convertToEntityAttribute(String dbData) {

        return Arrays.stream(dbData.split(DELIMITER))
                .map(Part::valueOf)
                .collect(Collectors.toList());
    }
}
