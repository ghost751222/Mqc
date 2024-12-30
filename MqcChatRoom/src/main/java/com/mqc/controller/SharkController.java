package com.mqc.controller;

import com.mqc.componet.QcComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class SharkController {

    @Autowired
    QcComponent qcComponent;


    @RequestMapping("/read")
    @ResponseBody
    public String read(@RequestParam Map<String, String> params) {
        String PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\" + params.get("file");
        //PCAP_FILE = "C:\\Users\\YY-PC\\Desktop\\AIA_SUSE15\\mute.pcap";
        qcComponent.getQcClerk().read(PCAP_FILE);
        return "read";
    }

    @RequestMapping("/shark")
    public String shark() {
        return "shark";
    }


    @RequestMapping("/startMonitor")
    @ResponseBody
    public String startMonitor() {
        if (!qcComponent.getServerStatus()) qcComponent.startQcClerk();
        return "startMonitor Successful";
    }

    @RequestMapping("/stopMonitor")
    @ResponseBody
    public String stopMonitor() {
        if (qcComponent.getServerStatus()) qcComponent.stopQcClerk();
        return "stopMonitor Successful";
    }

    @RequestMapping("/serverStatus")
    @ResponseBody
    public boolean serverStatus() {
        return qcComponent.getServerStatus();
    }

}
