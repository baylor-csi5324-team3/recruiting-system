package edu.baylor.csi5324.rams.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smallest possible REST endpoint. It exists so that the build, the web layer
 * and the test harness can be verified end to end before any domain code is
 * written; the recruiting endpoints replace it as the use cases are implemented.
 */
@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private final String applicationName;

    SystemInfoController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/info")
    public SystemInfo info() {
        return new SystemInfo(applicationName, "UP");
    }
}
