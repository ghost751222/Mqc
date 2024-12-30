package com.mqc.service;

import com.mqc.entity.ChatRecordDetail;

import java.io.IOException;
import java.util.function.Consumer;

public interface IAsrTranslate {
    void asrTranslate(ChatRecordDetail chatRecordDetail, Consumer<String> handler) throws IOException;
}
