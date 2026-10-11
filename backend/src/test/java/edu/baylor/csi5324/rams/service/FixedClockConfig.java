package edu.baylor.csi5324.rams.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Freezes "now" at 2026-10-12 09:00 so that date rules can be asserted exactly. */
@TestConfiguration
public class FixedClockConfig {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(TODAY.atTime(9, 0).atZone(ZoneId.of("America/Chicago")).toInstant(), ZoneId.of("America/Chicago"));
    }
}
