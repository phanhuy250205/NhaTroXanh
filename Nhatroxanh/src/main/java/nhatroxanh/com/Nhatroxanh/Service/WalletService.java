package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.entity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

/**
 * Service interface for wallet/balance operations
 */
public interface WalletService {
    
    /**
     * Add money to user's balance
     * @param user The user to add money to
     * @param amount The amount to add
     * @param description Description of the transaction
     * @return Updated user with new balance
     */
    Users addBalance(Users user, Double amount, String description);
    
    /**
     * Add payment amount to landlord's balance when tenant pays successfully
     * @param payment The successful payment
     * @return Updated landlord user with new balance
     */
    Users addPaymentToLandlordBalance(Payments payment);
    
    /**
     * Get user's current balance
     * @param userId The user ID
     * @return Current balance
     */
    Double getBalance(Integer userId);
    
    /**
     * Subtract money from user's balance
     * @param user The user to subtract money from
     * @param amount The amount to subtract
     * @param description Description of the transaction
     * @return Updated user with new balance
     */
    Users subtractBalance(Users user, Double amount, String description);
    
    /**
     * Check if user has sufficient balance
     * @param userId The user ID
     * @param amount The amount to check
     * @return true if user has sufficient balance
     */
    boolean hasSufficientBalance(Integer userId, Double amount);
}
