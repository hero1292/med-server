package org.aleksanyan.medserver.repo;

import org.aleksanyan.medserver.domain.DoctorProfile;
import org.aleksanyan.medserver.domain.PatientProfile;
import org.aleksanyan.medserver.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByUser(User user);
    List<PatientProfile> findAllByDoctor(DoctorProfile doctor);
}
