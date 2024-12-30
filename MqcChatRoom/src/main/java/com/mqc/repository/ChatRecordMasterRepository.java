package com.mqc.repository;

import com.mqc.entity.ChatRecordDetail;
import com.mqc.entity.ChatRecordMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatRecordMasterRepository extends JpaRepository<ChatRecordMaster, String> {

    @Query(value = "select c from ChatRecordMaster c  where  cast(createTime as date)  between cast(:startDateTime as date) and cast(:endDateTime as date)   and (   (c.agentExtension = :agentExtension or  :agentExtension is null)  and   (c.customerNumber = :customerNumber or  :customerNumber is null) )")
    Page<Object> findAllByAgentExtensionOrCustomerNumber(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime, @Param("agentExtension") String agentExtension, @Param("customerNumber") String customerNumber, Pageable pageable);

}
