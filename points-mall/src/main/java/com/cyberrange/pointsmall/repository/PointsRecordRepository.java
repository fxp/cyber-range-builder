package com.cyberrange.pointsmall.repository;

import com.cyberrange.pointsmall.model.PointsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {

    Page<PointsRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PointsRecord> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, PointsRecord.PointsType type, Pageable pageable);

    @Query("SELECT SUM(r.points) FROM PointsRecord r WHERE r.user.id = :userId AND r.type IN ('EARN_PURCHASE','EARN_REVIEW','EARN_SIGNIN','EARN_ACTIVITY','EARN_ADMIN') AND r.createdAt BETWEEN :start AND :end")
    Long sumEarnedPoints(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<PointsRecord> findByExpireAtBeforeAndTypeIn(LocalDateTime now, List<PointsRecord.PointsType> types);
}
