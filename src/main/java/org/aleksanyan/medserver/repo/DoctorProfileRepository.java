package org.aleksanyan.medserver.repo;

import org.aleksanyan.medserver.domain.DoctorProfile;
import org.aleksanyan.medserver.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUser(User user);
}
