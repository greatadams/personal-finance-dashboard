package com.pm.greatadamu.accountservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;

@Component
@Slf4j
public class TrustedHeaderAuthFilter extends OncePerRequestFilter {
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    private static final String HEADER_EMAIL = "X-User-Email";
    private static final String HEADER_ROLE = "X-User-Role";

    @Override
    protected  void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )throws ServletException, IOException{

        // Read headers forwarded by API Gateway
        String userId = request.getHeader(HEADER_USER_ID);
        String customerId = request.getHeader(HEADER_CUSTOMER_ID);
        String email = request.getHeader(HEADER_EMAIL);
        String role = request.getHeader(HEADER_ROLE);

        //if we have user info rom gateway,set authentication
        if (userId !=null && role != null) {
            log.debug("Setting authentication for userId: {}, customerId: {}, role: {}",
                    userId, customerId, role);


            // Create a simple principal object with user info
            UserPrincipal principal = new UserPrincipal(
                    Long.parseLong(userId),
                    customerId!=null? Long.parseLong(customerId):null,
                    email,
                    role
            );

            // Create authorities (roles) for Spring Security
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(role)
            );

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // Set in Spring Security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authentication set successfully for user: {}", userId);
        } else {
            log.debug("No user headers found in request");
        }
        // Continue the filter chain
        filterChain.doFilter(request, response);

        }
    // Simple class to hold user information
    public record UserPrincipal(
            Long userId,
            Long customerId,
            String email,
            String role
    ) {}
    }

//What This Filter Does:
//
//Reads headers that Gateway added:
//
//X-User-Id
//X-Customer-Id
//X-User-Email
//X-User-Role
//
//
//Creates UserPrincipal - holds all user info
//Sets Spring Security authentication - tells Spring "this user is authenticated"
//Logs for debugging - you'll see what's happening in console