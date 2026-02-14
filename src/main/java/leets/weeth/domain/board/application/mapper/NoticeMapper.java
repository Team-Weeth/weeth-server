package leets.weeth.domain.board.application.mapper;

import leets.weeth.domain.board.application.dto.NoticeDTO;
import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.comment.application.dto.CommentDTO;
import leets.weeth.domain.comment.application.mapper.CommentMapper;
import leets.weeth.domain.file.application.dto.response.FileResponse;
import leets.weeth.domain.user.domain.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = CommentMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NoticeMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "user", source = "user")
    })
    Notice fromNoticeDto(NoticeDTO.Save dto, User user);

    @Mappings({
            @Mapping(target = "name", source = "notice.user.name"),
            @Mapping(target = "position", source = "notice.user.position"),
            @Mapping(target = "role", source = "notice.user.role"),
            @Mapping(target = "time", source = "notice.createdAt"),
            @Mapping(target = "hasFile", expression = "java(fileExists)")
    })
    NoticeDTO.ResponseAll toAll(Notice notice, boolean fileExists);

    @Mappings({
            @Mapping(target = "name", source = "notice.user.name"),
            @Mapping(target = "position", source = "notice.user.position"),
            @Mapping(target = "role", source = "notice.user.role"),
            @Mapping(target = "time", source = "notice.createdAt"),
            @Mapping(target = "comments", source = "comments")
    })
    NoticeDTO.Response toNoticeDto(Notice notice, List<FileResponse> fileUrls, List<CommentDTO.Response> comments);

    NoticeDTO.SaveResponse  toSaveResponse(Notice notice);

}
