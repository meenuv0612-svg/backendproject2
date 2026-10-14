package com.glowora.service.impl;

import com.glowora.dto.ContactRequestDto;
import com.glowora.dto.ContactResponseDto;
import com.glowora.entity.ContactMessage;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.ContactMessageRepository;
import com.glowora.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMessageRepository contactMessageRepository;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public ContactResponseDto submitContactMessage(ContactRequestDto requestDto) {
        ContactMessage message = ContactMessage.builder()
                .fullName(requestDto.getFullName().trim())
                .email(requestDto.getEmail().trim().toLowerCase())
                .phone(requestDto.getPhone() != null ? requestDto.getPhone().trim() : null)
                .subject(requestDto.getSubject().trim())
                .message(requestDto.getMessage().trim())
                .status("RECEIVED")
                .build();

        ContactMessage saved = contactMessageRepository.save(message);
        return entityMapper.toContactDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponseDto> getAllMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(entityMapper::toContactDto)
                .toList();
    }
}
