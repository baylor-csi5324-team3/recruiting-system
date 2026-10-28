package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import edu.baylor.csi5324.rams.web.IdentityCheckInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Turns on the identity check (rams.login.enforce=true): every user id a request names must belong to the signed-in
 * user (IdentityCheckInterceptor and SubmittedApplicantCheck). It is on in application.properties and off in the
 * tests (src/test/resources/config/application.properties), so the existing tests run without signing in.
 */
@Configuration
@ConditionalOnProperty(name = "rams.login.enforce", havingValue = "true")
public class LoginGateConfig implements WebMvcConfigurer {
    private final UserAccountRepository accounts;

    public LoginGateConfig(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new IdentityCheckInterceptor(accounts)).addPathPatterns("/api/**");
    }
}
