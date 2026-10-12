package edu.baylor.csi5324.rams.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Registration-only errors must never echo credential-bearing request bodies or database details. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RegisterAccountController.class)
public class RegistrationExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError malformed() {
        return new ApiError("Provide valid registration JSON with role APPLICANT or RECRUITER.");
    }

    /** Also covers concurrent email/company insertion after the friendly duplicate precheck. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict() {
        return new ApiError("Registration conflicts with an existing account or company. Check the email and retry.");
    }
}
