package com.macaron.mqcsdk;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;

@Data
public class PartyInfo {

	private String audioAddress;
	private double lastRMS;
	private String silencer;
//	DN for rtp source
	private String speaker;
//	DN for rtp destination; to be "private List<String> audience"
	private String audience;
	private String sipMsgCode;
	private String sipCallID;
	private String genesysCallUUID;
	private String contactID;

	private int index;

	private LocalDateTime receiveDateTime ;
	
	private Vector<AudioInputStream> voiceStreamVector = new Vector<>();

	private LocalDateTime lastSilenceDateTime;

}
