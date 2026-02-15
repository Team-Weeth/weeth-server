package com.weeth.domain.user.domain.entity;

import jakarta.persistence.*;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.user.domain.entity.enums.Department;
import com.weeth.domain.user.domain.entity.enums.Position;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.entity.enums.Status;
import com.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static com.weeth.domain.user.application.dto.request.UserRequestDto.Update;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private Long kakaoId;

    @Column(unique = true)
    private String appleId;

    private String name;

    private String email;

    private String password;

    private String studentId;

    private String tel;

    @Enumerated(EnumType.STRING)
    private Position position;

    @Enumerated(EnumType.STRING)
    private Department department;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Integer attendanceCount;

    private Integer absenceCount;

    private Integer attendanceRate;

    private Integer penaltyCount;

    private Integer warningCount;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Attendance> attendances = new ArrayList<>();

    @PrePersist
    public void init() {
        status = Status.WAITING;
        role = Role.USER;
        attendanceCount = 0;
        absenceCount = 0;
        attendanceRate = 0;
        penaltyCount = 0;
        warningCount = 0;
    }

    public void addKakaoId(long kakaoId) {
        this.kakaoId = kakaoId;
    }

    public void addAppleId(String appleId) {
        this.appleId = appleId;
    }

    public void leave() {
        this.status = Status.LEFT;
    }

    /*
    todo 차후 일반 로그인 비활성화시 해당 메서드에서 예외를 날리도록 수정
     */
    public boolean isInactive() {
        return this.status != Status.ACTIVE;
    }

    public void update(Update dto) {
        this.name = dto.name();
        this.email = dto.email();
        this.studentId = dto.studentId();
        this.tel = dto.tel();
        this.department = Department.to(dto.department());
    }

    public void accept() {
        this.status = Status.ACTIVE;
    }

    public void ban() {
        this.status = Status.BANNED;
    }

    public void update(String role) {
        this.role = Role.valueOf(role);
    }

    public void reset(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(studentId);
    }

    public void add(Attendance attendance) {
        this.attendances.add(attendance);
    }

    public void initAttendance() {
        this.attendances.clear();
        this.attendanceCount = 0;
        this.absenceCount = 0;
        this.attendanceRate = 0;
    }

    public void attend() {
        attendanceCount++;
        calculateRate();
    }

    public void removeAttend() {
        if (attendanceCount > 0) {
            attendanceCount--;
            calculateRate();
        }
    }

    public void absent() {
        absenceCount++;
        calculateRate();
    }

    public void removeAbsent() {
        if (absenceCount > 0) {
            absenceCount--;
            calculateRate();
        }
    }

    private void calculateRate() {
        if (attendanceCount + absenceCount > 0) {
            attendanceRate = (attendanceCount * 100) / (attendanceCount + absenceCount);
        } else {
            attendanceRate = 0;
        }
    }

    public void incrementPenaltyCount() {
        penaltyCount++;
    }

    public void decrementPenaltyCount() {
        if (penaltyCount > 0) {
            penaltyCount--;
        }
    }

    public void incrementWarningCount() {
        warningCount++;
    }

    public void decrementWarningCount() {
        if (warningCount > 0) {
            warningCount--;
        }
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public Part getUserPart() {
        return Part.valueOf(this.position.name());
    }
}
