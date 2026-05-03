package com.payprosys.service;

import com.payprosys.dto.CreateReviewAssignmentRequest;
import com.payprosys.dto.ReviewLevelAssignmentDto;
import com.payprosys.dto.TenantWorkflowConfigDto;
import com.payprosys.dto.WorkflowStepDto;
import com.payprosys.dto.WorkflowStepLabelUpdate;
import com.payprosys.entity.Bank;
import com.payprosys.entity.BankUserReviewLevel;
import com.payprosys.entity.BankWorkflowStep;
import com.payprosys.entity.Corporate;
import com.payprosys.entity.CorporateUserReviewLevel;
import com.payprosys.entity.CorporateWorkflowStep;
import com.payprosys.entity.User;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ResourceNotFoundException;
import com.payprosys.repository.BankUserReviewLevelRepository;
import com.payprosys.repository.BankWorkflowStepRepository;
import com.payprosys.repository.CorporateUserReviewLevelRepository;
import com.payprosys.repository.CorporateWorkflowStepRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowConfigServiceImpl implements WorkflowConfigService {

    private final CorporateWorkflowStepRepository corporateWorkflowStepRepository;
    private final CorporateUserReviewLevelRepository corporateUserReviewLevelRepository;
    private final BankWorkflowStepRepository bankWorkflowStepRepository;
    private final BankUserReviewLevelRepository bankUserReviewLevelRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantWorkflowConfigDto getCorporateConfig(UUID corporateId) {
        List<WorkflowStepDto> steps = corporateWorkflowStepRepository.findByCorporate_IdOrderByStepLevelAsc(corporateId).stream()
                .map(s -> WorkflowStepDto.builder().id(s.getId()).stepLevel(s.getStepLevel()).label(s.getLabel()).build())
                .toList();
        List<ReviewLevelAssignmentDto> assignments = corporateUserReviewLevelRepository.findByCorporate_IdOrderByReviewLevelAsc(corporateId).stream()
                .map(this::toCorpAssignmentDto)
                .toList();
        return TenantWorkflowConfigDto.builder().steps(steps).assignments(assignments).build();
    }

    @Override
    @Transactional
    public void updateCorporateStepLabels(UUID corporateId, List<WorkflowStepLabelUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new BadRequestException("At least one step update is required");
        }
        for (WorkflowStepLabelUpdate u : updates) {
            if (u.getLabel() == null || u.getLabel().isBlank()) {
                throw new BadRequestException("Label is required for step " + u.getStepLevel());
            }
            CorporateWorkflowStep row = corporateWorkflowStepRepository
                    .findByCorporate_IdAndStepLevel(corporateId, u.getStepLevel())
                    .orElseThrow(() -> new BadRequestException("Unknown step level for this corporate: " + u.getStepLevel()));
            row.setLabel(u.getLabel().trim());
            corporateWorkflowStepRepository.save(row);
        }
    }

    @Override
    @Transactional
    public ReviewLevelAssignmentDto addCorporateAssignment(UUID corporateId, CreateReviewAssignmentRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("userId is required");
        }
        if (request.getReviewLevel() < 1) {
            throw new BadRequestException("reviewLevel must be >= 1");
        }
        corporateWorkflowStepRepository.findByCorporate_IdAndStepLevel(corporateId, request.getReviewLevel())
                .orElseThrow(() -> new BadRequestException("No workflow step at level " + request.getReviewLevel()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
            throw new BadRequestException("User must belong to this corporate");
        }
        corporateUserReviewLevelRepository.findByCorporate_IdAndUser_IdAndReviewLevel(corporateId, request.getUserId(), request.getReviewLevel())
                .ifPresent(a -> {
                    throw new BadRequestException("That user is already assigned to level " + request.getReviewLevel());
                });
        Corporate corporate = user.getCorporate();
        CorporateUserReviewLevel entity = CorporateUserReviewLevel.builder()
                .corporate(corporate)
                .user(user)
                .reviewLevel(request.getReviewLevel())
                .build();
        entity = corporateUserReviewLevelRepository.save(entity);
        return toCorpAssignmentDto(entity);
    }

    @Override
    @Transactional
    public void removeCorporateAssignment(UUID corporateId, UUID assignmentId) {
        CorporateUserReviewLevel row = corporateUserReviewLevelRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (!row.getCorporate().getId().equals(corporateId)) {
            throw new BadRequestException("Assignment does not belong to this corporate");
        }
        corporateUserReviewLevelRepository.delete(row);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantWorkflowConfigDto getBankConfig(UUID bankId) {
        List<WorkflowStepDto> steps = bankWorkflowStepRepository.findByBank_IdOrderByStepLevelAsc(bankId).stream()
                .map(s -> WorkflowStepDto.builder().id(s.getId()).stepLevel(s.getStepLevel()).label(s.getLabel()).build())
                .toList();
        List<ReviewLevelAssignmentDto> assignments = bankUserReviewLevelRepository.findByBank_IdOrderByReviewLevelAsc(bankId).stream()
                .map(this::toBankAssignmentDto)
                .toList();
        return TenantWorkflowConfigDto.builder().steps(steps).assignments(assignments).build();
    }

    @Override
    @Transactional
    public void updateBankStepLabels(UUID bankId, List<WorkflowStepLabelUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new BadRequestException("At least one step update is required");
        }
        for (WorkflowStepLabelUpdate u : updates) {
            if (u.getLabel() == null || u.getLabel().isBlank()) {
                throw new BadRequestException("Label is required for step " + u.getStepLevel());
            }
            BankWorkflowStep row = bankWorkflowStepRepository
                    .findByBank_IdAndStepLevel(bankId, u.getStepLevel())
                    .orElseThrow(() -> new BadRequestException("Unknown step level for this bank: " + u.getStepLevel()));
            row.setLabel(u.getLabel().trim());
            bankWorkflowStepRepository.save(row);
        }
    }

    @Override
    @Transactional
    public ReviewLevelAssignmentDto addBankAssignment(UUID bankId, CreateReviewAssignmentRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("userId is required");
        }
        if (request.getReviewLevel() < 1) {
            throw new BadRequestException("reviewLevel must be >= 1");
        }
        bankWorkflowStepRepository.findByBank_IdAndStepLevel(bankId, request.getReviewLevel())
                .orElseThrow(() -> new BadRequestException("No workflow step at level " + request.getReviewLevel()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        if (user.getBank() == null || !user.getBank().getId().equals(bankId)) {
            throw new BadRequestException("User must belong to this bank");
        }
        bankUserReviewLevelRepository.findByBank_IdAndUser_IdAndReviewLevel(bankId, request.getUserId(), request.getReviewLevel())
                .ifPresent(a -> {
                    throw new BadRequestException("That user is already assigned to level " + request.getReviewLevel());
                });
        Bank bank = user.getBank();
        BankUserReviewLevel entity = BankUserReviewLevel.builder()
                .bank(bank)
                .user(user)
                .reviewLevel(request.getReviewLevel())
                .build();
        entity = bankUserReviewLevelRepository.save(entity);
        return toBankAssignmentDto(entity);
    }

    @Override
    @Transactional
    public void removeBankAssignment(UUID bankId, UUID assignmentId) {
        BankUserReviewLevel row = bankUserReviewLevelRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (!row.getBank().getId().equals(bankId)) {
            throw new BadRequestException("Assignment does not belong to this bank");
        }
        bankUserReviewLevelRepository.delete(row);
    }

    private ReviewLevelAssignmentDto toCorpAssignmentDto(CorporateUserReviewLevel a) {
        return ReviewLevelAssignmentDto.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .userEmail(a.getUser().getEmail())
                .reviewLevel(a.getReviewLevel())
                .build();
    }

    private ReviewLevelAssignmentDto toBankAssignmentDto(BankUserReviewLevel a) {
        return ReviewLevelAssignmentDto.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .userEmail(a.getUser().getEmail())
                .reviewLevel(a.getReviewLevel())
                .build();
    }
}
