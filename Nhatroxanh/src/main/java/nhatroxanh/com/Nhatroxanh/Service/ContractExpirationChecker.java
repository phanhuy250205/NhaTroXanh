package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Repository.ContractRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class ContractExpirationChecker {

    private final ContractRepository contractRepository;
    private final EmailService emailService;

    @Autowired
    public ContractExpirationChecker(ContractRepository contractRepository, EmailService emailService) {
        this.contractRepository = contractRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkExpiringContracts() {
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysFromNow = today.plusDays(5);
        List<Contracts> activeContracts = contractRepository.findByStatus(Contracts.Status.ACTIVE);

        for (Contracts contract : activeContracts) {
            LocalDate endDate = contract.getEndDate().toLocalDate();
            if (endDate.equals(fiveDaysFromNow)) {
                contract.setStatus(Contracts.Status.EXPIRED);
                contractRepository.save(contract);
                String to = contract.getTenant().getEmail();
                String fullname = contract.getTenant().getFullname();
                String contractCode = "HD" + contract.getContractId();
                emailService.sendExpirationWarningEmail(to, fullname, contractCode, contract.getEndDate());
            }
        }
    }
}