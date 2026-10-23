package com.clinic.staff;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
class EmployeeController {
    private final EmployeeRepository repo;
    private final LeaveRepository leaves;
    private final AuditService audit;

    EmployeeController(EmployeeRepository repo, LeaveRepository leaves, AuditService audit) {
        this.repo = repo; this.leaves = leaves; this.audit = audit;
    }

    @GetMapping
    List<Employee> all(Authentication a) { return repo.findAll().stream().map(e -> view(e, a)).toList(); }

    @GetMapping("/{id}")
    Employee one(@PathVariable Long id, Authentication a) { return view(find(id), a); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Employee create(@Valid @RequestBody Employee in, Authentication a) {
        in.id = null;
        Employee saved = repo.save(in);
        audit.log(a.getName(), "Added employee " + saved.name);
        return view(saved, a);
    }

    @PutMapping("/{id}")
    Employee update(@PathVariable Long id, @Valid @RequestBody Employee in, Authentication a) {
        Employee e = find(id);
        String who = a.getName();
        note(who, e, "name", e.name, in.name);
        note(who, e, "email", e.email, in.email);
        note(who, e, "department", e.department, in.department);
        note(who, e, "title", e.title, in.title);
        note(who, e, "salary", e.salary, in.salary);
        note(who, e, "managerId", e.managerId, in.managerId);
        note(who, e, "status", e.status, in.status);
        e.name = in.name; e.email = in.email; e.department = in.department; e.title = in.title;
        e.salary = in.salary; e.managerId = in.managerId; e.status = in.status;
        return view(repo.save(e), a);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id, Authentication a) {
        Employee e = find(id);
        repo.delete(e);
        audit.log(a.getName(), "Removed employee " + e.name);
    }

    private Employee find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    /** Copies the entity so hiding the salary never touches the persisted object. */
    private Employee view(Employee e, Authentication a) {
        Employee c = e.copy();
        boolean privileged = a.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN") || g.getAuthority().equals("ROLE_HR"));
        if (!privileged) c.salary = null;
        c.effectiveStatus = (e.status == EmployeeStatus.ACTIVE && leaves.onLeaveToday(e.id)) ? "ON_LEAVE" : e.status.name();
        return c;
    }

    private void note(String who, Employee e, String field, Object oldV, Object newV) {
        if (!same(oldV, newV)) audit.log(who, e.name + " · " + field + ": " + oldV + " → " + newV);
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }
}
