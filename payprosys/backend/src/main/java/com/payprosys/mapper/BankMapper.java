package com.payprosys.mapper;

import com.payprosys.dto.BankDto;
import com.payprosys.entity.Bank;
import org.springframework.stereotype.Component;

@Component
public class BankMapper {

    public BankDto toDto(Bank entity) {
        if (entity == null) return null;
        return BankDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
