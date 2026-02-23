package com.pm.greatadamu.authservice.service;

import com.pm.greatadamu.authservice.dto.CreateAuthResponse;
import com.pm.greatadamu.authservice.dto.CreateUserLoginDTO;
import com.pm.greatadamu.authservice.dto.CreateUserRegistrationRequestDTO;
import com.pm.greatadamu.authservice.exception.AccountLockedException;
import com.pm.greatadamu.authservice.exception.InvalidCredentialsException;
import com.pm.greatadamu.authservice.exception.UserAlreadyExistsException;
import com.pm.greatadamu.authservice.exception.UserDeactivatedException;
import com.pm.greatadamu.authservice.jwtUtil.JwtService;
import com.pm.greatadamu.authservice.jwtUtil.TokenHashUtil;
import com.pm.greatadamu.authservice.kafka.CustomerEvent;
import com.pm.greatadamu.authservice.kafka.UserRegisteredEvent;
import com.pm.greatadamu.authservice.kafka.UserRegisteredEventProducer;
import com.pm.greatadamu.authservice.model.RefreshToken;
import com.pm.greatadamu.authservice.model.Role;
import com.pm.greatadamu.authservice.model.Status;
import com.pm.greatadamu.authservice.model.User;
import com.pm.greatadamu.authservice.repository.RefreshTokenRepository;
import com.pm.greatadamu.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final UserRegisteredEventProducer userRegisteredEventProducer;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Value("${auth.refresh.expiration-seconds}")
    private long refreshExpirationSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void register(CreateUserRegistrationRequestDTO createUserRegistrationRequestDTO) {
        //email must be unique

        if (userRepository.existsByEmailNormalized(createUserRegistrationRequestDTO.getEmail().trim().toLowerCase())) {
            throw new UserAlreadyExistsException( "Email already exists");
        }
        // hash the raw password from DTO
        User user = User.builder()
                .email(createUserRegistrationRequestDTO.getEmail())
                .passwordHash(passwordEncoder.encode(createUserRegistrationRequestDTO.getPassword()))
                .role(Role.ROLE_CUSTOMER)
                .status(Status.ACTIVE)
                .failedLoginAttempt(0)
                .customerId(null)
                .build();

        // Save user first to get ID
        user = userRepository.save(user);

      // Use userId as customerId (temporary)
        user.setCustomerId(user.getId());
        userRepository.save(user);

        // Publish event to Kafka for Customer Service
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .email(createUserRegistrationRequestDTO.getEmail())
                .firstName(createUserRegistrationRequestDTO.getFirstName())
                .lastName(createUserRegistrationRequestDTO.getLastName())
                .phoneNumber(createUserRegistrationRequestDTO.getPhoneNumber())
                .address(createUserRegistrationRequestDTO.getAddress())
                .gender(createUserRegistrationRequestDTO.getGender())       // Get from DTO
                .build();

        userRegisteredEventProducer.sendUserRegisteredEvent(event);
        log.info("UserRegisteredEvent published for email: {}", createUserRegistrationRequestDTO.getEmail());
    }

    @Transactional
    public CreateAuthResponse login(CreateUserLoginDTO createUserLoginDTO)  {
       return loginWithRefresh(createUserLoginDTO).response();
    }


    @Transactional
    public void updateCustomerIdFromEvent(CustomerEvent customerEvent) {
        log.info("Processing CustomerEvent for email: {}, customerId: {}",
                customerEvent.getEmail().trim().toLowerCase(),customerEvent.getCustomerId());

        //find by email
        Optional<User> userOptional = userRepository.findByEmailNormalized(customerEvent.getEmail().trim().toLowerCase());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            //Update customerID from customer service
            user.setCustomerId(customerEvent.getCustomerId());
            userRepository.save(user);

            log.info("Updated customerId: {} for user with email: {}", customerEvent.getCustomerId(), customerEvent.getEmail());


        }else {
            //user doesn't exist yet-they haven't registered
            // This is normal! Customer profile created before auth registration
            log.info("No user found with email: {}. User hasn't registered yet.",
                    customerEvent.getEmail());
        }

    }

    /// REFRESH TOKEN

    //helper to generate secure random token string
    private String generateRefreshToken(){
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    @Transactional
    public LoginResult loginWithRefresh(CreateUserLoginDTO dto){
        User user = userRepository.findByEmailNormalized(dto.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        //status check
        if(user.getStatus()== Status.DELETED) throw new InvalidCredentialsException("Invalid email or password");
        if (user.getStatus()==Status.PENDING_VERIFICATION) throw new UserDeactivatedException("Account not verified");
        if (user.getStatus()== Status.DISABLED) throw new UserDeactivatedException("User is already disabled");


        if (user.getStatus()== Status.LOCKED) {
            if (user.getLockedUntil() !=null && user.getLockedUntil().isAfter(Instant.now())){
                throw new AccountLockedException("Account is temporary Locked");
            }

            //auto unlock
            user.setStatus(Status.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginAttempt(0);
            userRepository.save(user);
        }

        boolean ok = passwordEncoder.matches(dto.getPassword(),user.getPasswordHash());
        if (!ok) {
            int attempts =user.getFailedLoginAttempt()+1;
            user.setFailedLoginAttempt(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(Status.LOCKED);
                user.setLockedUntil(Instant.now().plusSeconds(600) );
                userRepository.save(user);
                throw new AccountLockedException("Account is temporary locked");
            }
             userRepository.save(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        user.setFailedLoginAttempt(0);
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);

         refreshTokenRepository.revokeAllActiveByUserId(user.getId(), Instant.now());

        //create refresh token(raw + hashed store)
        String rawRefreshToken = generateRefreshToken();
        String refreshHash = TokenHashUtil.hashRefreshToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshHash)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(refreshExpirationSeconds))
                .revokedAt(null)
                .build();

        refreshTokenRepository.save(refreshToken);

        CreateAuthResponse response = CreateAuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpirationMs())
                .build();

        return new LoginResult(response, rawRefreshToken);
    }

    //Refresh endpoint (rotate refresh token)
    @Transactional
    public LoginResult refreshAccessToken(String rawRefreshToken){
        if (rawRefreshToken == null || rawRefreshToken.isBlank()){
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        String refreshHash=TokenHashUtil.hashRefreshToken(rawRefreshToken);

        RefreshToken tokenRow = refreshTokenRepository.findByTokenHash(refreshHash)
                .orElseThrow(() -> new  InvalidCredentialsException("Invalid refresh token"));

        // reuse detection (possible token theft)
        if (tokenRow.getRevokedAt() != null) {
            // revoke all refresh tokens for that user to force full re-login everywhere
            refreshTokenRepository.revokeAllActiveByUserId(tokenRow.getUser().getId(), Instant.now());
            throw new InvalidCredentialsException("Invalid refresh token");
        }


        if (tokenRow.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Refresh token expired");
        }

        User user = tokenRow.getUser();

        //Enforce DB status here
       // allow auto-unlock here too (same behavior as login)
        if (user.getStatus() == Status.LOCKED) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                throw new AccountLockedException("Account is temporary Locked");
            }
            user.setStatus(Status.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginAttempt(0);
            userRepository.save(user);
        }

        //block the real “hard blocks”
        if (user.getStatus() == Status.DELETED) throw new InvalidCredentialsException("Invalid refresh token");
        if (user.getStatus() == Status.PENDING_VERIFICATION) throw new UserDeactivatedException("Account not verified");
        if (user.getStatus() == Status.DISABLED) throw new UserDeactivatedException("User is disabled");

        //revoke old refresh token and mint new one
        tokenRow.setRevokedAt(Instant.now());
        refreshTokenRepository.save(tokenRow);

        String newRawRefreshToken = generateRefreshToken();
        String newHash = TokenHashUtil.hashRefreshToken(newRawRefreshToken);

        RefreshToken newRow = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(refreshExpirationSeconds))
                .revokedAt(null)
                .build();
        refreshTokenRepository.save(newRow);

        String newAccessToken = jwtService.generateToken(user);

        CreateAuthResponse response = CreateAuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpirationMs())
                .build();

        return new LoginResult(response, newRawRefreshToken);


    }
   //logout revokes refresh token
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;

        String refreshHash = TokenHashUtil.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(refreshHash).ifPresent(row -> {
            row.setRevokedAt(Instant.now());
            refreshTokenRepository.save(row);
        });
    }
//small result wrapper (response + refresh token for cookie)
    public record LoginResult(CreateAuthResponse response, String refreshToken) {}
}
