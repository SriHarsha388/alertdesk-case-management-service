package com.alertdesk.case_management.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateCaseWithAuditEntry() throws Exception {
        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "caseId": "CASE-2026-0001",
                                  "linkedAlertIds": ["ALT-99999"],
                                  "customerId": "CUST-0001",
                                  "priority": "HIGH",
                                  "assignedAnalyst": "analyst.one"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.auditLog[0].eventType").value("CASE_OPENED"));
    }

    @Test
    void shouldRejectStageSkipping() throws Exception {
        mockMvc.perform(patch("/api/cases/CASE-2024-0001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "analyst": "j.rahman",
                                  "status": "PENDING_REVIEW"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Cannot move case from OPEN to PENDING_REVIEW"));
    }

    @Test
    void shouldFileSarDecisionFromPendingReview() throws Exception {
        mockMvc.perform(post("/api/cases/CASE-2024-0003/sar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "analyst": "t.bergmann",
                                  "decision": "FILE",
                                  "rationale": "Confirmed suspicious activity."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAR_FILED"))
                .andExpect(jsonPath("$.sarDecision").value("FILE"))
                .andExpect(jsonPath("$.auditLog[length()-1].eventType").value("SAR_FILED"));
    }
}
