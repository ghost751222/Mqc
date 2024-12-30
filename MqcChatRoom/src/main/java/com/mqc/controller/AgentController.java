package com.mqc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@Controller
@RequestMapping(value = "/agent")
public class AgentController {

    @RequestMapping("")
    public String page() {return "agent";}

}
