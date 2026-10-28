package com.gympabon.service;

import java.time.LocalDate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Updates the status of the memberships without anybody pressing a button
 * (RF10, actor "Sistema"): when the application starts and every day at
 * 00:05, a scheduled renewal becomes active and a finished one expires.
 */
@Configuration
@EnableScheduling
public class MembershipStatusScheduler {

    private final MembershipService membershipService;

    public MembershipStatusScheduler(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** Runs once when the application is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        membershipService.refreshStatuses(LocalDate.now());
    }

    /** Runs every day at 00:05. */
    @Scheduled(cron = "0 5 0 * * *")
    public void refreshEveryDay() {
        membershipService.refreshStatuses(LocalDate.now());
    }
}
