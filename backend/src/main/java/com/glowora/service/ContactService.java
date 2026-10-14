package com.glowora.service;

import com.glowora.dto.ContactRequestDto;
import com.glowora.dto.ContactResponseDto;

import java.util.List;

public interface ContactService {
    ContactResponseDto submitContactMessage(ContactRequestDto requestDto);
    List<ContactResponseDto> getAllMessages();
}
