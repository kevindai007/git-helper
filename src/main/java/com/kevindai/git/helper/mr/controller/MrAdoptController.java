package com.kevindai.git.helper.mr.controller;

import com.kevindai.git.helper.mr.dto.AdoptResponse;
import com.kevindai.git.helper.mr.service.MrAdoptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/mr", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MrAdoptController {

    private final MrAdoptService mrAdoptService;

    @PostMapping("/adopt/{detailId}")
    public AdoptResponse adopt(@PathVariable("detailId") long detailId) {
        try {
            mrAdoptService.adoptRecommendation(detailId);
            return AdoptResponse.builder().status("success").message("Comment created.").build();
        } catch (Exception e) {
            return AdoptResponse.builder().status("failure").message(e.getMessage()).build();
        }
    }
}

