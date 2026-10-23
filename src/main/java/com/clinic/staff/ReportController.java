package com.clinic.staff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class ReportController {
    private final EmployeeRepository employees;
    private final LeaveRepository leaves;
    private final AuditLogRepository auditLogs;

    ReportController(EmployeeRepository employees, LeaveRepository leaves, AuditLogRepository auditLogs) {
        this.employees = employees; this.leaves = leaves; this.auditLogs = auditLogs;
    }

    @GetMapping("/me")
    Map<String, String> me(Authentication a) {
        String role = a.getAuthorities().stream().map(g -> g.getAuthority().replace("ROLE_", "")).findFirst().orElse("VIEWER");
        return Map.of("username", a.getName(), "role", role);
    }

    @GetMapping("/audit")
    List<AuditLog> audit() { return auditLogs.findAll(Sort.by(Sort.Direction.DESC, "at")); }

    @GetMapping("/dashboard")
    Map<String, Object> dashboard() {
        List<Employee> all = employees.findAll();
        long onLeave = all.stream().filter(e -> e.status == EmployeeStatus.ACTIVE && leaves.onLeaveToday(e.id)).count();
        long active = all.stream().filter(e -> e.status == EmployeeStatus.ACTIVE).count() - onLeave;

        Map<String, BigDecimal> avgByDept = new LinkedHashMap<>();
        for (Object[] row : employees.avgSalaryByDepartment())
            avgByDept.put((String) row[0], BigDecimal.valueOf(((Number) row[1]).doubleValue()).setScale(0, RoundingMode.HALF_UP));

        BigDecimal total = all.stream().map(e -> e.salary == null ? BigDecimal.ZERO : e.salary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = all.isEmpty() ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(all.size()), 0, RoundingMode.HALF_UP);
        List<String> outliers = all.stream()
                .filter(e -> e.salary != null && e.salary.compareTo(avg.multiply(BigDecimal.valueOf(2))) > 0)
                .map(e -> e.name).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalStaff", all.size());
        out.put("activeToday", active);
        out.put("onLeaveToday", onLeave);
        out.put("pendingLeaveRequests", leaves.countByState(LeaveState.PENDING));
        out.put("averageSalaryByDepartment", avgByDept);
        out.put("salaryOutliers", outliers);
        return out;
    }
}
