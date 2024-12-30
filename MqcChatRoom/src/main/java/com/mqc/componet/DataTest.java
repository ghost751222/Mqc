package com.mqc.componet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.macaron.mqcsdk.QcClerk;
import com.mqc.entity.ChatRecordDetail;
import com.mqc.entity.UserInfo;
import com.mqc.repository.ChatRecordDetailRepository;
import com.mqc.repository.UserInfoRepository;
import com.mqc.service.AsrServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class DataTest {


    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    ChatRecordDetailRepository chatRecordDetailRepository;

    @Autowired
    PasswordEncoder passwordEncoder;


    @Autowired
    AsrServiceImpl aAsrApiService;

    @Autowired
    QcClerk qcClerk;


    @Autowired
    @Qualifier("executor")
    TaskExecutor taskExecutor;

    //@PostConstruct
    public void qcTest() throws JsonProcessingException {

        // taskExecutor.execute(()->{
        String PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\p192-1004.pcap";
        //PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\p2249.pcap";
        //PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\qq.pcap";
        //PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\ez2.pcap";
        PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\1999.pcap";
        PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\1002-0979995939.pcap";
        //PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\mute.pcap";
        qcClerk.read(PCAP_FILE);
        //  });

    }

    //@PostConstruct
    public void userData() {
        UserInfo u = new UserInfo();
        u.setAccount("admin");
        u.setUserName("admin");
        u.setExtension("1234");
        u.setCreateTime(LocalDateTime.now());
        u.setId(1);
        u.setPassword(passwordEncoder.encode("000000"));
        userInfoRepository.save(u);
    }

    //@PostConstruct
    public void chatData() {
        int max = 31;
        String callId = UUID.randomUUID().toString();
        String contactId = UUID.randomUUID().toString();
        for (int i = 1; i <= max; i++) {
            ChatRecordDetail chatRecordDetail = new ChatRecordDetail();
            chatRecordDetail.setUniCallId(UUID.randomUUID().toString());
            chatRecordDetail.setCallId(callId);
            chatRecordDetail.setContactId(contactId);
            chatRecordDetail.setMessage("hello" + i);
            chatRecordDetail.setCreateTime(LocalDateTime.now());
            chatRecordDetail.setReceiveTime(LocalDateTime.now());
            if (i % 2 == 0) {
                chatRecordDetail.setCaller("5005");
                chatRecordDetail.setCallee("5006");
            } else {
                chatRecordDetail.setCaller("5006");
                chatRecordDetail.setCallee("5005");
            }

            if (i % 10 == 0) {
                callId = UUID.randomUUID().toString();
                contactId = UUID.randomUUID().toString();
            }
            chatRecordDetailRepository.save(chatRecordDetail);
        }

    }

}

