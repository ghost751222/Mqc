package com.macaron.mqcsdk;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RtpInform {
    private LocalDateTime receiveDateTime;
    private String caller;
    private String callee;
    private String callId;
    private String contactId;
    private int index;
    private byte[] voiceData;

}
