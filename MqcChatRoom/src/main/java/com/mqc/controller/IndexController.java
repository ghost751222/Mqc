package com.mqc.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = {"/",""})
public class IndexController {
    @RequestMapping(value = "/chatlink")
    public String chatLinkPage(){return "chatlink";}

    @RequestMapping(value = "")
    public String page(){return "index";}

    @RequestMapping(value = "/chat2")
    public String chat2Page(){return "chat2";}
}
