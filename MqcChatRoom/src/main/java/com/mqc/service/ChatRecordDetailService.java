package com.mqc.service;

import com.mqc.entity.ChatRecordDetail;
import com.mqc.repository.ChatRecordDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatRecordDetailService {

    @Autowired
    ChatRecordDetailRepository chatRecordDetailRepository;


    public List<ChatRecordDetail> findAllByCallIdOrderByCreateTimeAsc(String contactID) {
        return chatRecordDetailRepository.findAllByCallIdOrderByCreateTimeAsc(contactID);

    }


}
