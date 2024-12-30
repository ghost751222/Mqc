package com.mqc.demo;

import com.macaron.mqcsdk.PartyInfo;
import com.macaron.mqcsdk.RtpInform;
import com.pachira.vcgclient.bean.ViewData;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.packet.AbstractMessageHeader;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Udp;
import org.jnetpcap.protocol.voip.Rtp;
import org.jnetpcap.protocol.voip.Sdp;
import org.jnetpcap.protocol.voip.Sip;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PcapService {
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final AudioFormat pcmFormat = new AudioFormat(8000, 16, 1, true, false);
    private final AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);
    private double silenceThreshold = 20;
    private RtpReceiveListenerTest rtpReceiveListenerTest = null;

    public void setRtpReceiveListener(RtpReceiveListenerTest rtpReceiveListener) {
        this.rtpReceiveListenerTest = rtpReceiveListener;
    }

    ConcurrentHashMap<String, List<PartyInfo>> parties = new ConcurrentHashMap<>();

    private enum RtpAddressParty {Source, Destination}

    private String getSipDN(String fldValue) {

        String token = "<sip:";
        int start = fldValue.indexOf(token);
        int end = fldValue.indexOf('@');
        return fldValue.substring(start + token.length(), end);
    }

    public void read() {
        //final String PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\sip3.pcap";
        final String PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\p2249.pcap";
        //final String PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\rtp.pcap";

        /* Make sure we have a compatible Pcap runtime installed */
        //Pcap.checkPcapVersion(Pcap.VERSION);

        /* Automatically close Pcap resource when done */
        final StringBuilder errbuf = new StringBuilder();
        Pcap pcap = Pcap.openOffline(PCAP_FILE, errbuf);
        if (pcap == null) {
            System.err.println(errbuf);
            return;
        }
        /* Compile packet filter to capture TCP packets only */
        String expression = "sip or rtp";
        PcapBpfProgram filter = new PcapBpfProgram();
        if (pcap.compile(filter, expression, 0, 0xFFFFFF00) == Pcap.OK) {
            pcap.setFilter(filter);
        }


        /* Number of packets to capture */
        final int PACKET_COUNT = 10;

        /* Send packets to handler. The generic user parameter can be of any type. */
        pcap.dispatch(Pcap.LOOP_INFINITE, getPacketConsumer(), null);


    }

    private String getSdpAddress(Sdp sdp) {

        return getSdpIp(sdp) + ':' + getSdpPort(sdp);
    }

    private String getSdpIp(Sdp sdp) {

        String sdptext = sdp.text();
        String token = "c=IN IP4 ";
        int pos = sdptext.indexOf(token) + token.length();
        int end = pos;
        if (pos != -1)
            while (sdptext.charAt(end) != '\r') end++;

        return sdptext.substring(pos, end);
    }

    private int getSdpPort(Sdp sdp) {

        String sdptext = sdp.text();
        String token = "m=audio ";
        int pos = sdptext.indexOf(token) + token.length();
        int end = pos;
        if (pos != -1)
            while (sdptext.charAt(end) != ' ') end++;

        return Integer.parseInt(sdptext.substring(pos, end));


    }


    private PartyInfo GetDnByRtpAddress(ConcurrentHashMap<String, List<PartyInfo>> parties, String value) {
        for (Map.Entry<String, List<PartyInfo>> set : parties.entrySet()) {
            for (PartyInfo p : set.getValue()) {
                if (p.getAudioAddress().equals(value)) {
                    return p;
                }
            }

        }

        return null;
    }

    private String GetRtpAddress(JPacket packet, RtpAddressParty party) {

        Ip4 ip = new Ip4();
        packet.getHeader(ip);

        Udp udp = new Udp();
        packet.getHeader(udp);

        if (party.equals(RtpAddressParty.Source)) {

            return FormatUtils.ip(ip.source()) + ':' + udp.source();
        }

        return FormatUtils.ip(ip.destination()) + ':' + udp.destination();
    }

    private JPacketHandler<String> getPacketConsumer() {

        return new JPacketHandler<String>() {


            @Override
            public void nextPacket(JPacket packet, String user) {

                Sip sip = new Sip();
                Sdp sdp = new Sdp();
                Rtp rtp = new Rtp();

                try {
                    if (packet.hasHeader(Sip.ID)) {

                        packet.getHeader(sip);
                        if (packet.hasHeader(Sdp.ID)) {
                            packet.getHeader(sdp);
                            if (sdp.text() == null){
                                String agggg = "a";
                                return;
                           }
                        }
                        AbstractMessageHeader.MessageType sipMsgType = sip.getMessageType();
                        String sipMsgCode = sip.fieldValue(Sip.Response.ResponseCodeMsg);
                        String responseCode = sip.fieldValue(Sip.Response.ResponseCode);

                        String callID = sip.fieldValue(Sip.Fields.Call_ID);

                        if (sip.getMethod() == Sip.Method.BYE) {
                            //System.out.println(sip.toString());
                            parties.remove(callID);

                        }
                        //if (sip.getMethod() == Sip.Method.INVITE && sip.isResponse() && "200".equals(responseCode)) {
                        //else if ((sip.getMethod() == Sip.Method.INVITE && !"180".equals(responseCode)) || "200".equals(responseCode)) {
                        else if (  (sip.getMethod() == Sip.Method.INVITE && !"180".equals(responseCode) && !"Trying".equalsIgnoreCase(sipMsgCode) && !"Session".equalsIgnoreCase(sipMsgCode)) || (sip.getMethod() != Sip.Method.PRACK && sip.getMethod() != Sip.Method.OPTIONS && "200".equals(responseCode) && (sipMsgType == AbstractMessageHeader.MessageType.RESPONSE))) {
                            boolean isContact = false;

                            String from = getSipDN(sip.fieldValue(Sip.Fields.From));
                            String to = getSipDN(sip.fieldValue(Sip.Fields.To));

                            String speaker = (sipMsgType == AbstractMessageHeader.MessageType.REQUEST) ? from : to;
                            String audience = (sipMsgType == AbstractMessageHeader.MessageType.RESPONSE) ? from : to;
                            String uuidString = UUID.randomUUID().toString();
//                            System.out.println("MessageTYpe " + sip.getMessageType());
//                            System.out.println("ResponseCodeMsg " + sipMsgCode);
//                            System.out.println("ResponseCode " + responseCode);
//                            System.out.println("Method " + sip.getMethod());
//                            System.out.println("from " + sip.fieldValue(Sip.Fields.From));
//                            System.out.println("to " + sip.fieldValue(Sip.Fields.To));
//                            System.out.println("Call_ID " + sip.fieldValue(Sip.Fields.Call_ID));
                            //System.out.println("sdp text " + sdp.text());

                            List<PartyInfo> partyInfos = null;
                            PartyInfo vLog = null;
                            if (parties.containsKey(callID)) {
                                partyInfos = parties.get(callID);
                                vLog = new PartyInfo();
                                vLog.setSipCallID(callID);
                                vLog.setSpeaker(speaker);
                                vLog.setAudience(audience);
                                vLog.setAudioAddress(getSdpAddress(sdp));
                                partyInfos.add(vLog);
                                String c = "c";

                            } else {

//                                for (Map.Entry<String, PartyInfo> p : parties.entrySet()) {
//                                    PartyInfo v = p.getValue();
//                                    if ((v.getAudience().equals(to) && v.getSpeaker().equals(from)) || (v.getAudience().equals(from) && v.getSpeaker().equals(to))) {
//                                        uuidString = v.getContactID();
//                                        isContact = true;
//                                        break;
//                                    }
//                                }
                                partyInfos = new ArrayList<>();
                                vLog = new PartyInfo();
                                vLog.setSipCallID(callID);
                                vLog.setSpeaker(speaker);
                                vLog.setAudience(audience);
                                vLog.setAudioAddress(getSdpAddress(sdp));
                                if (!isContact)
                                    vLog.setContactID(uuidString);
                                else
                                    vLog.setContactID(uuidString);
                                partyInfos.add(vLog);
                                parties.put(callID, partyInfos);
                            }
                        }


                        String a = "a";
                    }
                    if (packet.hasHeader(Rtp.ID)) {

                        packet.getHeader(rtp);
                       // System.out.println(rtp.getIndex());
                       // System.out.println(rtp.toString());
                        String rtpDstAddress = GetRtpAddress(packet, RtpAddressParty.Destination);
                        PartyInfo receiver = GetDnByRtpAddress(parties, rtpDstAddress);
                        PartyInfo sourcer = GetDnByRtpAddress(parties, GetRtpAddress(packet, RtpAddressParty.Source));

                        if (sourcer != null && receiver != null) {
                            ProcessRTP(rtp, sourcer, receiver);
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void FlushOutStream(PartyInfo party, int streamsSize) {

        Vector<AudioInputStream> strmVctr = party.getVoiceStreamVector();
        if (strmVctr.size() > streamsSize) {

            String timeStamp = simpleDateFormat.format(new Date());
            //this.view.setParams(timeStamp, party.getSpeaker() + '-' + party.getAudience());
            //this.view.setParams(timeStamp, calling + '-' + called);

//			log.info(party.getSpeaker() + '-' + party.getAudience() + "; timeStamp: " + timeStamp + "; StrmVctr.size: " + strmVctr.size());
            SendStreams(strmVctr, pcmFormat, party);
            strmVctr.clear();
        }
    }


    private Map<String, String> getExtraParams() {
        boolean isAddPunct = false;
        boolean isTransDigit = false;
        boolean isButterFly = false;

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put("sessionid", "sessionid");
        extraParams.put("reqid", "reqid");
        extraParams.put(ViewData.IS_ADD_PUNCT, isAddPunct ? ViewData.IS_ADD_PUNCT_ON : ViewData.IS_ADD_PUNCT_OFF);
        extraParams.put(ViewData.IS_TRANS_DIGIT, isTransDigit ? ViewData.IS_TRANS_DIGIT_ON : ViewData.IS_TRANS_DIGIT_OFF);
        extraParams.put(ViewData.IS_BUTTER_FLY, isButterFly ? ViewData.IS_BUTTER_FLY_ON : ViewData.IS_BUTTER_FLY_OFF);
        return extraParams;
    }

    //private SendStreamByte sendStreamByte = null;

    private void SendPcm(AudioInputStream ulawStream, AudioFormat format, PartyInfo party) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(format, ulawStream);
        AudioSystem.write(pcmStream, AudioFileFormat.Type.WAVE, outputStream);
        outputStream.close();

        String timeStamp = simpleDateFormat.format(new Date());
        String caller = party.getSpeaker();
        String callee = party.getAudience();
        String callId = party.getSipCallID();
        RtpInform rtpInform = new RtpInform();
        rtpInform.setReceiveDateTime(LocalDateTime.now());
        rtpInform.setContactId(party.getContactID());
        rtpInform.setCallId(callId);
        rtpInform.setCallee(caller);
        rtpInform.setCaller(callee);
        rtpInform.setVoiceData(outputStream.toByteArray().clone());
        pcmStream.close();
        new Thread(() -> {
            try {
                if (this.rtpReceiveListenerTest != null) this.rtpReceiveListenerTest.RtpReceiveTest(rtpInform);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


        //this.connector.sendByte(this.view, outputStream.toByteArray());

    }

    private void SendStreams(Vector<AudioInputStream> streams, AudioFormat format, PartyInfo party) {

        SequenceInputStream seqInputStream = new SequenceInputStream(streams.elements());
        AudioFormat seqFormat = streams.firstElement().getFormat();
        long length = streams.firstElement().getFrameLength() * streams.size();
        AudioInputStream ulawStrm = new AudioInputStream(seqInputStream, seqFormat, length);

        try {

            SendPcm(ulawStrm, format, party);
            ulawStrm.close();
            for (AudioInputStream ais : streams) {

                ais.close();
            }
        } catch (IOException e) {

            //log.error("{}", "SendStreams Error", e);

        }
    }

    private void ProcessRTP(Rtp rtp, PartyInfo speaker, PartyInfo listener) {

        boolean silent = false;
        double rms = getRMS(rtp);
        double delta = Math.abs(rms - speaker.getLastRMS());
//		log.info("speaker" + speaker.getSpeaker() + " rms:" + rms + " delta:" + delta);
        silent = (delta > this.silenceThreshold + 2)
                || (rms < this.silenceThreshold && speaker.getSpeaker() != speaker.getSilencer());
        speaker.setLastRMS(rms);
        speaker.setSilencer(speaker.getSpeaker());
        listener.setSilencer(speaker.getSpeaker());
        //log.info("silent ={} rms ={}", silent, rms);
        if (silent) FlushOutStream(speaker,70);

        if (rms >= this.silenceThreshold) {

            ByteArrayInputStream rtpStream = new ByteArrayInputStream(rtp.getPayload());
            AudioInputStream ulawStrm = new AudioInputStream(rtpStream, ulawFormat, rtp.getPayloadLength());
            speaker.getVoiceStreamVector().addElement(ulawStrm);
        }
    }

    private double getRMS(Rtp rtp) {

        byte[] payload = rtp.getPayload().clone();
        AudioInputStream ulawStrm = new AudioInputStream(new ByteArrayInputStream(payload), ulawFormat, payload.length);
        AudioInputStream pcmStrm = AudioSystem.getAudioInputStream(pcmFormat, ulawStrm);
        byte[] voiceData = new byte[(int) pcmStrm.getFrameLength()];
        int noOfBytes = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {

            while ((noOfBytes = pcmStrm.read(voiceData)) != -1) {

                bos.write(voiceData, 0, noOfBytes);
            }

            bos.close();
            pcmStrm.close();
            ulawStrm.close();
        } catch (IOException e) {

            e.printStackTrace();
            return 50;
        }

        double sum = 0d;
        for (int v : voiceData) {
            sum += v;
        }
        double average = sum / voiceData.length;

        double sumMeanSquare = 0d;
        for (int v : voiceData) {
            sumMeanSquare += Math.pow(v - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / voiceData.length;

        double rootMeanSquare = Math.sqrt(averageMeanSquare);

        return Math.abs(rootMeanSquare);
    }
}
