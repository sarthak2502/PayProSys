package com.payprosys.mapper;

import com.payprosys.dto.UserDto;
import com.payprosys.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User entity) {
        if (entity == null) return null;
        return UserDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .bankId(entity.getBank() != null ? entity.getBank().getId() : null)
                .corporateId(entity.getCorporate() != null ? entity.getCorporate().getId() : null)
                .bankName(entity.getBank() != null ? entity.getBank().getName() : null)
                .corporateName(entity.getCorporate() != null ? entity.getCorporate().getName() : null)
                .roles(entity.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toList()))
                .build();
    }

    /** Same as toDto but includes password (for super admin all-users list only). */
    public UserDto toDtoWithPassword(User entity) {
        UserDto dto = toDto(entity);
        if (dto != null && entity != null) {
            dto.setPassword(entity.getPassword());
        }
        return dto;
    }
}
