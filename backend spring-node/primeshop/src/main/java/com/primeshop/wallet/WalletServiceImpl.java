package com.primeshop.wallet;

import com.primeshop.user.User;
import com.primeshop.user.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Override
    public WalletBalanceResponse getBalance(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        double balance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        double points = user.getPoint() != null ? user.getPoint() : 0.0;
        boolean walletActive = user.getWalletActive() != null ? user.getWalletActive() : false;
        return new WalletBalanceResponse(balance, points, walletActive);
    }

    @Override
    public void payOrder(Long userId, PaymentRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        double balance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        if (balance < request.getAmount()) {
            throw new RuntimeException("Insufficient balance");
        }
        // Trừ tiền
        user.setWalletBalance(balance - request.getAmount());
        // Tích điểm: 10% giá trị đơn hàng
        double point = user.getPoint() != null ? user.getPoint() : 0.0;
        user.setPoint(point + request.getAmount() * 0.1);
        userRepository.save(user);

        // Lưu giao dịch
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(-request.getAmount());
        tx.setType("PAY_ORDER");
        tx.setDescription("Thanh toán đơn hàng #" + request.getOrderId());
        walletTransactionRepository.save(tx);
    }

    @Override
    public QrResponse generateDepositQr(Long userId, DepositRequest request) {
        if (userId == null) throw new IllegalArgumentException("Thiếu userId");
        if (request == null || request.getAmount() == null || request.getAmount() <= 0)
            throw new IllegalArgumentException("Số tiền nạp không hợp lệ");
        // Sinh mã QR và signature giả lập
        String qrCode = "QR-" + userId + "-" + System.currentTimeMillis() + "-" + request.getAmount();
        String signature = java.util.UUID.randomUUID().toString();
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(request.getAmount());
        tx.setType("DEPOSIT_PENDING");
        tx.setDescription("Nạp tiền chờ xác nhận");
        tx.setCreatedAt(new java.util.Date());
        // Lưu signature vào description hoặc thêm trường signature nếu muốn
        tx.setDescription("Nạp tiền chờ xác nhận|signature:" + signature);
        walletTransactionRepository.save(tx);
        return new QrResponse(qrCode, request.getAmount(), signature);
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(Long userId) {
        List<WalletTransaction> txs = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return txs.stream()
            .map(tx -> new TransactionResponse(tx.getId(), tx.getAmount(), tx.getType(), tx.getDescription(), tx.getCreatedAt()))
            .collect(Collectors.toList());
    }

    @Override
    public void updateBalanceAfterDeposit(Long userId, Double amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        double balance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        user.setWalletBalance(balance + amount);
        userRepository.save(user);

        // Lưu giao dịch nạp tiền
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType("DEPOSIT");
        tx.setDescription("Nạp tiền thành công");
        walletTransactionRepository.save(tx);
    }

    @Override
    public void activateWallet(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Boolean active = user.getWalletActive();
        if (active == null || !active) {
            user.setWalletActive(true);
            userRepository.save(user);
        }
    }

    public void confirmDeposit(Long userId, Double amount, String signature) {
        // Tìm giao dịch chờ xác nhận đúng user, amount, signature
        List<WalletTransaction> pendingTxs = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        WalletTransaction tx = pendingTxs.stream()
            .filter(t -> "DEPOSIT_PENDING".equals(t.getType()) && t.getAmount().equals(amount)
                && t.getDescription() != null && t.getDescription().contains(signature))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch chờ xác nhận phù hợp"));
        // Cộng tiền vào ví
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        double balance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        user.setWalletBalance(balance + amount);
        userRepository.save(user);
        // Đánh dấu giao dịch đã xác nhận
        tx.setType("DEPOSIT");
        tx.setDescription("Nạp tiền thành công|signature:" + signature);
        walletTransactionRepository.save(tx);
    }
}