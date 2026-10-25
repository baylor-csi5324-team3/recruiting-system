package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.Role;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/** JSON contracts only: no registration controller is installed until shared changes are approved. */
class RegisterAccountPayloadTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void readsRegistrationInputAndDoesNotSerializePassword() {
        RegisterAccountRequest request = mapper.readValue("""
                {"email":" Person@Example.com ","password":"private phrase","role":"RECRUITER",
                 "companyName":"Baylor Tech","fullName":"Rhea"}
                """, RegisterAccountRequest.class);
        assertEquals("person@example.com", request.email());
        assertEquals("private phrase", request.password());
        assertEquals(Role.RECRUITER, request.role());
        String json = mapper.writeValueAsString(request);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("private phrase"));
    }

    @Test
    void successPayloadContainsOnlySafeAccountFields() {
        var json = mapper.readTree(mapper.writeValueAsString(
                new RegisterAccountResponse(7L, "person@example.com", Role.APPLICANT, AccountStatus.ACTIVE)));
        assertEquals(4, json.size());
        assertEquals(7, json.get("accountId").asInt());
        assertEquals("person@example.com", json.get("email").asString());
        assertEquals("APPLICANT", json.get("role").asString());
        assertEquals("ACTIVE", json.get("status").asString());
    }
}
