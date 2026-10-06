package com.eventra.backend.registration;

import com.eventra.backend.event.Event;
import com.eventra.backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registrations",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "event_id"}
        )
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private LocalDateTime registeredAt;

    public Registration() {
    }

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}