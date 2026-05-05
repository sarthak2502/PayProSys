package com.payprosys.service;

import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.OrganizationProfileDto;

public interface ProfileService {

    OrganizationProfileDto getOrganizationProfile(SessionInfo session);

    void updateOrganizationLogo(SessionInfo session, String logoUrl);

    void changePassword(SessionInfo session, String currentPassword, String newPassword);
}
