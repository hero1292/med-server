package org.aleksanyan.medserver.repo;

import org.aleksanyan.medserver.domain.DoctorProfile;
import org.aleksanyan.medserver.domain.PatientProfile;
import org.aleksanyan.medserver.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findAllByDoctor(DoctorProfile doctor);
    List<Recommendation> findAllByDoctorAndPatient(DoctorProfile doctor, PatientProfile patient);
    Optional<Recommendation> findByIdAndDoctor(Long id, DoctorProfile doctor);
}
