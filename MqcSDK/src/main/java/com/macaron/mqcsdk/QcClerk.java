package com.macaron.mqcsdk;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.AbstractMessageHeader;
import org.jnetpcap.packet.AbstractMessageHeader.MessageType;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Udp;
import org.jnetpcap.protocol.voip.Rtp;
import org.jnetpcap.protocol.voip.Sdp;
import org.jnetpcap.protocol.voip.Sip;
import org.jnetpcap.protocol.voip.Sip.Fields;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class QcClerk {

    private static final float sampleRate = 8000f;

    private static final int sampleSize_8 = 8;
    private static final int sampleSize_16 = 16;
    private static final int channels = 1;
    private static final float frameRate = sampleRate;

    private static AudioFormat pcmformat = new AudioFormat(8000, 16, 1, true, false);
    private static AudioFormat ulawformat= new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);
    //private final AudioFormat pcmFormat = new AudioFormat(sampleRate, sampleSize_16, 1, true, false);
    //private final AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, sampleRate, sampleSize_8, channels, (sampleSize_8 / 8) * channels, frameRate, true);
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private enum RtpAddressParty {Source, Destination}

    private String xCallUUID = "X-Genesys-CallUUID";

    @Getter
    @Setter
    private boolean toDebugPartyInfo = false;
    @Getter
    @Setter
    private boolean toDebugRTP = false;

    @Getter
    @Setter
    private boolean toDebugSIP = false;

    private Pcap parser = null;

    private String deviceMac = null;
    private double silenceThreshold = 20;

    @Setter
    @Getter
    private long silenceSeconds = 2;

    private int streamSize;

    @Setter
    private RtpReceiveListener rtpReceiveListener = null;

    public QcClerk(String deviceMac) {
        this(deviceMac, 20, 50);
    }


    public QcClerk(String deviceMac, double silenceThreshold, int streamSize) {

        this.deviceMac = deviceMac.trim();
        if (silenceThreshold > 0) this.silenceThreshold = silenceThreshold;
        if (streamSize > 0) this.streamSize = streamSize;
    }


    public void read(String PCAP_FILE) {
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


    public void Start() throws SocketException {

        Start(Pcap.LOOP_INFINITE);
    }

    public void Start(int loopCount) throws SocketException {


        try {
            log.info("silenceThreshold ={} , silenceSeconds ={},streamSize={}",silenceThreshold,silenceSeconds,streamSize);
            this.parser = getShark(this.deviceMac);
            //this.connector.setHandler(printer);
//			this.parser.activate();

            this.parser.loop(loopCount, getPacketConsumer(), null);
        } catch (Exception e) {

            this.parser.close();
            log.error("{}", "Start", e);
        }
    }

    public void Stop() {

        if (this.parser == null) return;

        this.parser.close();
        log.info("ByeBye!");
    }


    //	for Pcap
    private boolean SetPcapFilter(String expression, Pcap parser) {

//		it's jnetpcap's bug !!!
        PcapBpfProgram filter = new PcapBpfProgram();
        if (parser.compile(filter, expression, 0, 0xFFFFFF00) == Pcap.OK)
            return parser.setFilter(filter) == Pcap.OK;

        return false;
    }

    private String getHostIP() {

        try {

            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getDeviceMac(PcapIf device) {

        try {
            return FormatUtils.mac(device.getHardwareAddress()).toLowerCase();
        } catch (Exception e) {

            return "";
        }
    }

    private Pcap getShark(String deviceMac) throws SocketException {
        String os = getSystemOS();
        Pcap parser = null;
        StringBuilder errbuf = new StringBuilder();
        ArrayList<PcapIf> allDevs = new ArrayList<PcapIf>();

        if (Pcap.findAllDevs(allDevs, errbuf) != Pcap.OK) {
            log.error("Can't read list of devices, reason: '{}'", errbuf.toString());
            return parser;
        }

        log.info("Network interface devices found on OS {}", os);
        int i = 0;
        for (PcapIf device : allDevs) {
            String description = (device.getDescription() != null) ? device.getDescription() : (device.getName() != null) ? device.getName() : "No description available";
            if (os.contains("win"))
                log.info("#{}: {} [{}] MacAddress is {}", i++, device.getName(), description, getDeviceMac(device));
            else
                log.info("#{}: {} [{}]", i++, device.getName(), description);
        }
        ;

        PcapIf device = getDeviceByMacOrDesc(allDevs, deviceMac);

        parser = Pcap.openLive(device.getName(),
                Pcap.DEFAULT_SNAPLEN, // Capture all packets, no trucation
                Pcap.MODE_PROMISCUOUS,// capture all packets
                Pcap.DEFAULT_TIMEOUT, // 10 * 1000; // 10 seconds in millis
                errbuf);


        boolean filterOK = SetPcapFilter("not (dst host 224.0.0.252 and dst port 5335)", parser);
        //boolean filterOK = SetPcapFilter("sip or rtp", parser);
        log.info("Network interface Device opened: " + getDeviceName(device));
        return parser;
    }

    private PcapIf getDeviceByDesc(ArrayList<PcapIf> allDevs, String description) {

        if (allDevs == null
                || allDevs.isEmpty()
                || description == null
                || description.equals("")) return null;
        try {

            return allDevs.stream().filter(d -> d.getDescription().equals(description)).findFirst().get();
        } catch (Exception e) {

            System.err.printf("Wrong device ID");
            return null;
        }
    }

    private PcapIf getDeviceByMacOrDesc(ArrayList<PcapIf> allDevs, String macOrDescription) {
        String os = getSystemOS();
        if (allDevs == null
                || allDevs.isEmpty()
                || macOrDescription == null
                || macOrDescription.equals("")) return null;

        try {
            int targetIndex = -1;
            int i = 0;
            for (PcapIf d : allDevs) {
                String description = (d.getDescription() != null) ? d.getDescription() : "No description available";
                if (os.contains("win")) {
                    if (getDeviceMac(d).equals(macOrDescription) || d.getName().equalsIgnoreCase(macOrDescription)) {
                        targetIndex = i;
                        break;
                    }
                } else {
                    if (d.getName().equalsIgnoreCase(macOrDescription)) {
                        targetIndex = i;
                        break;
                    }
                }


                i++;
            }
            if (targetIndex == -1) {
                throw new Exception("Wrong Network interface device " + macOrDescription);
            }
            return allDevs.get(targetIndex);
        } catch (Exception e) {
            log.error("{}", "getDeviceByDesc Error ", e);
            return null;
        }
    }

    private String getDeviceName(PcapIf device) {
        return (device.getDescription() != null) ? device.getDescription() : (device.getName() != null) ? device.getName() : "No description available";
    }

    private JPacketHandler<String> getPacketConsumer() {

        return new JPacketHandler<String>() {

            final ConcurrentHashMap<String, List<PartyInfo>> parties = new ConcurrentHashMap<>();
            boolean started = false;

            @Override
            public void nextPacket(JPacket packet, String user) {

                if (!started) {

                    log.info("packet coming...");
                    started = true;
                }

                Sip sip = new Sip();
                Sdp sdp = new Sdp();
                Rtp rtp = new Rtp();
                try {

                    if (packet.hasHeader(Sip.ID)) {

                        packet.getHeader(sip);
                        if (sip.getMethod() == Sip.Method.REGISTER) return;
                        if (packet.hasHeader(Sdp.ID)) packet.getHeader(sdp);
//	        			for dubug
                        ShowSipInfo(sip, sdp, parties);
                        ProcessSIP(sip, sdp, parties);
                    }
                    if (packet.hasHeader(Rtp.ID)) {
                        //for debug
                        packet.getHeader(rtp);
                        ShowRtpInfo(packet, rtp);

                        String rtpDstAddress = GetRtpAddress(packet, RtpAddressParty.Destination);

//	        			for fix bug in jNetpcap's filtering
//	        			log.info(rtpDstAddress);
                        if (rtpDstAddress.equals("224.0.0.252:5355")) {
                            //log.info("jNetpcap's bug bug bug");
                            return;
                        }

                        PartyInfo receiver = GetDnByRtpAddress(parties, rtpDstAddress);
                        PartyInfo sourcer = GetDnByRtpAddress(parties, GetRtpAddress(packet, RtpAddressParty.Source));


                        if (sourcer != null && receiver != null) {
                            ProcessRTP(rtp, sourcer, receiver);
                        }
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    log.error("{}", "nextPacket ", e);
                }
            }
        };
    }


    //	for RTP
    private void ShowRtpInfo(JPacket packet, Rtp rtp) {

        if (!toDebugRTP) return;
        log.info("rtp info : {}", packet.toString());

        if (packet.getPacketWirelen() != 214) return;

        int pkt_version =      (packet.getByte(42) >> 6);
        int pkt_padding =      (packet.getByte(42) >> 5) & 0x01;
        int pkt_extension =    (packet.getByte(42) >> 4) & 0x01;
        int pkt_csrc_count =   (packet.getByte(42) >> 0) & 0x0F;
        int pkt_marker =       (packet.getByte(43) >> 7) & 0x01;
        int pkt_payload_type = (packet.getByte(43) >> 0) & 0x7F;

        int rtp_version =      (rtp.getByte(0) >> 6);
        int rtp_padding =      (rtp.getByte(0) >> 5) & 0x01;
        int rtp_extension =    (rtp.getByte(0) >> 4) & 0x01;
        int rtp_csrc_count =   (rtp.getByte(0) >> 0) & 0x0F;
        int rtp_marker =       (rtp.getByte(1) >> 7) & 0x01;
        int rtp_payload_type = (rtp.getByte(1) >> 0) & 0x7F;

        log.info("version: " + pkt_version + "; padding: " + pkt_padding + "; extension: " + pkt_extension + "; csrc_count: " + pkt_csrc_count + "; marker: " + pkt_marker + "; payload_type: " + pkt_payload_type);
        log.info("version: " + rtp_version + "; padding: " + rtp_padding + "; extension: " + rtp_extension + "; csrc_count: " + rtp_csrc_count + "; marker: " + rtp_marker + "; payload_type: " + rtp_payload_type);
        log.info("version: " + rtp.version() + "; padding: " + rtp.paddingLength() + "; extension: " + rtp_extension + "; csrc_count: " + rtp.csrcLength() + "; marker: " + rtp.hasMarker() + "; payload_type: " + rtp.type());
//        uint rtp_sequence_number = ((uint)rtp.getByte(2) << 8) + (uint)(rtp.getByte(3));
//        uint rtp_timestamp = ((uint)rtp.getByte(4) <<24) + (uint)(rtp.getByte(5) << 16) + (uint)(rtp.getByte(6) << 8) + (uint)(rtp.getByte(7));
    }

    private String GetAmplitude(byte[] buffers) {

        String rs = "";
        for (int i = 0; i < buffers.length; i++) {

            int n = buffers[i];
            rs += (i > 0) ? ", " + n : n;
        }
        return rs;
    }


    private double getRMS(Rtp rtp) {

        byte[] payload = rtp.getPayload().clone();
        AudioInputStream ulawStrm = new AudioInputStream(new ByteArrayInputStream(payload), ulawformat, payload.length);
        AudioInputStream pcmStrm = AudioSystem.getAudioInputStream(pcmformat, ulawStrm);
        byte[] voiceData = new byte[(int)pcmStrm.getFrameLength()];
        int noOfBytes = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {

            while((noOfBytes = pcmStrm.read(voiceData)) != -1) {

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
        for(int v : voiceData) {
            sum += v;
        }
        double average = sum / voiceData.length;

        double sumMeanSquare = 0d;
        for(int v : voiceData) {
            sumMeanSquare += Math.pow(v - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / voiceData.length;

        double rootMeanSquare = Math.sqrt(averageMeanSquare);
        if(toDebugRTP)
           log.info("rms: " + Math.abs(rootMeanSquare)+";\n PCM Amplitude: " + GetAmplitude(voiceData));
        return Math.abs(rootMeanSquare);
    }


//    private void ProcessRTP(Rtp rtp, PartyInfo speaker, PartyInfo listener) {
//
//        boolean silent = false;
//        double rms = getRMS(rtp);
//        double delta = Math.abs(rms - speaker.getLastRMS());
//        boolean isFlush = false;
//        silent = (delta > silenceThreshold + 2)
//                || (rms < silenceThreshold && !Objects.equals(speaker.getSpeaker(), speaker.getSilencer()));
//        speaker.setLastRMS(rms);
//        speaker.setSilencer(speaker.getSpeaker());
//
//        //listener.setSilencer(speaker.getSpeaker());
//        //log.info("speaker {},delta {},silent ={} rms ={},sequence={}", speaker.getSpeaker(), delta, silent, rms, rtp.sequence());
//
//
//        if (silent) {
//            isFlush = FlushOutStream(speaker, streamSize);
//        }
//
//
//        //if (rms >= silenceThreshold) {
//
//        ByteArrayInputStream rtpStream = new ByteArrayInputStream(rtp.getPayload());
//        AudioInputStream ulawStrm = new AudioInputStream(rtpStream, ulawFormat, rtp.getPayloadLength());
//        speaker.getVoiceStreamVector().addElement(ulawStrm);
//        //}
//    }

    private void ProcessRTP(Rtp rtp, PartyInfo speaker, PartyInfo listener) {

        boolean silent = false;
        double rms = getRMS(rtp);
        double delta = Math.abs(rms - speaker.getLastRMS());
        boolean isFlush = false;
        silent = (delta > silenceThreshold + 2) || (rms < silenceThreshold && !Objects.equals(speaker.getSpeaker(), speaker.getSilencer()));
        silent = (rms < silenceThreshold);
        speaker.setLastRMS(rms);
        speaker.setSilencer(speaker.getSpeaker());
        //listener.setSilencer(speaker.getSpeaker());

        //if (speaker.getSpeaker().equals("0988809097")) {


        int size = speaker.getVoiceStreamVector().size();
        LocalDateTime packetDateTime = LocalDateTimeUtils.fromLong(rtp.getPacket().getCaptureHeader().timestampInMillis());
        LocalDateTime lastSilenceDateTime = speaker.getLastSilenceDateTime() == null ? packetDateTime : speaker.getLastSilenceDateTime();
        Duration duration = Duration.between(lastSilenceDateTime, packetDateTime);

//        if (speaker.getSpeaker().equals("1999")) {
//            log.info("speaker {},delta {},silent ={} rms ={},start ={},end ={},sequence={}", speaker.getSpeaker(), delta, silent, rms, lastSilenceDateTime, packetDateTime, rtp.sequence());
//        }


        if (silent) {
            isFlush = FlushOutStream(speaker, streamSize);

            if (!isFlush) {
                if (duration.getSeconds() >= silenceSeconds) {
                    isFlush = FlushOutStream(speaker, 0);
                }
            }

            if (isFlush) {
//                if (speaker.getSpeaker().equals("1999")) {
//                    log.info("start ={},end ={},seconds ={},streamSize={},rtp seq={}", lastSilenceDateTime, packetDateTime, duration.getSeconds(), size, rtp.sequence());
//                }
                speaker.setLastSilenceDateTime(null);
            } else
                speaker.setLastSilenceDateTime(lastSilenceDateTime);
        } else {
            speaker.setLastSilenceDateTime(packetDateTime);
        }


        //if (rms >= silenceThreshold) {
        ByteArrayInputStream rtpStream = new ByteArrayInputStream(rtp.getPayload());
        AudioInputStream ulawStrm = new AudioInputStream(rtpStream, ulawformat, rtp.getPayloadLength());
        speaker.getVoiceStreamVector().addElement(ulawStrm);

        //speaker.setLastSilenceDateTime(packetDateTime);
        //}
        // }


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

//    private PartyInfo GetDnByRtpAddress(ConcurrentHashMap<String, PartyInfo> parties, String value) {
//
//        for (String key : parties.keySet()) {
//
//            if (parties.get(key).getAudioAddress().equals(value)) {
//
////	        	will return the first found
//                return parties.get(key);
//            }
//        }
//        return null;
//    }

    private boolean FlushOutStream(PartyInfo party, int streamsSize) {

        boolean isFlush = false;
        Vector<AudioInputStream> strmVctr = party.getVoiceStreamVector();
        if (strmVctr.size() > streamsSize) {

            String timeStamp = simpleDateFormat.format(new Date());
            //this.view.setParams(timeStamp, party.getSpeaker() + '-' + party.getAudience());
            //this.view.setParams(timeStamp, calling + '-' + called);

//			log.info(party.getSpeaker() + '-' + party.getAudience() + "; timeStamp: " + timeStamp + "; StrmVctr.size: " + strmVctr.size());

            SendStreams(strmVctr, pcmformat, party);
            strmVctr.clear();
            isFlush = true;
        }
        return isFlush;
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

            log.error("{}", "SendStreams Error", e);

        }
    }

    private void SendPcm(AudioInputStream ulawStream, AudioFormat format, PartyInfo party) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(format, ulawStream);
        AudioSystem.write(pcmStream, AudioFileFormat.Type.WAVE, outputStream);
        outputStream.close();

        //String timeStamp = simpleDateFormat.format(new Date());
        String caller = party.getSpeaker();
        String callee = party.getAudience();
        String callId = party.getSipCallID();
        RtpInform rtpInform = new RtpInform();
        rtpInform.setReceiveDateTime(LocalDateTime.now());
        rtpInform.setContactId(party.getContactID());
        rtpInform.setCallId(callId);
        rtpInform.setCallee(callee);
        rtpInform.setCaller(caller);
        rtpInform.setIndex(party.getIndex());
        rtpInform.setVoiceData(outputStream.toByteArray().clone());
        pcmStream.close();
        try {
            if (this.rtpReceiveListener != null) this.rtpReceiveListener.RtpReceive(rtpInform);
        } catch (Exception e) {
            log.error(e.toString());
        }
        //this.connector.sendByte(this.view, outputStream.toByteArray());

    }


    //	for SIP
    private void ShowSipInfo(Sip sip, Sdp sdp, ConcurrentHashMap<String, List<PartyInfo>> parties) {


        if (toDebugSIP) {
            log.info("SIP info : {}", sip.toString());
        }

        if (toDebugPartyInfo) {
            log.info("###Parties size: " + parties.size());
            for (Map.Entry<String, List<PartyInfo>> set : parties.entrySet()) {
                for (PartyInfo v : set.getValue()) {
                    log.info(
                            " Speaker: [" + v.getSpeaker()
                                    + "] ,Audience: [" + v.getAudience()
                                    + "], SipCallID: [" + v.getSipCallID()
                                    + "], AudioAddress: [" + v.getAudioAddress()
                                    + "], GenesysCallUUID: [" + v.getGenesysCallUUID()
                                    + "], receiveTime: [" + v.getReceiveDateTime() + "]");
                }

            }
        }

    }

    private void ProcessSIP(Sip sip, Sdp sdp, ConcurrentHashMap<String, List<PartyInfo>> parties) {

        if (IsResponse200(sip, "200")
                || sip.hasField(Sip.Request.RequestMethod)) {

            ParseDnAddress(sip, sdp, parties);
        } else {

            String sipMsgCode = sip.hasField(Sip.Response.ResponseCode)
                    ? sip.fieldValue(Sip.Response.ResponseCode) + " " + sip.fieldValue(Sip.Response.ResponseCodeMsg)
                    : "Ooooops!";
//			log.info("\nsipMsgType:" + sip.getMessageType() + "; sipMsgCode:" + sipMsgCode);
        }
    }

    private void ParseDnAddress(Sip sip, Sdp sdp, ConcurrentHashMap<String, List<PartyInfo>> parties) {

        MessageType sipMsgType = sip.getMessageType();
        String sipMsgCode = null;
        String responseCode = sip.fieldValue(Sip.Response.ResponseCode);
        if (sipMsgType == MessageType.RESPONSE) {
            sipMsgCode = sip.fieldValue(Sip.Response.ResponseCode);
            if (toDebugSIP)
                log.info("\nsipMsgType:" + sipMsgType + "; sipMsgCode:" + sipMsgCode + " " + sip.fieldValue(Sip.Response.ResponseCodeMsg));
        } else {

            sipMsgCode = sip.fieldValue(Sip.Request.RequestMethod);
            if (toDebugSIP) log.info("\nsipMsgType:" + sipMsgType + "; sipMsgCode:" + sipMsgCode);
        }


        if (sip.hasField(Fields.To)
                && sip.hasField(Fields.From)
                && sip.hasField(Fields.Call_ID)) {


            String callID = sip.fieldValue(Fields.Call_ID);

            if (sip.getMethod() == Sip.Method.BYE || sip.getMethod() == Sip.Method.CANCEL) {

                for (Map.Entry<String, List<PartyInfo>> set : parties.entrySet()) {
                    for (PartyInfo p : set.getValue()) {
                        if (p.getSipCallID().equals(callID)) FlushOutStream(p, 0);
                    }
                }
                parties.remove(callID);
            }
            //if ((sipMsgCode.equals("200") || sipMsgCode.equals("INVITE")) && (GetSipFieldValue(sip, Fields.CSeq).equals("1 INVITE") && GetSipFieldValue(sip, xCallUUID) != null)) {
            //if ((sipMsgCode.equals("200") || sipMsgCode.equals("INVITE")) && (GetSipFieldValue(sip, Fields.CSeq).equals("1 INVITE"))) {
            //else if ((sip.getMethod() == Sip.Method.INVITE && !"180".equals(responseCode)) || "200".equals(responseCode)) {
            else if ((sip.getMethod() == Sip.Method.INVITE && !"180".equals(responseCode) && !"Trying".equalsIgnoreCase(sipMsgCode) && !"Session".equalsIgnoreCase(sipMsgCode)) || (sip.getMethod() != Sip.Method.PRACK && sip.getMethod() != Sip.Method.OPTIONS && "200".equals(responseCode) && (sipMsgType == AbstractMessageHeader.MessageType.RESPONSE))) {


                String to = getSipDN(sip.fieldValue(Fields.To));
                String from = getSipDN(sip.fieldValue(Fields.From));
                String speaker = (sipMsgType == MessageType.REQUEST) ? from : to;
                String audience = (sipMsgType == MessageType.RESPONSE) ? from : to;
                String genesysCallUUID = GetSipFieldValue(sip, xCallUUID);
                boolean isContact = false;

                List<PartyInfo> partyInfos = null;
                PartyInfo vLog = null;
                
                if (parties.containsKey(callID)) {
                    partyInfos = parties.get(callID);
                    vLog = new PartyInfo();
                    vLog.setSipCallID(callID);
                    vLog.setSpeaker(speaker);
                    vLog.setAudience(audience);
                    vLog.setAudioAddress(getSdpAddress(sdp));
                    vLog.setIndex(partyInfos.size() + 1);
                    partyInfos.add(vLog);
                } else {
                    String uuidString = UUID.randomUUID().toString();
//                    for (Map.Entry<String, PartyInfo> p : parties.entrySet()) {
//                        PartyInfo v = p.getValue();
//                        if ((v.getAudience().equals(to) && v.getSpeaker().equals(from)) || (v.getAudience().equals(from) && v.getSpeaker().equals(to))) {
//                            uuidString = v.getContactID();
//                            isContact = true;
//                            break;
//                        }
//                    }
                    partyInfos = new ArrayList<>();
                    vLog = new PartyInfo();
                    vLog.setSpeaker(speaker);
                    vLog.setAudience(audience);
                    vLog.setAudioAddress(getSdpAddress(sdp));
                    vLog.setSipCallID(callID);
                    if (!isContact)
                        vLog.setContactID(uuidString);
                    else
                        vLog.setContactID(uuidString);
                    vLog.setIndex(1);
                    vLog.setReceiveDateTime(LocalDateTime.now());
                    vLog.setGenesysCallUUID(genesysCallUUID);
                    partyInfos.add(vLog);
                    parties.put(callID, partyInfos);
                }
                //} else if (sipMsgCode.equals("BYE") || sipMsgCode.equals("CANCEL")) {
            }
        }
    }

    private boolean IsResponse200(Sip sip, String responseCode) {

        return sip.hasField(Sip.Response.ResponseCode)
                && sip.fieldValue(Sip.Response.ResponseCode).equals("200");
    }

    private String GetSipFieldValue(Sip sip, Fields field) {

        return sip.hasField(field) ? sip.fieldValue(field) : null;
    }

    private String GetSipFieldValue(Sip sip, String xFieldName) {

        String field = xFieldName + ": ";
        String sipText = sip.getUTF8String(0, sip.getLength());

        int pos = sipText.indexOf(field);
        if (pos == -1)
            return null;
        else {
            pos += field.length();
            int end = pos;
            while (sipText.charAt(end) != '\r') end++;

            return sipText.substring(pos, end);
        }
    }

    private String getSipDN(String fldValue) {

        String token = "<sip:";
        int start = fldValue.indexOf(token);
        int end = fldValue.indexOf('@');
        return fldValue.substring(start + token.length(), end);
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

    private String getSystemOS() {
        return System.getProperty("os.name").toLowerCase();
    }

}
