package com.mqc.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping(value = "/api")
@Slf4j
public class ApiController {


    @RequestMapping(value = "/getBase64", method = RequestMethod.GET)
    @ResponseBody
    public String convertToBase64() throws IOException {


        try {
            File file = new File("D:\\Consiliuminc\\IntelljWorkspace\\Mqc\\MqcClientDemo\\46454194.wav");
            byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "fail";
        }

    }

    @RequestMapping(value = "/saveBase64", method = {RequestMethod.POST})
    @ResponseBody
    public String convertToBase64(@RequestBody String base64String) {

        try {
            byte[] decoded = Base64.decodeBase64(base64String);
            Files.write(Paths.get("a.wav"), decoded);

        } catch (Exception e) {
            return "fail";
        }
        return "OK";
    }

}
