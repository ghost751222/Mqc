package com.mqc.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ChatRecordDetail")
public class ChatRecordDetail {
    @Id
    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String uniCallId;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String callId;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String contactId;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String caller;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String callee;


    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String message;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String translate;

    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;


    @Transient
    @JsonIgnore
    private transient int index;

    @Transient
    @JsonIgnore
    private transient byte[] voiceData;

    @Transient
    @JsonIgnore
    private transient String wavfile;

}

