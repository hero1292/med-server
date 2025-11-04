package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.domain.Role;
import org.aleksanyan.medserver.domain.User;
import org.aleksanyan.medserver.dto.response.PatientShortResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.DoctorProfileRepository;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorRepository;
    private final PatientProfileRepository patientRepository;

    @Transactional(readOnly = true)
    public List<PatientShortResponse> getMyPatients(String doctorEmail) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (doctorUser.getRole() != Role.DOCTOR) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        var doctorProfile = doctorRepository.findByUser(doctorUser)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        var patients = patientRepository.findAllByDoctor(doctorProfile);

        return patients.stream()
                .map(p -> PatientShortResponse.builder()
                        .id(p.getId())
                        .fullName(p.getUser().getFullName())
                        .email(p.getUser().getEmail())
                        .birthDate(p.getBirthDate())
                        .phone(p.getPhone())
                        .address(p.getAddress())
                        .build())
                .toList();
    }
}
