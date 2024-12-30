package com.mqc.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ResponseMessage extends RequestMessage {

    private Date createTime;

    public ResponseMessage(String caller, String callee, String message,String translate) {
        this.setCaller(caller);
        this.setCallee(callee);
        this.setMessage(message);
        this.setTranslate(translate);
    }


}
