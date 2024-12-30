package com.mqc.service;

import com.google.gson.Gson;
import com.mqc.entity.ChatRecordDetail;
import com.mqc.utils.HttpClientUtils;
import com.mqc.utils.JacksonUtils;
import com.mqc.vo.AsrVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service("AsrServiceImpl")
@Slf4j
public class AsrServiceImpl implements IAsrTranslate {


    @Value("${asr.api.url:10.12.0.21}")
    private String asrApiUrl;

    @Value("${asr.api.port:8085}")
    private int asrApiPort;

    public Map<String, Object> translateText(AsrVo asrVo) {
        Map<String, Object> map = new HashMap<>();
        InputStream is = null;
        String content = "";
        try {
            HttpClient client = HttpClientUtils.getHttpClient();

            String url = new StringBuilder().append("http://").append(this.asrApiUrl).append(":").append(this.asrApiPort).append(new MessageFormat("/{0}").format(new Object[]{"translateText"})).toString();

            HttpPost post = HttpClientUtils.getHttpPostMethod(url);
            HttpClientUtils.addHeaderContentJson(post);

            StringEntity entity = new StringEntity(JacksonUtils.objectToJsonStr(asrVo),StandardCharsets.UTF_8);
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            post.setEntity(entity);

            HttpResponse response = client.execute(post);

            is = response.getEntity().getContent();
            content = IOUtils.toString(is, StandardCharsets.UTF_8);
            if(!Strings.isBlank(content))
                map = JacksonUtils.jsonStrToMap(content);

        } catch (Exception e) {
            log.error("{} content={}", e, content);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return map;
    }

    public Map<String, Object> asrTranslate(AsrVo asrVo) {
        Map<String, Object> map = new HashMap<>();
        InputStream is = null;
        String content = "";
        try {
            HttpClient client = HttpClientUtils.getHttpClient();

            String url = new StringBuilder().append("http://").append(this.asrApiUrl).append(":").append(this.asrApiPort).append(new MessageFormat("/{0}/").format(new Object[]{"asrtranslate"})).toString();

            HttpPost post = HttpClientUtils.getHttpPostMethod(url);
            HttpClientUtils.addHeaderContentJson(post);

            StringEntity entity = new StringEntity(JacksonUtils.objectToJsonStr(asrVo),StandardCharsets.UTF_8);
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            post.setEntity(entity);

            HttpResponse response = client.execute(post);

            is = response.getEntity().getContent();
            content = IOUtils.toString(is, StandardCharsets.UTF_8);
            if(!Strings.isBlank(content))
                map = JacksonUtils.jsonStrToMap(content);

        }
        catch (Exception e) {
            log.error("{} content={}", e, content);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return map;
    }


    @Override
    public void asrTranslate(ChatRecordDetail chatRecordDetail, Consumer<String> handler) throws IOException {

        AsrVo asrVo = new AsrVo();
        byte[] encoded = Base64.encodeBase64(chatRecordDetail.getVoiceData());
        asrVo.setWavfile(chatRecordDetail.getWavfile());
        asrVo.setContent("b" + new String(encoded, StandardCharsets.UTF_8));
        //log.info("asrVo ={}",JacksonUtils.objectToJsonStr(asrVo));
        Map<String, Object> map = asrTranslate(asrVo);
        String source = map.containsKey("source") ? (String) map.get("source") : Strings.EMPTY;
        String result = map.containsKey("result") ? (String) map.get("result") : Strings.EMPTY;
        if ("Thanks for watching!".equalsIgnoreCase(source) || "感謝您的觀看！".equalsIgnoreCase(result)) {
            source = Strings.EMPTY;
            result = Strings.EMPTY;
        }

        String json = String.format("{source:\"%s\",result:\"%s\"}", source, result);
        handler.accept(json);

    }
}
