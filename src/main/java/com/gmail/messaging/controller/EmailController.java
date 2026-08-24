package com.gmail.messaging.controller;

import com.gmail.messaging.model.EmailRequest;
import com.gmail.messaging.service.GmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    private final GmailService gmailService;

    @Autowired
    public EmailController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendEmail(@Valid @RequestBody EmailRequest request) {
        Map<String, Object> result = gmailService.sendEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody(),
                request.isHtml()
        );

        boolean success = (boolean) result.getOrDefault("success", false);
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
