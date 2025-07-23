package nhatroxanh.com.Nhatroxanh.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @Override
    @Transactional
    public Users addBalance(Users user, Double amount, String description) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            
            if (amount == null || amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }

            // Get current balance, default to 0.0 if null
            Double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
            Double newBalance = currentBalance + amount;
            
            user.setBalance(newBalance);
            Users updatedUser = userRepository.save(user);
            
            log.info("Added {} to user {} balance. Previous: {}, New: {}. Description: {}", 
                    CURRENCY_FORMAT.format(amount), 
                    user.getUserId(), 
                    CURRENCY_FORMAT.format(currentBalance), 
                    CURRENCY_FORMAT.format(newBalance),
                    description);
            
            return updatedUser;
            
        } catch (Exception e) {
            log.error("Error adding balance for user {}: {}", user != null ? user.getUserId() : "null", e.getMessage(), e);
            throw new RuntimeException("Failed to add balance: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Users addPaymentToLandlordBalance(Payments payment) {
        try {
            if (payment == null) {
                throw new IllegalArgumentException("Payment cannot be null");
            }

            Contracts contract = payment.getContract();
            if (contract == null) {
                throw new IllegalArgumentException("Payment contract cannot be null");
            }

            Users landlord = contract.getOwner();
            if (landlord == null) {
                throw new IllegalArgumentException("Contract owner (landlord) cannot be null");
            }

            if (landlord.getRole() != Users.Role.OWNER) {
                throw new IllegalArgumentException("User is not a landlord/owner");
            }

            Float paymentAmountFloat = payment.getTotalAmount();
            if (paymentAmountFloat == null || paymentAmountFloat <= 0) {
                throw new IllegalArgumentException("Payment amount must be positive");
            }
            
            Double paymentAmount = paymentAmountFloat.doubleValue();

            String description = String.format("Payment received from tenant for invoice #%d - Room: %s", 
                    payment.getId(), 
                    contract.getRoom() != null ? contract.getRoom().getNamerooms() : "N/A");

            Users updatedLandlord = addBalance(landlord, paymentAmount, description);
            
            log.info("Successfully added payment amount {} to landlord {} balance for payment {}", 
                    CURRENCY_FORMAT.format(paymentAmount), 
                    landlord.getUserId(), 
                    payment.getId());
            
            return updatedLandlord;
            
        } catch (Exception e) {
            log.error("Error adding payment to landlord balance for payment {}: {}", 
                    payment != null ? payment.getId() : "null", e.getMessage(), e);
            throw new RuntimeException("Failed to add payment to landlord balance: " + e.getMessage(), e);
        }
    }

    @Override
    public Double getBalance(Integer userId) {
        try {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            
            return user.getBalance() != null ? user.getBalance() : 0.0;
            
        } catch (Exception e) {
            log.error("Error getting balance for user {}: {}", userId, e.getMessage(), e);
            return 0.0;
        }
    }

    @Override
    @Transactional
    public Users subtractBalance(Users user, Double amount, String description) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            
            if (amount == null || amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }

            Double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
            
            if (currentBalance < amount) {
                throw new IllegalArgumentException("Insufficient balance. Current: " + 
                        CURRENCY_FORMAT.format(currentBalance) + ", Required: " + CURRENCY_FORMAT.format(amount));
            }

            Double newBalance = currentBalance - amount;
            user.setBalance(newBalance);
            Users updatedUser = userRepository.save(user);
            
            log.info("Subtracted {} from user {} balance. Previous: {}, New: {}. Description: {}", 
                    CURRENCY_FORMAT.format(amount), 
                    user.getUserId(), 
                    CURRENCY_FORMAT.format(currentBalance), 
                    CURRENCY_FORMAT.format(newBalance),
                    description);
            
            return updatedUser;
            
        } catch (Exception e) {
            log.error("Error subtracting balance for user {}: {}", user != null ? user.getUserId() : "null", e.getMessage(), e);
            throw new RuntimeException("Failed to subtract balance: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasSufficientBalance(Integer userId, Double amount) {
        try {
            if (userId == null || amount == null || amount <= 0) {
                return false;
            }

            Double currentBalance = getBalance(userId);
            return currentBalance >= amount;
            
        } catch (Exception e) {
            log.error("Error checking sufficient balance for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
}
