package com.mqc.service;

import com.mqc.entity.UserInfo;
import com.mqc.repository.UserInfoRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInfoService {

    private final static String defaultPassword = "123456";

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserInfoRepository userInfoRepository;


    public Page<UserInfo> findAllByAccount(String account, PageRequest pageRequest) {
        return userInfoRepository.findAllByAccount(account, pageRequest);

    }

    public List<UserInfo> findAllByIsAdmin(boolean isAdmin) {
        return userInfoRepository.findAllByIsAdmin(isAdmin);

    }

    public UserInfo findByAccount(String account) {
        return userInfoRepository.findByAccount(account);

    }


    public UserInfo save(UserInfo userInfo) {
        if (Strings.isEmpty(userInfo.getPassword())) {
            userInfo.setPassword(passwordEncoder.encode(defaultPassword));
        }

        return userInfoRepository.save(userInfo);
    }

    public void delete(UserInfo userInfo) {
        userInfoRepository.delete(userInfo);
    }

}
