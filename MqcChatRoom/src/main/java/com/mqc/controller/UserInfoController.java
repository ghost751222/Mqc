package com.mqc.controller;

import com.mqc.entity.UserInfo;
import com.mqc.service.UserInfoService;
import com.mqc.utils.JacksonUtils;
import com.mqc.vo.RequestQueryVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "userInfo")
public class UserInfoController {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserInfoService userInfoService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String page() {
        return "userInfo";
    }


    @RequestMapping(value = "/findAllByIsAdmin", method = RequestMethod.GET)
    @ResponseBody
    public List<UserInfo> findAllByIsAdmin() {
        return userInfoService.findAllByIsAdmin(false);
    }


    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public Page<UserInfo> findAll(@RequestParam Map<String, Object> params) {
        RequestQueryVo requestQueryVo = JacksonUtils.mapToObject(params, RequestQueryVo.class);
        String sortSource = requestQueryVo.getSort();
        String sortField = null;
        String sortDirection = null;
        int page = requestQueryVo.getPageIndex() - 1;
        int size = requestQueryVo.getPageSize();
        PageRequest pageRequest = null;
        String account = (String) params.get("account");
        if (!Strings.isEmpty(sortSource)) {


            sortField = sortSource.split("\\|")[0];
            sortDirection = sortSource.split("\\|")[1];
            Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                    ? Sort.by(sortField).ascending()
                    : Sort.by(sortField).descending();
            pageRequest = PageRequest.of(page, size, sort);
        } else {
            pageRequest = PageRequest.of(page, size);
        }

        return userInfoService.findAllByAccount(account, pageRequest);
    }


    @RequestMapping(value = "/data/{account}", method = RequestMethod.GET)
    @ResponseBody
    public UserInfo findByAccount(@PathVariable("account") String account) {
        return userInfoService.findByAccount(account);
    }


    @RequestMapping(value = "", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseBody
    public UserInfo save(@RequestBody UserInfo user) {
        userInfoService.save(user);
        return user;
    }


    @RequestMapping(value = "", method = RequestMethod.DELETE)
    @ResponseBody
    public UserInfo delete(@RequestBody UserInfo user) {
        userInfoService.delete(user);
        return user;
    }

    @RequestMapping(value = "rePassword", method = RequestMethod.POST)
    @ResponseBody
    public UserInfo rePassword(@RequestBody String password, HttpSession session) throws IOException {
        UserInfo user = (UserInfo) session.getAttribute("userInfo");
        password = JacksonUtils.toJsonNode(password).get("password").asText();
        user.setPassword(passwordEncoder.encode(password));
        return userInfoService.save(user);
    }

    @RequestMapping(value = "resetPassword", method = RequestMethod.POST)
    @ResponseBody
    public UserInfo resetPassword(@RequestBody UserInfo user, HttpSession session) throws IOException {
        user.setPassword("");
        return userInfoService.save(user);

    }

}
