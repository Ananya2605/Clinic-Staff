package com.clinic.staff;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/leaves")
class LeaveController {
    private final LeaveRepository leaves;
    private final EmployeeRepository employees;
    private final AuditService audit;

    LeaveController(LeaveRepository leaves, EmployeeRepository employees, AuditService audit) {
        this.leaves = leaves; this.employees = employees; this.audit = audit;
    }

    @GetMapping
    List<LeaveRequest> all() { return leaves.findAll(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    LeaveRequest request(@Valid @RequestBody LeaveRequest in, Authentication a) {
        if (in.toDate.isBefore(in.fromDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toDate must not be before fromDate");
        Employee e = employees.findById(in.employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        in.id = null; in.state = LeaveState.PENDING;
        LeaveRequest saved = leaves.save(in);
        audit.log(a.getName(), "Leave requested for " + e.name + " (" + in.fromDate + " to " + in.toDate + ")");
        return saved;
    }

    @PostMapping("/{id}/approve")
    LeaveRequest approve(@PathVariable Long id, Authentication a) { return decide(id, LeaveState.APPROVED, a); }

    @PostMapping("/{id}/reject")
    LeaveRequest reject(@PathVariable Long id, Authentication a) { return decide(id, LeaveState.REJECTED, a); }

    private LeaveRequest decide(Long id, LeaveState state, Authentication a) {
        LeaveRequest l = leaves.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
        if (l.state != LeaveState.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request was already " + l.state);
        l.state = state;
        String name = employees.findById(l.employeeId).map(e -> e.name).orElse("unknown");
        audit.log(a.getName(), "Leave " + state + " for " + name + " (" + l.fromDate + " to " + l.toDate + ")");
        return leaves.save(l);
    }
}
