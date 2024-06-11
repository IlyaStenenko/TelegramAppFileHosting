package com.example.webservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/web-service")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    @CrossOrigin(origins = "*")
    @PostMapping("/givePhoto")
    public String takePhotoMessage(@RequestParam("file") MultipartFile file) {
        log.info("Method takePhotoMessage start");
        System.out.println(file.getContentType());
        System.out.println(file.getOriginalFilename());

        log.info("Method takePhotoMessage end");
        return "200";
    }
}
