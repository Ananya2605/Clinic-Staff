package com.clinic.staff;

import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
class AuditService {
    private final AuditLogRepository repo;
    AuditService(AuditLogRepository repo) { this.repo = repo; }

    void log(String actor, String message) {
        AuditLog a = new AuditLog();
        a.at = Instant.now(); a.actor = actor; a.message = message;
        repo.save(a);
    }
}
