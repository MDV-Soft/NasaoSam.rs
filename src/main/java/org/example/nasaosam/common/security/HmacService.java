package org.example.nasaosam.common.security;
/*
* ZNACI ZASTITA PODATAKA IDE U DVA SLOJA :
* FRONTEND PRETVARA U sha-256 HASH, NIKAD NE NAPUSTA CLIENT SIDE
* SERVER PREKO HESHA PRIMENJUJE HMAC SA TAJNIM KLJUCEM I SAMO REZULTAT SE CUVA U BAZI
*
* cuvaju se ime prezime kao zasebna var
* cuvaju se ime prezime datum rodjenja kao druga zasebna var
* nema jmbg
* */


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
@Service
public class HmacService {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secretKeyBytes;

    public HmacService(@Value("${app.hmac-secret}") String hmacSecret) {
        byte[] keyBytes;

        keyBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.hmac-secret mora imati najmanje 32 karaktera");
        }
        this.secretKeyBytes = keyBytes;
    }

    //vraca HMAC kao heksadecimalni string od 64 znaka
    public String hashClientHash(String clientSha256Hex) {
        Mac mac;
        byte[] resultBytes;

        if (!isValidSha256Hex(clientSha256Hex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neispravan heš imena");
        }

        // Mac nije bezbedan za vise niti odjednom, zato se pravi nov za svaki poziv
        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, ALGORITHM));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC nije moguće inicijalizovati", exception);
        }

        resultBytes = mac.doFinal(clientSha256Hex.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(resultBytes);
    }

    // 64 znaka, samo cifre i mala slova a-f
    private boolean isValidSha256Hex(String value) {
        if (value == null) {
            return false;
        }
        return value.matches("[0-9a-f]{64}");
    }
}
