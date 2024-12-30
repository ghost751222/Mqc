package com.mqc.service;


import com.google.gson.Gson;
import com.mqc.entity.ChatRecordDetail;
import com.pachira.vcgclient.bean.ViewData;
import com.pachira.vcgclient.service.send.SendStreamByte;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service("PsttAsrServiceImpl")
@Slf4j
public class PsttAsrServiceImpl implements IAsrTranslate {


    @Value("${pstt.ip}")
    String psttIp;


    @Value("${pstt.port}")
    Integer psttPort;


    @Value("3")
    String psttKid;


    public String ParseResult(String result) throws Exception {
        String message = "";
        if ("-1".equals(result)) {
            log.error("Tomcat Server Error");
        } else if (!"0".equals(result)) {

            Document doc = parseXmlFromString(result, "UTF-8");
            if (doc != null) {

                String state = doc.getElementsByTagName("state").item(0).getTextContent();
                if (state.equals("0")) {
                    String sessionid = doc.getElementsByTagName("sessionid").item(0).getTextContent();
                    String reqid = doc.getElementsByTagName("requestid").item(0).getTextContent();
                    String value = doc.getElementsByTagName("value").item(0).getTextContent();
                    String weight = doc.getElementsByTagName("weight").item(0).getTextContent();


                    String tKey = "timestamp: " + reqid;
                    String tValue = "Text:" + value + "; Score:" + weight;
                    // log.info(tKey + "; " + tValue + "\n");
                    message = value;

                }
            }

        }
        return message;
    }

    public Document parseXmlFromString(String xmlString, String encoding) {

        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream inputStream = new ByteArrayInputStream(
                    encoding == null ? xmlString.getBytes() : xmlString.getBytes(encoding));
            Document document = builder.parse(inputStream);
            return document;
        } catch (Exception e) {

            System.out.println(e);

            return null;
        }
    }

    private Map<String, String> getExtraParams(ChatRecordDetail chatRecordDetail) {
        boolean isAddPunct = false;
        boolean isTransDigit = false;
        boolean isButterFly = false;

        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("sessionid", chatRecordDetail.getReceiveTime().toString());
        extraParams.put("reqid", new Gson().toJson(chatRecordDetail));
        extraParams.put(ViewData.IS_ADD_PUNCT, isAddPunct ? ViewData.IS_ADD_PUNCT_ON : ViewData.IS_ADD_PUNCT_OFF);
        extraParams.put(ViewData.IS_TRANS_DIGIT, isTransDigit ? ViewData.IS_TRANS_DIGIT_ON : ViewData.IS_TRANS_DIGIT_OFF);
        extraParams.put(ViewData.IS_BUTTER_FLY, isButterFly ? ViewData.IS_BUTTER_FLY_ON : ViewData.IS_BUTTER_FLY_OFF);
        return extraParams;
    }

    @Override
    public void asrTranslate(ChatRecordDetail chatRecordDetail, Consumer<String> handler) {
        ViewData viewData = new ViewData(psttIp, psttPort, "", "23", psttKid);
        viewData.setExtraParams(getExtraParams(chatRecordDetail));

        SendStreamByte sendStreamByte = new SendStreamByte(viewData, s -> {
            try {
                String json = String.format("{source:\"%s\",result:\"%s\"}", ParseResult(s),"");
                handler.accept(json);
            } catch (Exception e) {
                log.error("ParseResult Error {}", e);
            }
        });

        if (!sendStreamByte.isAlive()) {
            sendStreamByte.init();
        }
        sendStreamByte.sendData(chatRecordDetail.getVoiceData());
        sendStreamByte.sendData(new byte[0]);


    }
}
