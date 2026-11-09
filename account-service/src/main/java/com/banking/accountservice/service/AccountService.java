package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("creating account for :: {}", request.getEmail());

        if(accountRepository.existByEmail(request.getEmail())) {
            throw new RuntimeException("Account already Exists for this email:: " + request.getEmail());
        }

        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        // account need to generated 12 digits
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType() == AccountType.SAVINGS ? new BigDecimal(100000) : new BigDecimal(500000));
        Account savedAccount = accountRepository.save(account);
        log.info("Account Created :: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);

    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountStatus(account.getAccountStatus());
        response.setAccountNumber(account.getAccountNumber());
        response.setPhone(account.getPhone());
        response.setBalance(account.getBalance());
        response.setEmail(account.getEmail());
        response.setAccountType(account.getAccountType());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setCreatedAt(account.getCreatedAt());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        return response;
    }

    /*
    generated unqiue 12 digit number
     */
    private String generateAccountNumber() {
        String accountNumber;
        do {
            long number = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d", number);

        } while (accountRepository.existsByAccountNUmber(accountNumber));
        return accountNumber;
    }

    /**
     * get account detail by accountnumber
     * @param accountNumber
     * @return
     */

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Accont not found"));
        return mapToResponse(account);
    }

    /**
     * get account balance
     * @param accountNumber
     * @return
     */
    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Accont not found"));
        return account.getBalance();
    }

    /**
     * Block account -> called by fraud detection service via kafka
     * @param accountNumber
     */
    public void blockAccount(String accountNumber) {
        log.info("Blocking account : {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Accont not found"));
        account.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account Blocked {}", accountNumber);
    }

    /**
     * Deduct balance from  sender account
     * called by transaction service
     * @param accountNumber
     * @param amount
     */
    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("deducting balance {} from account {}", amount,accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Accont not found"));
        if(account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active" + accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient fund for account "+ accountNumber);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.info("balance updated,  new Balance : {}" ,account.getBalance());
    }

    /**
     * credit balance called by transaction service via kafka
     * @param accountNumber
     * @param amount
     */
    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("crediting {} to account : {}" ,amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Accont not found"));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Balance credited. New Balance: {}", account.getBalance());
    }
}
