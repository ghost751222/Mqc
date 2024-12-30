package com.mqc.controller;

import com.mqc.service.ChatRecordMasterService;
import com.mqc.utils.JacksonUtils;
import com.mqc.vo.RequestQueryVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import javax.xml.ws.Action;
import java.util.Map;

@Controller
@RequestMapping(value="/chatRecordMaster")
public class ChatRecordMasterController {

    @Autowired
    HttpSession httpSession;

    @Autowired
    ChatRecordMasterService chatRecordMasterService;

    @RequestMapping("")
    public String page(Model model){
        return "chatRecordMaster";
    }

    @RequestMapping(value = "/page/data")
    @ResponseBody
    public Page<Object> index(@RequestParam Map<String, Object> params) {

        String extension = (String) httpSession.getAttribute("extension");
        boolean isAdmin = (boolean) httpSession.getAttribute("isAdmin");
        if (extension == null && !isAdmin) {
            return null;
        } else {
            RequestQueryVo requestQueryVo = JacksonUtils.mapToObject(params, RequestQueryVo.class);
            if(!isAdmin){
                requestQueryVo.setAgentExtension(extension);
            }

            String sortSource = requestQueryVo.getSort();
            String sortField = null;
            String sortDirection = null;
            int page = requestQueryVo.getPageIndex() - 1;
            int size = requestQueryVo.getPageSize();
            PageRequest pageRequest = null;
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

            return chatRecordMasterService.findAllByAgentExtensionOrCustomerNumber(requestQueryVo, pageRequest);
        }

    }
}
