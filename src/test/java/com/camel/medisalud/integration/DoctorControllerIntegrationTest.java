package com.camel.medisalud.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camel.medisalud.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class DoctorControllerIntegrationTest {

    private static final String BASE = "/api/v1/doctors";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String doctorJson(String fullName, String specialty, String phone, String email) {
        return """
                {"fullName":%s,"specialty":%s,"phone":%s,"email":%s}
                """.formatted(quote(fullName), quote(specialty), quote(phone), quote(email));
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID createValidDoctor() throws Exception {
        String body = doctorJson("Dr. Gregory House", "Diagnostics", "3001234567",
                "house+" + UUID.randomUUID() + "@clinic.com");
        String json = create(body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    @Test
    void create_returnsCreatedWithLocationAndBody() throws Exception {
        String body = doctorJson("Dr. Gregory House", "Diagnostics", "3001234567", "house@clinic.com");

        create(body)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Dr. Gregory House"))
                .andExpect(jsonPath("$.specialty").value("Diagnostics"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void create_withOptionalFieldsOmitted_isAllowed() throws Exception {
        create(doctorJson("Dr. James Wilson", "Oncology", null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Dr. James Wilson"));
    }

    @Test
    void getById_returnsDoctor() throws Exception {
        UUID id = createValidDoctor();

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void list_returnsCreatedDoctor() throws Exception {
        createValidDoctor();

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void update_modifiesDoctor() throws Exception {
        UUID id = createValidDoctor();
        String body = doctorJson("Dr. Gregory House", "Nephrology", "3009998888", "house@clinic.com");

        mockMvc.perform(put(BASE + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty").value("Nephrology"));
    }

    @Test
    void delete_removesDoctor() throws Exception {
        UUID id = createValidDoctor();

        mockMvc.perform(delete(BASE + "/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_whenMissing_returnsNotFoundWithErrorBody() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").isNotEmpty());
    }

    @Test
    void update_whenMissing_returnsNotFound() throws Exception {
        String body = doctorJson("Dr. Gregory House", "Diagnostics", "3001234567", "house@clinic.com");

        mockMvc.perform(put(BASE + "/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withShortFullName_returnsBadRequest() throws Exception {
        create(doctorJson("Dr", "Diagnostics", "3001234567", "house@clinic.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_withMissingSpecialty_returnsBadRequest() throws Exception {
        create(doctorJson("Dr. Gregory House", null, "3001234567", "house@clinic.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withShortPhone_returnsBadRequest() throws Exception {
        create(doctorJson("Dr. Gregory House", "Diagnostics", "12345", "house@clinic.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withInvalidEmail_returnsBadRequest() throws Exception {
        create(doctorJson("Dr. Gregory House", "Diagnostics", "3001234567", "not-an-email"))
                .andExpect(status().isBadRequest());
    }
}
