package com.mqc.repository;


import com.mqc.entity.ChatRecordDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRecordDetailRepository extends JpaRepository<ChatRecordDetail, String> {

    List<ChatRecordDetail> findAllByCallerOrCalleeOrderByCreateTimeAsc(String caller, String callee);

    List<ChatRecordDetail> findAllByCallIdOrderByCreateTimeAsc(String callId);


}
