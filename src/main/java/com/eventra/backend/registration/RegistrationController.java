package com.eventra.backend.registration;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/events")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/{eventId}/registrations")
    public RegistrationResponse register(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {

        Registration registration =
                registrationService.register(eventId, jwt.getSubject());

        return new RegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getUser().getId(),
                registration.getRegisteredAt()
        );
    }

    public record RegistrationResponse(
            Long id,
            Long eventId,
            Long userId,
            LocalDateTime registeredAt
    ) {
    }
}