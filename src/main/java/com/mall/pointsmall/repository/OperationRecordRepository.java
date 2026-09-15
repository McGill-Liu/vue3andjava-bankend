package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.OperationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface OperationRecordRepository extends JpaRepository<OperationRecord, Long> {
    @Query("""
            select record from OperationRecord record
            where (:success is null or record.success = :success)
              and (:operatorName is null or lower(record.operatorName) like lower(concat('%', :operatorName, '%')))
              and (:action is null or lower(record.action) like lower(concat('%', :action, '%')))
              and (:targetName is null or lower(record.targetName) like lower(concat('%', :targetName, '%')))
              and (:startAt is null or record.createdAt >= :startAt)
              and (:endAt is null or record.createdAt <= :endAt)
            """)
    Page<OperationRecord> search(String operatorName, String action, String targetName, Boolean success,
                                 LocalDateTime startAt, LocalDateTime endAt, Pageable pageable);
}
