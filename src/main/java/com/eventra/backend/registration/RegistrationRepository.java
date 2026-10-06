package com.eventra.backend.registration;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository
        extends JpaRepository<Registration, Long> {

    long countByEvent_Id(Long eventId);

    boolean existsByUser_IdAndEvent_Id(Long userId, Long eventId);
}