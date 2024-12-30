package com.mqc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.macaron.mqcsdk.RtpInform;
import com.macaron.mqcsdk.RtpReceiveListener;
import com.mqc.entity.ChatRecordDetail;
import com.mqc.entity.ChatRecordMaster;
import com.mqc.entity.UserInfo;
import com.mqc.repository.ChatRecordDetailRepository;
import com.mqc.repository.ChatRecordMasterRepository;
import com.mqc.repository.UserInfoRepository;
import com.mqc.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RtpReceiveListenerImpl implements RtpReceiveListener {


    @Value("${device.save.rtp.wav:false}")
    boolean deviceSaveRtpWav;

    @Value("${device.save.rtp.wav.path:/NFS}")
    String deviceSaveRtpWavPath;


    @Value("${device.save.rtp.wav.delete:false}")
    boolean deviceSaveRtpWavDelete;

    @Value("${device.save.rtp.wav.size.kb:35}")
    int deviceSaveRtpWavSizeKB;

    @Autowired
    @Qualifier("executor")
    @Lazy
    TaskExecutor taskExecutor;

    @Autowired
    TaskQueueService taskQueueService;

    IAsrTranslate iAsrTranslate;

    @Autowired
    private SimpMessagingTemplate webSocket;

    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    ChatRecordDetailRepository chatRecordDetailRepository;
    @Autowired
    ChatRecordMasterRepository chatRecordMasterRepository;

    @Autowired
    public RtpReceiveListenerImpl(@Value("${asr.type}AsrServiceImpl") String asrType, BeanFactory beanFactory) {
        this.iAsrTranslate = beanFactory.getBean(asrType, IAsrTranslate.class);

    }


    private void saveToDB(ChatRecordDetail chatRecordDetail) {



        taskExecutor.execute(() -> {
            try {

                String direction = null;
                String customerNumber = null;
                UserInfo userInfo = null;
                userInfo = userInfoRepository.findByExtension(chatRecordDetail.getCaller());
                if (userInfo != null) {
                    customerNumber = chatRecordDetail.getCallee();
                    if (chatRecordDetail.getIndex() == 1) {
                        direction = "outbound";
                    } else {
                        direction = "inbound";
                    }


                } else {
                    userInfo = userInfoRepository.findByExtension(chatRecordDetail.getCallee());
                    customerNumber = chatRecordDetail.getCaller();
                    if (chatRecordDetail.getIndex() == 1) {
                        direction = "inbound";
                    } else {
                        direction = "outbound";
                    }

                }
                if (userInfo == null) {
                    log.warn("caller ={} or callee ={} doesn't exist", chatRecordDetail.getCaller(), chatRecordDetail.getCallee());
                    return;
                }

                //if (chatRecordDetail.getIndex() == 1) {
                Optional<ChatRecordMaster> ChatRecordMasterOpt = chatRecordMasterRepository.findById(chatRecordDetail.getCallId());
                ChatRecordMaster chatRecordMaster = ChatRecordMasterOpt.orElse(null);
                if (chatRecordMaster == null) {
                    chatRecordMaster = new ChatRecordMaster();
                    chatRecordMaster.setCallId(chatRecordDetail.getCallId());
                    chatRecordMaster.setAgentID(userInfo.getId());
                    chatRecordMaster.setAgentAccount(userInfo.getAccount());
                    chatRecordMaster.setAgentName(userInfo.getUserName());
                    chatRecordMaster.setAgentExtension(userInfo.getExtension());
                    chatRecordMaster.setCustomerNumber(customerNumber);
                    chatRecordMaster.setDirection(direction);
                    chatRecordMaster.setCreateTime(LocalDateTime.now());
                    chatRecordMasterRepository.save(chatRecordMaster);

                }
                //}
                chatRecordDetailRepository.save(chatRecordDetail);

            } catch (Exception e) {
                try {
                    log.error("saveToDB {} {}", e, JacksonUtils.toJsonString(chatRecordDetail));
                } catch (JsonProcessingException ex) {
                    log.error(ex.toString());
                }
            }

        });
    }

    @Override
    public void RtpReceive(RtpInform rtpInform) {





        Runnable task =
                () -> {
                    try {

                        final ChatRecordDetail chatRecordDetail = createChatRecord(rtpInform);
                        String fileName = "";
                        Path p = null;
                        long kilobytes = chatRecordDetail.getVoiceData().length / 1024;

                        try {

                            if (deviceSaveRtpWav) {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss-SSS");

                                String d = formatter.format(LocalDateTime.now());
                                fileName = d + "-" + chatRecordDetail.getCaller() + "-" + chatRecordDetail.getCallee() + "-" + chatRecordDetail.getCallId() + ".wav";
                                p = Paths.get(deviceSaveRtpWavPath, fileName);
                                Files.write(p, chatRecordDetail.getVoiceData());
                            } else {
                                fileName = "do not create";
                            }
                        } catch (Exception e) {
                            log.error("File Write Data  :", e);
                        }
                        chatRecordDetail.setWavfile(fileName);

                        if(kilobytes <= deviceSaveRtpWavSizeKB){
                            log.warn("caller ={} ,callee={} ,file wav = {} ,file size ={}KB ,data not send",chatRecordDetail.getCaller(),chatRecordDetail.getCallee(),fileName,kilobytes);
                            return;
                        }

                        Path finalP = p;

                        iAsrTranslate.asrTranslate(chatRecordDetail, s -> {

                            try {

                                Duration dur = Duration.between(chatRecordDetail.getCreateTime(), LocalDateTime.now());
                                long seconds = dur.getSeconds();
                                JsonObject jsonObject = JsonParser.parseString(s).getAsJsonObject();

                                String source = jsonObject.get("source").getAsString();
                                String result = jsonObject.get("result").getAsString();

                                log.info("caller ={} ,callee={} ,message={} ,translate={} ,translateExecuteTime={}s,file wav ={},file size={}KB", chatRecordDetail.getCaller(), chatRecordDetail.getCallee(), source, result, seconds, chatRecordDetail.getWavfile(), kilobytes);
                                if (!Strings.isBlank(source)) {
                                    chatRecordDetail.setMessage(source);
                                    chatRecordDetail.setTranslate(result);
                                    webSocket.convertAndSend("/topic/getResponse", chatRecordDetail);
                                    saveToDB(chatRecordDetail);
                                } else {
                                    if (deviceSaveRtpWav && deviceSaveRtpWavDelete) {
                                        Files.deleteIfExists(finalP);
                                    }
                                }

                            } catch (Exception e) {
                                log.error("Rtp Data asrTranslate :{},{}", e,s);
                            }


                        });
                    } catch (IOException e) {
                        log.error("taskExecutor  IOException:", e);
                    }
                };

       // taskQueueService.addToQueue(task);
        taskExecutor.execute(task);
    }

    private ChatRecordDetail createChatRecord(RtpInform rtpInform) {
        ChatRecordDetail chatRecordDetail = new ChatRecordDetail();
        chatRecordDetail.setCallee(rtpInform.getCallee());
        chatRecordDetail.setCaller(rtpInform.getCaller());
        chatRecordDetail.setReceiveTime(rtpInform.getReceiveDateTime());
        chatRecordDetail.setCreateTime(LocalDateTime.now());
        chatRecordDetail.setCallId(rtpInform.getCallId());
        chatRecordDetail.setUniCallId(UUID.randomUUID().toString());
        chatRecordDetail.setContactId(rtpInform.getCallId());
        chatRecordDetail.setVoiceData(rtpInform.getVoiceData());
        chatRecordDetail.setIndex(rtpInform.getIndex());
        return chatRecordDetail;
    }


}
