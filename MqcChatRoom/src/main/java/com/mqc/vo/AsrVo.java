package com.mqc.vo;

import lombok.Data;

@Data
public class AsrVo {

    private String channels ="1";
    private String sampwidth="2";
    private String framerate="8000";
    private String content;
    private String wavfile;
    private String to_lang;
}

