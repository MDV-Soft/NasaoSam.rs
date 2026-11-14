package org.example.nasaosam.notification.service;

/**
 * Slanje mejlova.
 */
public interface EmailService {

    void sendFinderAccessLink(String toEmail, String link);
}