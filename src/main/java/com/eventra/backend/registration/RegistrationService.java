package com.eventra.backend.registration;

import com.eventra.backend.event.Event;
import com.eventra.backend.event.EventRepository;
import com.eventra.backend.user.User;
import com.eventra.backend.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;

    public RegistrationService(
            EventRepository eventRepository,
            UserRepository userRepository,
            RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public Registration register(Long eventId, String userEmail) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (registrationRepository.existsByUser_IdAndEvent_Id(user.getId(), eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User is already registered");
        }

        if (registrationRepository.countByEvent_Id(eventId) >= event.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Event is full");
        }

        Registration registration = new Registration();
        registration.setUser(user);
        registration.setEvent(event);

        return registrationRepository.save(registration);
    }
}