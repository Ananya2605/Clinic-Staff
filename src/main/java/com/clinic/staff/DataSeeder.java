package com.clinic.staff;

import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates demo users and staff on first start. Change these passwords before deploying. */
@Component
@Profile("!test")
class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;

    DataSeeder(AppUserRepository users, EmployeeRepository employees, PasswordEncoder encoder) {
        this.users = users; this.employees = employees; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            addUser("admin", "admin123", "ADMIN");
            addUser("hr", "hr123", "HR");
            addUser("viewer", "viewer123", "VIEWER");
        }
        if (employees.count() == 0) {
            Employee director = emp("Dr. Meera Kulkarni", "meera@clinic.in", "Management", "Medical Director", 240000, null);
            Employee head = emp("Rohan Patil", "rohan@clinic.in", "Nursing", "Head Nurse", 62000, director.id);
            emp("Anita Deshmukh", "anita@clinic.in", "Nursing", "Staff Nurse", 38000, head.id);
            emp("Priya Nair", "priya@clinic.in", "Front Desk", "Receptionist", 26000, director.id);
            emp("Vikram Shah", "vikram@clinic.in", "Diagnostics", "Lab Technician", 41000, director.id);
        }
    }

    private void addUser(String name, String password, String role) {
        AppUser u = new AppUser();
        u.username = name; u.passwordHash = encoder.encode(password); u.role = role;
        users.save(u);
    }

    private Employee emp(String name, String email, String dept, String title, int salary, Long managerId) {
        Employee e = new Employee();
        e.name = name; e.email = email; e.department = dept; e.title = title;
        e.salary = BigDecimal.valueOf(salary); e.managerId = managerId;
        return employees.save(e);
    }
}
