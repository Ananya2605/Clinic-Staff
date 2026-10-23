package com.clinic.staff;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    static final String EMP = "{\"name\":\"Test Nurse\",\"email\":\"t@clinic.in\",\"department\":\"Nursing\",\"title\":\"Nurse\",\"salary\":30000}";

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "VIEWER")
    void viewerCannotCreateEmployees() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(EMP))
           .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username = "hr", roles = "HR")
    void invalidEmailIsRejected() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(EMP.replace("t@clinic.in", "not-an-email")))
           .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username = "admin", roles = "ADMIN")
    void approvedLeaveSwitchesStatusAndEditsAreAudited() throws Exception {
        String created = mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(EMP))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long empId = om.readTree(created).get("id").asLong();

        String leave = String.format("{\"employeeId\":%d,\"fromDate\":\"%s\",\"toDate\":\"%s\",\"reason\":\"Rest\"}",
                empId, LocalDate.now(), LocalDate.now().plusDays(1));
        String leaveBody = mvc.perform(post("/api/leaves").contentType(MediaType.APPLICATION_JSON).content(leave))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long leaveId = om.readTree(leaveBody).get("id").asLong();

        mvc.perform(get("/api/employees/" + empId)).andExpect(jsonPath("$.effectiveStatus").value("ACTIVE"));
        mvc.perform(post("/api/leaves/" + leaveId + "/approve")).andExpect(status().isOk());
        mvc.perform(get("/api/employees/" + empId)).andExpect(jsonPath("$.effectiveStatus").value("ON_LEAVE"));

        mvc.perform(put("/api/employees/" + empId).contentType(MediaType.APPLICATION_JSON)
                .content(EMP.replace("30000", "35000"))).andExpect(status().isOk());
        mvc.perform(get("/api/audit")).andExpect(status().isOk())
           .andExpect(content().string(containsString("salary: 30000")));
    }

    @Test @WithMockUser(roles = "HR")
    void hrCannotApproveLeave() throws Exception {
        mvc.perform(post("/api/leaves/1/approve")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username = "hr", roles = "HR")
    void meReturnsRole() throws Exception {
        mvc.perform(get("/api/me")).andExpect(jsonPath("$.role").value("HR"));
    }
}
