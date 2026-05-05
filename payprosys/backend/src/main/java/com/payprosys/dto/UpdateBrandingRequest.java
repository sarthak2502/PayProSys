package com.payprosys.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBrandingRequest {

    /** HTTPS URL, optional data URL, or blank to clear. */
    @Size(max = 1024)
    private String logoUrl;
}
