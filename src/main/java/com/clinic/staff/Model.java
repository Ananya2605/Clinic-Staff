package com.clinic.staff;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

enum EmployeeStatus { ACTIVE, INACTIVE }
enum LeaveState { PENDING, APPROVED, REJECTED }

@Entity
class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String name;
    @NotBlank @Email public String email;
    @NotBlank public String department;
    @NotBlank public String title;
    public BigDecimal salary;
    /** Self-reference: id of this employee's manager, null for the top of the org chart. */
    public Long managerId;
    @Enumerated(EnumType.STRING) public EmployeeStatus status = EmployeeStatus.ACTIVE;
    /** Calculated: ON_LEAVE while an approved leave covers today, otherwise the stored status. */
    @Transient public String effectiveStatus;

    Employee copy() {
        Employee c = new Employee();
        c.id = id; c.name = name; c.email = email; c.department = department; c.title = title;
        c.salary = salary; c.managerId = managerId; c.status = status;
        return c;
    }
}

@Entity
class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotNull public Long employeeId;
    @NotNull public LocalDate fromDate;
    @NotNull public LocalDate toDate;
    public String reason;
    @Enumerated(EnumType.STRING) public LeaveState state = LeaveState.PENDING;
}

@Entity
class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Instant at;
    public String actor;
    @Column(length = 500) public String message;
}

@Entity
class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String username;
    public String passwordHash;
    public String role; // ADMIN, HR or VIEWER
}
