package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.service.AuthenticatedUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * With the identity check on (rams.login.enforce=true), the applicant named in the body of Submit Job Application
 * must be the signed-in applicant. The body is checked right after it is read, so ApplicationController is unchanged.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ApplicationController.class)
@ConditionalOnProperty(name = "rams.login.enforce", havingValue = "true")
public class SubmittedApplicantCheck extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return targetType == SubmitApplicationRequest.class;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        Long applicantId = ((SubmitApplicationRequest) body).applicantId();
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        AuthenticatedUser user = CurrentUser.of(request).orElseThrow(NotSignedInException::new);
        if (user.role() != Role.APPLICANT || !user.id().equals(applicantId)) {
            throw new NotTheSignedInApplicantException();
        }
        return body;
    }

    @ExceptionHandler(NotSignedInException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError notSignedIn(NotSignedInException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(NotTheSignedInApplicantException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError notTheSignedInApplicant(NotTheSignedInApplicantException e) {
        return new ApiError(e.getMessage());
    }

    static class NotSignedInException extends RuntimeException {
        NotSignedInException() {
            super("Sign in to continue.");
        }
    }

    static class NotTheSignedInApplicantException extends RuntimeException {
        NotTheSignedInApplicantException() {
            super("This request does not belong to the signed-in user.");
        }
    }
}
