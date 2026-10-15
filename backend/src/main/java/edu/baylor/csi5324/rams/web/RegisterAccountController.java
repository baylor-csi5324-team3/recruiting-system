package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/register")
public class RegisterAccountController {
    private final RegistrationService registrationService;

    public RegisterAccountController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterAccountResponse register(@RequestBody RegisterAccountRequest request) {
        UserAccount account = registrationService.register(request);
        return new RegisterAccountResponse(account.getId(), account.getEmail(), account.getRole(), account.getStatus());
    }
}
