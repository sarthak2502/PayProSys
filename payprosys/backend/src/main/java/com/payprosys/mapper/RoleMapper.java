package com.payprosys.mapper;

import com.payprosys.dto.RoleDto;
import com.payprosys.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleDto toDto(Role entity) {
        if (entity == null) return null;
        return RoleDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
