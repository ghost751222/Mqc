package com.mqc.service;


import com.mqc.repository.ChatRecordMasterRepository;
import com.mqc.vo.RequestQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ChatRecordMasterService {

    @Autowired
    ChatRecordMasterRepository chatRecordMasterRepository;


    public Page<Object> findAllByAgentExtensionOrCustomerNumber(RequestQueryVo requestQueryVo, PageRequest pageRequest) {
        return chatRecordMasterRepository.findAllByAgentExtensionOrCustomerNumber(requestQueryVo.getStartDateTime(),requestQueryVo.getEndDateTime(),requestQueryVo.getAgentExtension(), requestQueryVo.getCustomerNumber(), pageRequest);

    }

}
