package com.mqc.demo;

import com.macaron.mqcsdk.RtpInform;

import java.io.IOException;

public interface RtpReceiveListenerTest {
    void RtpReceiveTest(RtpInform rtpInform) throws Exception;
}
