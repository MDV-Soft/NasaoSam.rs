package org.example.nasaosam.auth.service;

import org.example.nasaosam.common.security.PhoneNumberNormalizer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.example.nasaosam.auth.dto.LoginRequest;
import org.example.nasaosam.auth.dto.RegisterRequest;
import org.example.nasaosam.user.entity.Role;
import org.example.nasaosam.user.entity.User;
import org.example.nasaosam.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.example.nasaosam.auth.dto.AuthResponse;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Pogrešan mejl ili lozinka";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final RevokedTokenService revokedTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, PhoneNumberNormalizer phoneNumberNormalizer, RevokedTokenService revokedTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.revokedTokenService = revokedTokenService;
    }

    private String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest){
        String normalizedEmail;
        String passwordHash;
        User newUser;
        User savedUser;
        String token;
        String normalizedPhone;

        normalizedPhone = phoneNumberNormalizer.normalize(registerRequest.phone());

        normalizedEmail = normalizeEmail(registerRequest.email());

        if(userRepository.existsByEmail(normalizedEmail)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nalog sa ovim mejlov vec postoji");
        }

        passwordHash = passwordEncoder.encode(registerRequest.password());
        newUser = new User(normalizedEmail, passwordHash, registerRequest.displayName(), Role.OWNER);
        newUser.setPhone(normalizedPhone);
        savedUser = userRepository.save(newUser);

        token = jwtService.createToken(savedUser);
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail;
        Optional<User> foundUser;
        User user;
        String token;

        normalizedEmail = normalizeEmail(request.email());
        foundUser = userRepository.findByEmail(normalizedEmail);

        if(foundUser.isEmpty()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        user = foundUser.get();

        if(user.getPasswordHash() == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        token = jwtService.createToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    //LOGOUT
    // Odjava: ID i vreme isteka tokena iz kog je zahtev stigao idu u spisak opozvanih
    public void logout(Jwt jwt) {
        revokedTokenService.revoke(jwt.getId(), jwt.getExpiresAt());
    }
}
