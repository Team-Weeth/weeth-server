package leets.weeth.domain.attendance.domain.repository;

import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.user.domain.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllByMeetingAndUserStatus(Meeting meeting, Status status);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.meeting = :meeting")
    void deleteAllByMeeting(Meeting meeting);
}
