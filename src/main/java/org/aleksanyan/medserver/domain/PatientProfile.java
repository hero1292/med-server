package org.aleksanyan.medserver.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private LocalDate birthDate;

    @Column(length = 64)
    private String phone;

    @Column(length = 255)
    private String address;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private DoctorProfile doctor;
}
