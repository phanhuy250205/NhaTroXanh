
package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/web/host")
public class HostContractsController {

    @Autowired
    private ContractService contractService;

    @GetMapping("/{ownerId}/contracts")
    public ResponseEntity<List<Contracts>> getContractsByOwnerId(@PathVariable Integer ownerId) {
        if (ownerId == null || ownerId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByOwnerId(ownerId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/{ownerId}/contracts/count")
    public ResponseEntity<Long> countContractsByOwnerId(@PathVariable Integer ownerId) {
        if (ownerId == null || ownerId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Long count = contractService.countContractsByOwnerId(ownerId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{ownerId}/contracts/count/status")
    public ResponseEntity<Long> countContractsByOwnerIdAndStatus(
            @PathVariable Integer ownerId,
            @RequestParam String status
    ) {
        if (ownerId == null || ownerId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Contracts.Status contractStatus;
        try {
            contractStatus = Contracts.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(0L);
        }
        Long count = contractService.countContractsByOwnerIdAndStatus(ownerId, contractStatus);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{ownerId}/revenue")
    public ResponseEntity<Float> getTotalRevenueByOwnerId(@PathVariable Integer ownerId) {
        if (ownerId == null || ownerId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Float revenue = contractService.getTotalRevenueByOwnerId(ownerId);
        return ResponseEntity.ok(revenue != null ? revenue : 0.0f);
    }

    @GetMapping("/{ownerId}/contracts/expiring")
    public ResponseEntity<List<Contracts>> getExpiringContractsByOwnerId(@PathVariable Integer ownerId) {
        if (ownerId == null || ownerId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByOwnerId(ownerId)
                .stream()
                .filter(c -> c.getStatus() == Contracts.Status.ACTIVE && isExpiringSoon(c.getEndDate()))
                .toList();
        return ResponseEntity.ok(contracts);
    }

    private boolean isExpiringSoon(Date endDate) {
        LocalDate endLocalDate = endDate.toLocalDate();
        LocalDate now = LocalDate.now();
        return endLocalDate.isBefore(now.plusDays(30)) && endLocalDate.isAfter(now.minusDays(1));
    }
}
