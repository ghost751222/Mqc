package com.macaron.mqcsdk;

import java.io.IOException;

public interface RtpReceiveListener {
    void RtpReceive(RtpInform rtpInform) throws Exception;
}
