package org.example.nasaosam.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Privremena zamena za slanje mejla: ispisuje link u konzolu
 *
 * SAMO ZA RAZVOJ. Link sadrži tajni token
 **/
@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendFinderAccessLink(String toEmail, String link) {
        logger.info("[MEJL - samo za razvoj] Link za nalazača: {}", link);
    }
}