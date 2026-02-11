package com.cashbee.presentation.controller;

import com.cashbee.presentation.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {

  @GetMapping
  public ResponseEntity<ApiResponse<String>> getAllBanks() {
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success("OKE"));
  }
}
