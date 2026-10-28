package edu.baylor.csi5324.rams.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/** SD 6.3 message 2: the login information; the password is write-only in JSON and redacted in diagnostics. */
public record LoginRequest(String email, @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {

    @Override
    public String toString() {
        return "LoginRequest[password=<redacted>]";
    }
}
