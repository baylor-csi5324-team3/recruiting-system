package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.service.InactiveAccountException;
import edu.baylor.csi5324.rams.service.InvalidCredentialsException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Login-only errors; like registration, they never echo the credential-bearing request body. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = LoginController.class)
public class LoginExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError invalidCredentials(InvalidCredentialsException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(InactiveAccountException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError inactive(InactiveAccountException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(LoginController.NotSignedInException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError notSignedIn(LoginController.NotSignedInException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError malformed() {
        return new ApiError("Provide your email address and password.");
    }
}
