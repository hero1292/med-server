package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aleksanyan.medserver.domain.DoctorProfile;
import org.aleksanyan.medserver.domain.PatientProfile;
import org.aleksanyan.medserver.domain.Role;
import org.aleksanyan.medserver.domain.User;
import org.aleksanyan.medserver.dto.request.UpdateDoctorProfileRequest;
import org.aleksanyan.medserver.dto.request.UpdatePatientProfileRequest;
import org.aleksanyan.medserver.dto.response.DoctorProfileResponse;
import org.aleksanyan.medserver.dto.response.PatientProfileResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.DoctorProfileRepository;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorRepository;
    private final PatientProfileRepository patientRepository;

    @Transactional(readOnly = true)
    public Object getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return switch (user.getRole()) {
            case DOCTOR -> getDoctorProfile(user);
            case PATIENT -> getPatientProfile(user);
            default -> throw new ApiException(ErrorCode.ACCESS_DENIED);
        };
    }

    @Transactional
    public void createProfileForUser(User user) {
        if (user == null || user.getRole() == null) {
            throw new ApiException(ErrorCode.INVALID_ROLE);
        }

        switch (user.getRole()) {
            case DOCTOR -> createForDoctor(user);
            case PATIENT -> createForPatient(user);
            default -> throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Transactional
    public DoctorProfileResponse updateDoctorProfile(String email, UpdateDoctorProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.DOCTOR)
            throw new ApiException(ErrorCode.ACCESS_DENIED);

        var profile = doctorRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getSpecialty() != null)
            profile.setSpecialty(request.getSpecialty());
        if (request.getPhone() != null)
            profile.setPhone(request.getPhone());

        userRepository.save(user);
        doctorRepository.save(profile);

        return DoctorProfileResponse.builder()
                .id(profile.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .specialty(profile.getSpecialty())
                .phone(profile.getPhone())
                .build();
    }

    @Transactional
    public PatientProfileResponse updatePatientProfile(String email, UpdatePatientProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.PATIENT)
            throw new ApiException(ErrorCode.ACCESS_DENIED);

        var profile = patientRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getBirthDate() != null)
            profile.setBirthDate(request.getBirthDate());
        if (request.getPhone() != null)
            profile.setPhone(request.getPhone());
        if (request.getAddress() != null)
            profile.setAddress(request.getAddress());

        if (request.getDoctorId() != null) {
            var doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ApiException(ErrorCode.DOCTOR_NOT_FOUND));
            profile.setDoctor(doctor);
        }

        userRepository.save(user);
        patientRepository.save(profile);

        return PatientProfileResponse.builder()
                .id(profile.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .birthDate(profile.getBirthDate())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .doctorId(profile.getDoctor() != null ? profile.getDoctor().getId() : null)
                .doctorName(profile.getDoctor() != null ? profile.getDoctor().getUser().getFullName() : null)
                .build();
    }


    private DoctorProfileResponse getDoctorProfile(User user) {
        var profile = doctorRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        return DoctorProfileResponse.builder()
                .id(profile.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .specialty(profile.getSpecialty())
                .phone(profile.getPhone())
                .build();
    }

    private PatientProfileResponse getPatientProfile(User user) {
        var profile = patientRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        return PatientProfileResponse.builder()
                .id(profile.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .birthDate(profile.getBirthDate())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .doctorId(profile.getDoctor() != null ? profile.getDoctor().getId() : null)
                .doctorName(profile.getDoctor() != null ? profile.getDoctor().getUser().getFullName() : null)
                .build();
    }

    private void createForDoctor(User user) {
        doctorRepository.findByUser(user).orElseGet(() -> {
            DoctorProfile profile = DoctorProfile.builder()
                    .user(user)
                    .specialty(null)
                    .phone(null)
                    .build();

            return doctorRepository.save(profile);
        });
    }

    public void createForPatient(User user) {
        patientRepository.findByUser(user).orElseGet(() -> {
            PatientProfile profile = PatientProfile.builder()
                    .user(user)
                    .birthDate(null)
                    .phone(null)
                    .address(null)
                    .doctor(null)
                    .build();

            return patientRepository.save(profile);
        });
    }
}
