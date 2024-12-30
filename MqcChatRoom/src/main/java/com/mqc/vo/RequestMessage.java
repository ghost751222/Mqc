package com.mqc.vo;
import lombok.Data;
@Data
public class RequestMessage {

    private String caller;
    private String callee;

    private String message;

    private String translate;
    private String to_lang;
}

