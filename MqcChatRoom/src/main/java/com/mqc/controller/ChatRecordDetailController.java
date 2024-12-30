package com.mqc.controller;

import com.mqc.entity.ChatRecordDetail;
import com.mqc.service.ChatRecordDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/chatRecordDetail")
public class ChatRecordDetailController {


    @Autowired
    ChatRecordDetailService recordDetailService;

    @RequestMapping("")
    public String page() {
        return "chatRecordDetail";
    }



    @RequestMapping(value = "/chatRecord",method = RequestMethod.GET)
    @ResponseBody
    public List<ChatRecordDetail> chatRecord(@RequestParam Map<String, Object> params) {
        String contactID = (String) params.get("id");
        return  recordDetailService.findAllByCallIdOrderByCreateTimeAsc(contactID);
    }



}

