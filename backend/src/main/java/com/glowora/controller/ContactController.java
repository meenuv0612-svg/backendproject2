package com.glowora.controller;

import com.glowora.dto.ContactRequestDto;
import com.glowora.dto.ContactResponseDto;
import com.glowora.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactResponseDto> submitContactMessage(@Valid @RequestBody ContactRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactService.submitContactMessage(requestDto));
    }

    @GetMapping
    public ResponseEntity<List<ContactResponseDto>> getAllMessages() {
        return ResponseEntity.ok(contactService.getAllMessages());
    }
}
