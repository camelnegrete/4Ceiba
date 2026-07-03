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
class PatientControllerIntegrationTest {

    private static final String BASE = "/api/v1/patients";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String patientJson(String fullName, String document, String phone,
                               String email, String birthDate) {
        return """
                {"fullName":%s,"document":%s,"phone":%s,"email":%s,"birthDate":%s}
                """.formatted(quote(fullName), quote(document), quote(phone),
                quote(email), quote(birthDate));
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private String uniqueEmail() {
        return "john+" + UUID.randomUUID() + "@mail.com";
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID createValidPatient() throws Exception {
        String body = patientJson("John Doe", UUID.randomUUID().toString(),
                "3001234567", uniqueEmail(), "1990-05-20");
        String json = create(body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    @Test
    void create_returnsCreatedWithLocationAndBody() throws Exception {
        String body = patientJson("John Doe", "1010101010", "3001234567",
                uniqueEmail(), "1990-05-20");

        create(body)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.document").value("1010101010"));
    }

    @Test
    void create_withoutBirthDate_isAllowed() throws Exception {
        String body = patientJson("Jane Roe", UUID.randomUUID().toString(),
                "3009998888", uniqueEmail(), null);

        create(body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Jane Roe"));
    }

    @Test
    void getById_returnsPatient() throws Exception {
        UUID id = createValidPatient();

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void list_returnsCreatedPatient() throws Exception {
        createValidPatient();

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void update_modifiesPatient() throws Exception {
        UUID id = createValidPatient();
        String body = patientJson("John Q. Doe", UUID.randomUUID().toString(),
                "3005556666", uniqueEmail(), "1985-01-01");

        mockMvc.perform(put(BASE + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Q. Doe"));
    }

    @Test
    void delete_removesPatient() throws Exception {
        UUID id = createValidPatient();

        mockMvc.perform(delete(BASE + "/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withDuplicateDocument_returnsConflict() throws Exception {
        String document = UUID.randomUUID().toString();
        create(patientJson("John Doe", document, "3001234567", uniqueEmail(), "1990-05-20"))
                .andExpect(status().isCreated());

        create(patientJson("Another Name", document, "3001112222", uniqueEmail(), "1991-06-21"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void getById_whenMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_withShortFullName_returnsBadRequest() throws Exception {
        create(patientJson("Jo", UUID.randomUUID().toString(), "3001234567",
                uniqueEmail(), "1990-05-20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingDocument_returnsBadRequest() throws Exception {
        create(patientJson("John Doe", null, "3001234567", uniqueEmail(), "1990-05-20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingPhone_returnsBadRequest() throws Exception {
        create(patientJson("John Doe", UUID.randomUUID().toString(), null,
                uniqueEmail(), "1990-05-20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withInvalidEmail_returnsBadRequest() throws Exception {
        create(patientJson("John Doe", UUID.randomUUID().toString(), "3001234567",
                "not-an-email", "1990-05-20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withFutureBirthDate_returnsBadRequest() throws Exception {
        create(patientJson("John Doe", UUID.randomUUID().toString(), "3001234567",
                uniqueEmail(), "2999-01-01"))
                .andExpect(status().isBadRequest());
    }
}
