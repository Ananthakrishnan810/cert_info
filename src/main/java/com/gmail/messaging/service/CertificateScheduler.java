package com.gmail.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CertificateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CertificateScheduler.class);

    private final ClusterService clusterService;
    private final GmailService gmailService;

    @Autowired
    public CertificateScheduler(ClusterService clusterService, GmailService gmailService) {
        this.clusterService = clusterService;
        this.gmailService = gmailService;
    }

    // Daily at 08:00 AM (Cron: "0 0 8 * * ?")
    @Scheduled(cron = "0 0 8 * * ?")
    public void runDailyExpirationCheckCron() {
        logger.info("⏰ [DAILY SCHEDULER (CRON 8:00 AM)] Executing daily certificate expiration check...");
        int triggered = clusterService.checkAndTriggerExpirationAlerts(gmailService, false);
        logger.info("⏰ [DAILY SCHEDULER] Daily check complete. Dispatched {} expiration alert(s).", triggered);
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 10000)
    public void runDailyExpirationCheckFixedRate() {
        logger.info("⏰ [DAILY SCHEDULER (FIXED RATE 24H)] Executing daily certificate expiration check...");
        int triggered = clusterService.checkAndTriggerExpirationAlerts(gmailService, false);
        logger.info("⏰ [DAILY SCHEDULER] Fixed rate check complete. Dispatched {} expiration alert(s).", triggered);
    }
}
