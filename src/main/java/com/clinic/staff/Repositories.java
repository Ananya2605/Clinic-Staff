package com.clinic.staff;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query("select e.department, avg(e.salary) from Employee e group by e.department")
    List<Object[]> avgSalaryByDepartment();
}

interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
    boolean existsByEmployeeIdAndStateAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long employeeId, LeaveState state, LocalDate start, LocalDate end);
    long countByState(LeaveState state);

    default boolean onLeaveToday(Long employeeId) {
        LocalDate t = LocalDate.now();
        return existsByEmployeeIdAndStateAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                employeeId, LeaveState.APPROVED, t, t);
    }
}

interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}

interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
