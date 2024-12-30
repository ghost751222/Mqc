package com.mqc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ChatRecordMaster")
public class ChatRecordMaster {


    @Id
    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String callId;


    private int agentID;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String agentAccount;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String agentName;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String agentExtension;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String direction;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String customerNumber;
    
    private LocalDateTime createTime;



}

