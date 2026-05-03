package com.payprosys.mapper;

import com.payprosys.dto.CorporateDto;
import com.payprosys.entity.Corporate;
import org.springframework.stereotype.Component;

@Component
public class CorporateMapper {

    public CorporateDto toDto(Corporate entity) {
        if (entity == null) return null;
        return CorporateDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .bankId(entity.getBank() != null ? entity.getBank().getId() : null)
                .build();
    }
}
