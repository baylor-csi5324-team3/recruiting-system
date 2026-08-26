package edu.baylor.csi5324.rams.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer slice test. {@code @WebMvcTest} starts only the MVC infrastructure
 * and the named controller, so it runs in a fraction of the time a full
 * application context would take and fails for web reasons only.
 */
@WebMvcTest(SystemInfoController.class)
class SystemInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/system/info reports the application name and an UP status")
    void reportsApplicationNameAndStatus() throws Exception {
        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("recruiting-system"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
