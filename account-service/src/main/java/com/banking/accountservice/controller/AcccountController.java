package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AcccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request)  {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber) {
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked successfully");
    }

    /*
    *Saga Step 1: Deduct balance
    * called by Transaction Service when transfer is initiated
     */
    @PutMapping("/{accontNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber,
                                                @RequestParam BigDecimal amount) {
        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("balance deducted Successfully");
    }

    /*
     *Saga Step 4: compensating transaction endpoint
     * called by TRANSACTION SERVICE in TWO SCENARIOS:
     * 1. fraud detection -> refunf sender (undo step 1)
     * 2. Transaction completed -> credit receiver
     */
    @PutMapping("/{accontNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok("BALANCE CREDITED SUCCESSFULLY");
    }

}

