package com.pm.greatadamu.accountservice.controller;

import com.pm.greatadamu.accountservice.dto.AccountRequestDto;
import com.pm.greatadamu.accountservice.dto.AccountResponseDto;
import com.pm.greatadamu.accountservice.filter.TrustedHeaderAuthFilter;
import com.pm.greatadamu.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDto> createAccount(
            @AuthenticationPrincipal TrustedHeaderAuthFilter.UserPrincipal userPrincipal,
            @RequestBody @Valid
            AccountRequestDto accountRequestDto)
    {
        //log authenticated user info
        log.info("Creating account fro user: {},customerId:{}",userPrincipal.userId(),userPrincipal.customerId());

        //Use customerId from JWT (authenticated user), not from request body
        accountRequestDto.setCustomerId(userPrincipal.customerId());


        //call the service to get the user input->saved to db->send back to user as json to view
        AccountResponseDto accountResponseDto = accountService.createAccount(accountRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountResponseDto);

    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponseDto> updateAccount(
            @AuthenticationPrincipal TrustedHeaderAuthFilter.UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody @Valid
            AccountRequestDto accountRequestDto){


        log.info("Updating account {} for user: {}", id, userPrincipal.userId());

        // Ensure user can only update their own accounts
        accountRequestDto.setCustomerId(userPrincipal.customerId());


        //call the service to get the user input and update account
        AccountResponseDto accountResponseDto = accountService.updateAccount(id,accountRequestDto);
        return ResponseEntity.ok(accountResponseDto);

    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getAccountByAccountNumber(
            @AuthenticationPrincipal TrustedHeaderAuthFilter.UserPrincipal userPrincipal,
            @PathVariable("accountNumber")
            String accountNumber) {

        log.info("Getting account {} for user: {}", accountNumber, userPrincipal.userId());

        //call the service to get  the response DTO(what user will see)
       AccountResponseDto responseDto = accountService.getAccountByAccountNumber(accountNumber);
       //response dto as json in the HTTP 200 ok response
       return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByCustomerId(
            @AuthenticationPrincipal TrustedHeaderAuthFilter.UserPrincipal userPrincipal,
            @PathVariable Long customerId) {

        log.info("Getting accounts for customer: {}", customerId);

        // Security: ensure user can only get their own accounts
        if (!userPrincipal.customerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<AccountResponseDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteAccountByAccountNumber(

            @AuthenticationPrincipal TrustedHeaderAuthFilter.UserPrincipal userPrincipal,
            @PathVariable  String accountNumber) {

        log.info("Deleting account {} for user: {}", accountNumber, userPrincipal.userId());

        accountService.deleteAccountByAccountNumber(accountNumber);
        return ResponseEntity.noContent().build();
    }
}
