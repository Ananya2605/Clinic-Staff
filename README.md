# Clinic Staff API

Spring Boot 3 + MySQL back end for a small clinic's staff management: roles, leave approval
that drives employee status, an audit trail, salary analytics and a self-referencing org chart (`managerId`).

## Run
1. Install Java 17+, Maven and MySQL. The database `clinic_staff` is created automatically.
2. Set credentials if yours differ: `DB_URL`, `DB_USER`, `DB_PASSWORD` (defaults: root / empty password).
3. `mvn spring-boot:run`
4. Open http://localhost:8080 for the web app, or http://localhost:8080/swagger-ui.html for the API docs (click Authorize and use HTTP Basic).

Demo users, created on first start (change before deploying): `admin/admin123`, `hr/hr123`, `viewer/viewer123`.

## Roles
| Action | Admin | HR | Viewer |
|---|---|---|---|
| View staff (salary hidden for Viewer) | yes | yes | yes |
| Add / edit staff, request leave | yes | yes | no |
| Approve / reject leave, delete staff | yes | no | no |
| Dashboard and audit trail | yes | yes | no |

## Endpoints
- `GET/POST /api/employees`, `GET/PUT/DELETE /api/employees/{id}`. Each employee has a calculated `effectiveStatus` (`ON_LEAVE` while approved leave covers today).
- `GET/POST /api/leaves`, `POST /api/leaves/{id}/approve|reject`
- `GET /api/dashboard` (headcount, leave counts, average salary by department, salary outliers)
- `GET /api/audit` (who changed what, old → new)

Example: `curl -u admin:admin123 localhost:8080/api/dashboard`

## Tests and CI
`mvn test` runs MockMvc tests against in-memory H2. `.github/workflows/ci.yml` runs them on every push.
To add the Maven wrapper so CI needs no installed Maven: `mvn wrapper:wrapper`.

## Front end
Plain HTML, CSS and JavaScript in `src/main/resources/static` (`index.html`, `style.css`, `app.js`), served by Spring Boot on the same origin, so no CORS setup or build step is needed. It has sign-in, dashboard, directory with add/edit/delete, org chart, leave requests and approval, audit trail, dark mode and print styles. Buttons and tabs appear according to the signed-in role, but the server enforces every rule. The login is kept in `sessionStorage` for the browser tab.

## Next steps
Switch from HTTP Basic to JWT or sessions, add attendance and document upload.
