package com.weeth.domain.user.application.usecase;

import jakarta.transaction.Transactional;
import com.weeth.domain.attendance.domain.service.AttendanceSaveService;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.user.application.exception.InvalidUserOrderException;
import com.weeth.domain.user.application.mapper.UserMapper;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.UserCardinal;
import com.weeth.domain.user.domain.entity.enums.StatusPriority;
import com.weeth.domain.user.domain.entity.enums.UsersOrderBy;
import com.weeth.domain.user.domain.service.*;
import com.weeth.global.auth.jwt.service.JwtRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.weeth.domain.user.application.dto.request.UserRequestDto.*;
import static com.weeth.domain.user.application.dto.response.UserResponseDto.AdminResponse;
import static com.weeth.domain.user.domain.entity.enums.UsersOrderBy.CARDINAL_DESCENDING;
import static com.weeth.domain.user.domain.entity.enums.UsersOrderBy.NAME_ASCENDING;

@Service
@RequiredArgsConstructor
public class UserManageUseCaseImpl implements UserManageUseCase {

    private final UserGetService userGetService;
    private final UserUpdateService userUpdateService;
    private final UserDeleteService userDeleteService;

    private final AttendanceSaveService attendanceSaveService;
    private final MeetingGetService meetingGetService;
    private final JwtRedisService jwtRedisService;
    private final CardinalGetService cardinalGetService;
    private final UserCardinalSaveService userCardinalSaveService;
    private final UserCardinalGetService userCardinalGetService;

    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<AdminResponse> findAllByAdmin(UsersOrderBy orderBy) {
        if (orderBy == null || !EnumSet.allOf(UsersOrderBy.class).contains(orderBy)) {
            throw new InvalidUserOrderException();
        }

        Map<User, List<UserCardinal>> userCardinalMap = userCardinalGetService.findAll()
                .stream()
                .collect(Collectors.groupingBy(UserCardinal::getUser, LinkedHashMap::new, Collectors.toList()));

        if (orderBy.equals(NAME_ASCENDING)) {
            return userCardinalMap.entrySet()
                    .stream()
                    .sorted(Comparator
                            .comparingInt(((Map.Entry<User, List<UserCardinal>> entry) -> (StatusPriority.fromStatus(entry.getKey().getStatus())).getPriority())))
                    .map(entry -> {
                        List<UserCardinal> userCardinals = userCardinalGetService.getUserCardinals(entry.getKey());
                        return mapper.toAdminResponse(entry.getKey(), userCardinals);
                    })
                    .toList();
        }

        if (orderBy.equals(CARDINAL_DESCENDING)) {

            return userCardinalMap.entrySet()
                    .stream()
                    .sorted(Comparator
                            .comparingInt(((Map.Entry<User, List<UserCardinal>> entry) -> (StatusPriority.fromStatus(entry.getKey().getStatus())).getPriority()))
                            .thenComparing(entry -> entry.getValue().stream()
                                    .map(uc -> uc.getCardinal().getCardinalNumber())
                                    .max(Integer::compare)
                                    .orElse(-1), Comparator.reverseOrder()))
                    .map(entry -> {
                        List<UserCardinal> userCardinals = userCardinalGetService.getUserCardinals(entry.getKey());
                        return mapper.toAdminResponse(entry.getKey(), userCardinals);
                    })
                    .toList();
        }

        return null;
    }

    @Override
    @Transactional
    public void accept(UserId userIds) {
        List<User> users = userGetService.findAll(userIds.userId());

        users.forEach(user -> {
            Integer cardinal = userCardinalGetService.getCurrentCardinal(user).getCardinalNumber();

            if (user.isInactive()) {
                userUpdateService.accept(user);
                List<Session> sessions = meetingGetService.find(cardinal);
                attendanceSaveService.init(user, sessions);
            }
        });
    }

    @Override
    @Transactional
    public void update(List<UserRoleUpdate> requests) {
        requests.forEach(request -> {
            User user = userGetService.find(request.userId());

            userUpdateService.update(user, request.role().name());
            jwtRedisService.updateRole(user.getId(), request.role().name());
        });
    }

    @Override
    public void leave(Long userId) {
        User user = userGetService.find(userId);
        // 탈퇴하는 경우 리프레시 토큰 삭제
        jwtRedisService.delete(user.getId());
        userDeleteService.leave(user);
    }

    @Override
    public void ban(UserId userIds) {
        List<User> users = userGetService.findAll(userIds.userId());

        users.forEach(user -> {
            jwtRedisService.delete(user.getId());
            userDeleteService.ban(user);
        });
    }

    @Override
    @Transactional
    public void applyOB(List<UserApplyOB> requests) {
        requests.forEach(request -> {
            User user = userGetService.find(request.userId());
            Cardinal nextCardinal = cardinalGetService.findByAdminSide(request.cardinal());

            if (userCardinalGetService.notContains(user, nextCardinal)) {
                if (userCardinalGetService.isCurrent(user, nextCardinal)) {
                    user.initAttendance();
                    List<Session> sessionList = meetingGetService.find(request.cardinal());
                    attendanceSaveService.init(user, sessionList);
                }
                UserCardinal userCardinal = new UserCardinal(user, nextCardinal);

                userCardinalSaveService.save(userCardinal);
            }
        });
    }

    @Override
    @Transactional
    public void reset(UserId userId) {

        List<User> users = userGetService.findAll(userId.userId());

        users.forEach(user -> userUpdateService.reset(user, passwordEncoder));
    }

}
