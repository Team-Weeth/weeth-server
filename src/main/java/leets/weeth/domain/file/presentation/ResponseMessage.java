package leets.weeth.domain.file.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    PRESIGNED_URL_GET_SUCCESS("Presigned Url 반환에 성공했습니다");

    private final String message;

}
