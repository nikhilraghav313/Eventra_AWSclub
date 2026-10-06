package com.eventra.backend.registration;

import com.eventra.backend.event.Event;
import com.eventra.backend.event.EventRepository;
import com.eventra.backend.user.User;
import com.eventra.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final String MEMBER_EMAIL = "member@example.com";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registersMemberWhenSeatIsAvailable() {
        Event event = eventWithCapacity(2);
        User user = member();

        when(eventRepository.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(userRepository.findByEmail(MEMBER_EMAIL))
                .thenReturn(Optional.of(user));
        when(registrationRepository.existsByUser_IdAndEvent_Id(user.getId(), EVENT_ID))
                .thenReturn(false);
        when(registrationRepository.countByEvent_Id(EVENT_ID)).thenReturn(0L);
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Registration result = registrationService.register(EVENT_ID, MEMBER_EMAIL);

        assertSame(user, result.getUser());
        assertSame(event, result.getEvent());
        verify(eventRepository).findByIdForUpdate(EVENT_ID);
    }

    @Test
    void rejectsDuplicateRegistration() {
        Event event = eventWithCapacity(2);
        User user = member();

        when(eventRepository.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(userRepository.findByEmail(MEMBER_EMAIL))
                .thenReturn(Optional.of(user));
        when(registrationRepository.existsByUser_IdAndEvent_Id(user.getId(), EVENT_ID))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(EVENT_ID, MEMBER_EMAIL)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void rejectsRegistrationWhenEventIsFull() {
        Event event = eventWithCapacity(1);
        User user = member();

        when(eventRepository.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(userRepository.findByEmail(MEMBER_EMAIL))
                .thenReturn(Optional.of(user));
        when(registrationRepository.existsByUser_IdAndEvent_Id(user.getId(), EVENT_ID))
                .thenReturn(false);
        when(registrationRepository.countByEvent_Id(EVENT_ID)).thenReturn(1L);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(EVENT_ID, MEMBER_EMAIL)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void rejectsRegistrationForMissingEvent() {
        when(eventRepository.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(EVENT_ID, MEMBER_EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(registrationRepository, never()).save(any());
    }

    private Event eventWithCapacity(int capacity) {
        Event event = new Event();
        event.setCapacity(capacity);
        return event;
    }

    private User member() {
        User user = new User();
        user.setName("Test Member");
        user.setEmail(MEMBER_EMAIL);
        return user;
    }
}