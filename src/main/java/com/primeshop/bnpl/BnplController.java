package com.primeshop.bnpl;

import com.primeshop.order.Order;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderStatus;
import com.primeshop.payment.PaymentRequest;
import com.primeshop.user.UserRepo;
import com.primeshop.payment.VNPayService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bnpl")
@RequiredArgsConstructor
public class BnplController {

    private final BNPLAgreementRepository agreementRepo;
    private final BNPLInstallmentRepository installmentRepo;
    private final BNPLBlacklistRepository blacklistRepo;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final VNPayService vnPayService;

    // ========== 1️⃣ User yêu cầu trả sau ==========
    @PostMapping("/init")
    public ResponseEntity<?> initBnpl(@RequestBody BnplInitRequest req) {

        if (!List.of(1, 3, 6, 12).contains(req.getMonths())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Chỉ hỗ trợ 1 / 3 / 6 / 12 kỳ"));
        }

        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // check blacklist
        if (blacklistRepo.existsById(order.getUser().getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "User is blacklisted"));
        }

        // đã có BNPL cho order này
        var existing = agreementRepo.findByOrderId(order.getId()).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(existing);
        }

        BNPLAgreement agr = new BNPLAgreement();
        agr.setUser(order.getUser());
        agr.setOrder(order);
        agr.setProvider("INTERNAL_BNPL");
        agr.setStatus("PENDING_APPROVAL");
        agr.setTotalAmount(
                order.getFinalAmount() != null
                        ? order.getFinalAmount()
                        : order.getTotalAmount());

        agreementRepo.save(agr);

        return ResponseEntity.ok(Map.of(
                "agreementId", agr.getId(),
                "status", agr.getStatus(),
                "months", req.getMonths()));
    }

    // ========== 2️⃣ Admin duyệt BNPL + tạo lịch trả ==========
    @PostMapping("/{agreementId}/approve")
    public ResponseEntity<?> approveBnpl(
            @PathVariable Long agreementId,
            @RequestParam int months) {
        if (!List.of(1, 3, 6, 12).contains(months)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid installment option"));
        }

        BNPLAgreement agr = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));

        agr.setStatus("ACTIVE");
        agreementRepo.save(agr);

        BigDecimal total = agr.getTotalAmount();
        BigDecimal base = total.divide(BigDecimal.valueOf(months), 0, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(months)));

        for (int i = 1; i <= months; i++) {
            BNPLInstallment ins = new BNPLInstallment();
            ins.setAgreement(agr);
            ins.setInstallmentNumber(i);
            ins.setDueDate(LocalDateTime.now().plusMonths(i));
            ins.setAmount(i == 1 ? base.add(remainder) : base);
            installmentRepo.save(ins);
        }

        Order order = agr.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "BNPL approved",
                "installments", months));
    }

    // ========== 3️⃣ Xem chi tiết BNPL + lịch trả ==========
    @GetMapping("/agreements/{agreementId}")
    public ResponseEntity<?> getAgreementDetail(@PathVariable Long agreementId) {

        // ===== 1️⃣ LẤY AGREEMENT =====
        BNPLAgreement agr = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));

        // ===== 2️⃣ LẤY INSTALLMENTS =====
        var installments = installmentRepo.findByAgreement_Id(agreementId);

        // ===== 3️⃣ MAP AGREEMENT -> DTO =====
        BNPLAgreementDTO agreementDTO = new BNPLAgreementDTO();
        agreementDTO.setId(agr.getId());
        agreementDTO.setProvider(agr.getProvider());
        agreementDTO.setFundiinOrderId(agr.getFundiinOrderId());
        agreementDTO.setTotalAmount(agr.getTotalAmount());
        agreementDTO.setStatus(agr.getStatus());
        agreementDTO.setCreatedAt(agr.getCreatedAt());
        agreementDTO.setDueDate(agr.getDueDate());
        agreementDTO.setOrderId(agr.getOrder().getId());
        agreementDTO.setUsername(agr.getUser().getUsername());

        // ===== 4️⃣ MAP INSTALLMENTS -> DTO (CHỐNG JSON LẶP) =====
        var installmentDTOs = installments.stream().map(i -> {
            BNPLInstallmentDTO d = new BNPLInstallmentDTO();
            d.setId(i.getId());
            d.setInstallmentNumber(i.getInstallmentNumber());
            d.setAmount(i.getAmount());
            d.setDueDate(i.getDueDate());
            d.setPaid(i.isPaid());
            return d;
        }).toList();

        // ===== 5️⃣ RESPONSE CUỐI =====
        return ResponseEntity.ok(Map.of(
                "agreement", agreementDTO,
                "installments", installmentDTOs));
    }

    @PostMapping("/installments/{id}/pay")
    public ResponseEntity<?> payInstallment(@PathVariable Long id)
            throws UnsupportedEncodingException {

        BNPLInstallment ins = installmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        if (ins.isPaid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Installment already paid"));
        }

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(ins.getId()); // ⚠️ DÙNG installmentId
        req.setAmount(ins.getAmount()); // tiền của kỳ

        String paymentUrl = vnPayService.createPaymentUrl(req);

        return ResponseEntity.ok(Map.of(
                "installmentId", ins.getId(),
                "amount", ins.getAmount(),
                "paymentUrl", paymentUrl));
    }

    // ========== 4️⃣ Thanh toán 1 kỳ (sẽ gọi VNPAY / MoMo) ==========
    @PostMapping("/installments/{id}/pay-success")
    public ResponseEntity<?> markInstallmentPaid(@PathVariable Long id) {

        BNPLInstallment ins = installmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        ins.setPaid(true);
        installmentRepo.save(ins);

        BNPLAgreement agr = ins.getAgreement();
        boolean allPaid = installmentRepo.findAll().stream()
                .filter(i -> i.getAgreement().getId().equals(agr.getId()))
                .allMatch(BNPLInstallment::isPaid);

        if (allPaid) {
            agr.setStatus("COMPLETED");
            agreementRepo.save(agr);
        }

        return ResponseEntity.ok(Map.of(
                "paid", true,
                "bnplCompleted", allPaid));
    }

    @GetMapping("/payment/callback")
    public ResponseEntity<?> paymentCallback(@RequestParam Map<String, String> params) {

        String responseCode = params.get("vnp_ResponseCode");
        Long installmentId = Long.valueOf(params.get("vnp_TxnRef"));

        if (!"00".equals(responseCode)) {
            return ResponseEntity.badRequest().body("Payment failed");
        }

        // ✅ xử lý trực tiếp
        BNPLInstallment ins = installmentRepo.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        ins.setPaid(true);
        installmentRepo.save(ins);

        BNPLAgreement agr = ins.getAgreement();
        boolean allPaid = installmentRepo.findByAgreement_Id(agr.getId())
                .stream()
                .allMatch(BNPLInstallment::isPaid);

        if (allPaid) {
            agr.setStatus("COMPLETED");
            agreementRepo.save(agr);
        }

        return ResponseEntity.ok("OK");
    }
}
