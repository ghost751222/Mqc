package com.mqc.controller;

import com.mqc.service.AsrServiceImpl;
import com.mqc.vo.AsrVo;
import com.mqc.vo.RequestMessage;
import com.mqc.vo.ResponseMessage;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpSession;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    HttpSession httpSession;

    @Autowired
    private SimpUserRegistry userRegistry;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    AsrServiceImpl asrService;

    @MessageMapping("/welcome")
    @SendTo("/topic/getResponse")
    public RequestMessage say(RequestMessage message) {
        return message;
    }

//    @MessageMapping("/message")
//    @SendToUser("/topic/message")
//    public ResponseMessage handleSubscribe(RequestMessage message) {
//        System.out.println("this is the @SubscribeMapping('/marco')");
//        return new ResponseMessage("I am a msg from SubscribeMapping('/macro').");
//    }

    @MessageMapping("/send")
    public void send(@Payload RequestMessage chatMessage) {

        int i = 1;
        for (SimpUser user : userRegistry.getUsers()) {
            System.out.println("用户" + i++ + "---" + user);
        }
        String translate = "English Server Error";
        String source = chatMessage.getMessage();
        try {
            AsrVo asrVo = new AsrVo();
            asrVo.setContent(chatMessage.getMessage());
            asrVo.setTo_lang(chatMessage.getTo_lang());
            Map<String, Object> map = asrService.translateText(asrVo);
            translate = (String) map.get("result");
        } catch (Exception ignore) {
            translate = "English Server Error";
        }
        //发送消息给指定用户
        String userName = chatMessage.getCallee();
        ResponseMessage res = new ResponseMessage(chatMessage.getCaller(), userName, source, translate);
        res.setCreateTime(new Date());
        messagingTemplate.convertAndSendToUser(userName, "/queue/" + userName, res);
        messagingTemplate.convertAndSendToUser(chatMessage.getCaller(), "/queue/" + chatMessage.getCaller(), res);
    }
}
