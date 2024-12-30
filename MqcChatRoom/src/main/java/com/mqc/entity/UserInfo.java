package com.mqc.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "UserInfo")
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String account;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String password;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String extension;

    @Nationalized
    @Column(length = Integer.MAX_VALUE)
    private String userName;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    private LocalDateTime createTime;
}


