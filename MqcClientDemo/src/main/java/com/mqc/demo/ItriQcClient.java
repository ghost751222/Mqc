package com.mqc.demo;


import com.macaron.mqcsdk.QcClerk;
import com.macaron.mqcsdk.RtpInform;
import com.macaron.mqcsdk.RtpReceiveListener;
import com.pachira.vcgclient.bean.ViewData;
import com.pachira.vcgclient.service.RetrieveResult;
import com.pachira.vcgclient.service.send.SendFile;
import com.pachira.vcgclient.service.send.SendStreamByte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.w3c.dom.Document;

import javax.annotation.PreDestroy;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class ItriQcClient implements RtpReceiveListener, RtpReceiveListenerTest {


    private static boolean toDebug = true;
    @Value("${pstt.ip:10.12.0.78}")
    private String psttIP;

    @Value("${pstt.port:8080}")
    private int psttPort;

    @Value("${pstt.kid:3}")
    private String psttKid;
    @Value("${audio.data}")
    private String audio_data;

    @Value("${com.mqc.demo.device}")
    private String deviceMac = "8c:16:45:53:1a:80";


    @Value("${com.mqc.shark.sip.debug:false}")
    private boolean sipDebug;

    @Value("${com.mqc.shark.party.info.debug:false}")
    private boolean partyInfoDebug;
    @Autowired
    TaskExecutor taskExecutor;
    QcClerk qc = null;

    @Autowired
    PcapService pcapService;

    //@PostConstruct
    public void startMonitorNet() throws SocketException {
        taskExecutor.execute(() -> {
            qc = new QcClerk(deviceMac);
            qc.setRtpReceiveListener(this);
            qc.setToDebugSIP(sipDebug);
            qc.setToDebugPartyInfo(partyInfoDebug);
            try {
                qc.Start();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @PreDestroy
    public void stopQcClerk() {
        try {
            qc.Stop();
        } catch (Exception ignored) {

        }

    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(50);

        return taskExecutor;
    }


    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            //
            //testVCG();
            //testToLocalVosk();
            //testToNetworkVosk();
            try {
                //startMonitorNet();
                pcapService.setRtpReceiveListener(this);
                pcapService.read();
            } catch (Exception e) {
                e.printStackTrace();
            }


        };
    }

    private void testToNetworkVosk() throws Exception {

//        VoskClient client = new VoskClient();
//        for (String res : client.transcribe("D:\\Consiliuminc\\IntelljWorkspace\\Mqc\\MqcClientDemo\\46454194.wav")) {
//            if (!res.contains("partial")) {
//                //JsonObject jsonObject = JsonParser.parseString(res).getAsJsonObject();
//                //System.out.println(jsonObject.get("text").getAsString());
//                //System.out.println(res);
//            }
//
//        }
    }

    private void testToLocalVosk() throws IOException, UnsupportedAudioFileException {
        LibVosk.setLogLevel(LogLevel.DEBUG);

        try (Model model = new Model("D:\\Consiliuminc\\IntelljWorkspace\\Mqc\\MqcClientDemo\\model");
             InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream("D:\\Consiliuminc\\IntelljWorkspace\\Mqc\\MqcClientDemo\\46454194.wav")));
             Recognizer recognizer = new Recognizer(model, 16000)) {

            int nbytes;
            byte[] b = new byte[4096];
            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    System.out.println(recognizer.getResult());
                } else {
                    System.out.println(recognizer.getPartialResult());
                }
            }

            System.out.println(recognizer.getFinalResult());
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ItriQcClient.class, args);
    }

    private void testVCG() throws Exception {
        // 錄音文件絕對路徑
        // String audio_data = "C:\\Users\\abc\\Desktop\\新增資料夾\\Speech\\2019-05-29\\file\\43291779.wav";
        // audio_data = ".\\46454194.wav";
        // audio_data = "D:\\ffmpeg\\13005.wav";

        File f = new File(audio_data);
        System.out.println(f.exists());
        if (!f.exists()) return;
        // 初始化
        ViewData viewData = new ViewData(psttIP, psttPort, audio_data, "23", psttKid);
        String sessionID = "sessionID";
        String requestID = "requestID";
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put("sessionid", sessionID);
//        extraParams.put("uId", UUID.randomUUID().toString());
//        extraParams.put("uid", UUID.randomUUID().toString());

        extraParams.put("reqid", sessionID + " From-To:" + requestID);
        // isTransDigit 表示是否開r啟數字轉換（一百三十三轉為133）
        extraParams.put(ViewData.IS_TRANS_DIGIT, ViewData.IS_TRANS_DIGIT_ON);
        extraParams.put(ViewData.IS_BUTTER_FLY, ViewData.IS_BUTTER_FLY_OFF);
        viewData.setExtraParams(extraParams);
        SendFile ss = new SendFile(viewData, new RetrieveResult() {
            public void retrieveResult(String result) {
                // System.out.println(result);// 結果文本
                System.out.println(result);
            }
        });
        ss.send();
    }

    private static void ParseResult(String result) {

        if (toDebug) System.out.println("\nDebug:\n" + result);
        if ("-1".equals(result)) {
            ;

//        	服務器錯誤
//			System.out.println("Server Error.");
        } else if (!"0".equals(result)) {

            Document doc = parseXmlFromString(result, "UTF-8");
            if (doc != null) {

                String state = doc.getElementsByTagName("state").item(0).getTextContent();
                if (state.equals("0")) {
                    // <pachira><sessionid>-1</sessionid><voiceURL><![CDATA[http://127.0.0.1/QianYuSrv/viewVoice?op=detail&uuid=8f923343-8823-4ef1-8fff-4d5606c32420]]></voiceURL><state>0</state><resultid>8f923343-8823-4ef1-8fff-4d5606c32420</resultid><requestid>2019-07-02
                    // 10:14:58.440
                    // From-To:7779-7780</requestid><transcribedResult><confirmed>true</confirmed>
                    // <listCount>1</listCount> <trascirbedSentence> <list> <int>0</int> <word>
                    // <value>哦哦羅</value> <weight>100</weight> <nlpres><![CDATA[]]></nlpres> </word>
                    // </list> </trascirbedSentence></transcribedResult></pachira>

//					String sessionid = doc.getElementsByTagName("sessionid").item(0).getTextContent();
                    String reqid = doc.getElementsByTagName("requestid").item(0).getTextContent();
                    String value = doc.getElementsByTagName("value").item(0).getTextContent();
                    String weight = doc.getElementsByTagName("weight").item(0).getTextContent();
                    String tKey = "timestamp: " + reqid;
                    String tValue = "Text:" + value + "; Score:" + weight;

                    System.out.println(tKey + "; " + tValue + "\n");


                }
            }
        } else {

//			輸出識別結果
//			System.out.println("##" + result);		
        }
    }

    private static Document parseXmlFromString(String xmlString) {

        return parseXmlFromString(xmlString, null);
    }

    private static Document parseXmlFromString(String xmlString, String encoding) {

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

    @Override
    public void RtpReceive(RtpInform rtpInform) throws Exception {

        Map<String, String> extraParams = getExtraParams(rtpInform);


//    String randomUUID = UUID.randomUUID().toString();
//
//        File f =new File(randomUUID + ".wav");
//        Files.write(f.toPath(), rtpInform.getVoiceData());
//
//        ViewData viewData = new ViewData(psttIP, psttPort, null, "23", psttKid);
//        ViewData viewData = new ViewData(psttIP, psttPort, f.getAbsolutePath(), "23", psttKid);
//        extraParams.put("filePath",f.getAbsolutePath());
//        viewData.setExtraParams(extraParams);
//        SendFile ss = new SendFile(viewData, result -> {
//            f.delete();
//            // System.out.println(result);// 結果文本
//            ParseResult(result);
//        });
//        ss.send();
        ViewData viewData = new ViewData(psttIP, psttPort, "", "23", psttKid);
        viewData.setExtraParams(extraParams);
        SendStreamByte sendStreamByte = new SendStreamByte(viewData, ItriQcClient::ParseResult);

        if (!sendStreamByte.isAlive()) {
            sendStreamByte.init();
        }

        sendStreamByte.sendData(rtpInform.getVoiceData());
        sendStreamByte.sendData(new byte[0]);
    }

    private Map<String, String> getExtraParams(RtpInform rtpInform) {
        boolean isAddPunct = false;
        boolean isTransDigit = false;
        boolean isButterFly = false;

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put("sessionid", rtpInform.getReceiveDateTime().toString());
        extraParams.put("reqid", rtpInform.getCallee() + " From-To:" + rtpInform.getCaller());
        extraParams.put(ViewData.IS_ADD_PUNCT, isAddPunct ? ViewData.IS_ADD_PUNCT_ON : ViewData.IS_ADD_PUNCT_OFF);
        extraParams.put(ViewData.IS_TRANS_DIGIT, isTransDigit ? ViewData.IS_TRANS_DIGIT_ON : ViewData.IS_TRANS_DIGIT_OFF);
        extraParams.put(ViewData.IS_BUTTER_FLY, isButterFly ? ViewData.IS_BUTTER_FLY_ON : ViewData.IS_BUTTER_FLY_OFF);
        return extraParams;
    }

    @Override
    public void RtpReceiveTest(RtpInform rtpInform) throws Exception {
        RtpReceive(rtpInform);
    }
}
