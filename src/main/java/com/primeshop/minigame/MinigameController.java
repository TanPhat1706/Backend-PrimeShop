package com.primeshop.minigame;

import com.primeshop.voucher.Voucher;
import com.primeshop.voucher.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/minigame")
public class MinigameController {
    @Autowired
    private VoucherService voucherService;

    // Danh sách game cấu hình
    private static final List<GameInfo> GAME_LIST = List.of(
        new GameInfo("who_wants_to_be_millionaire", "Ai là triệu phú", "Trả lời đúng 5 câu hỏi để nhận voucher!", "💡"),
        new GameInfo("quiz", "Quiz nhanh", "Trả lời nhanh 3 câu hỏi, nhận voucher hấp dẫn!", "📝")
        // Có thể mở rộng thêm game khác
    );

    // Câu hỏi cho từng gameId
    private static final Map<String, List<Question>> GAME_QUESTIONS = Map.of(
        "who_wants_to_be_millionaire", List.of(
            new Question("Thủ đô của Việt Nam là gì?", List.of("Hà Nội", "Hải Phòng", "Đà Nẵng", "TP.HCM"), 0),
            new Question("2 + 2 = ?", List.of("3", "4", "5", "6"), 1),
            new Question("Màu cờ Việt Nam là?", List.of("Đỏ", "Xanh", "Vàng", "Trắng"), 0),
            new Question("Chữ cái đầu tiên trong bảng chữ cái?", List.of("A", "B", "C", "D"), 0),
            new Question("Sông nào dài nhất Việt Nam?", List.of("Sông Hồng", "Sông Mekong", "Sông Đà", "Sông Đồng Nai"), 1)
        ),
        "quiz", List.of(
            new Question("Trái đất quay quanh gì?", List.of("Mặt trời", "Mặt trăng", "Sao Hỏa", "Sao Kim"), 0),
            new Question("1 + 1 = ?", List.of("1", "2", "3", "4"), 1),
            new Question("Màu lá cây?", List.of("Xanh", "Đỏ", "Vàng", "Tím"), 0)
        )
    );

    @GetMapping("/list")
    public ResponseEntity<?> getGameList() {
        List<Map<String, Object>> games = new ArrayList<>();
        for (GameInfo g : GAME_LIST) {
            games.add(Map.of(
                "gameId", g.getGameId(),
                "name", g.getName(),
                "description", g.getDescription(),
                "icon", g.getIcon()
            ));
        }
        return ResponseEntity.ok(Map.of("games", games));
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(@RequestParam String gameId) {
        List<Question> questions = GAME_QUESTIONS.get(gameId);
        if (questions == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Game không tồn tại!"));
        }
        List<Map<String, Object>> qList = new ArrayList<>();
        for (Question q : questions) {
            qList.add(Map.of(
                "question", q.getQuestion(),
                "options", q.getOptions()
            ));
        }
        return ResponseEntity.ok(Map.of("questions", qList));
    }

    @PostMapping("/play")
    public ResponseEntity<?> playMinigame(@RequestBody MinigameRequest request, @RequestParam String gameId) {
        List<Question> questions = GAME_QUESTIONS.get(gameId);
        if (questions == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Game không tồn tại!"));
        }
        if (request.getAnswers() == null || request.getAnswers().size() != questions.size()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số lượng đáp án không hợp lệ!"));
        }
        boolean allCorrect = true;
        for (int i = 0; i < questions.size(); i++) {
            if (!Objects.equals(request.getAnswers().get(i), questions.get(i).getCorrectIndex())) {
                allCorrect = false;
                break;
            }
        }
        if (!allCorrect) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Bạn chưa trả lời đúng hết!"));
        }
        Voucher voucher = voucherService.createMinigameVoucherForUser(request.getUserId());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "voucher", voucher
        ));
    }

    // DTOs
    public static class MinigameRequest {
        private List<Integer> answers;
        private Long userId;
        public List<Integer> getAnswers() { return answers; }
        public void setAnswers(List<Integer> answers) { this.answers = answers; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
    public static class Question {
        private String question;
        private List<String> options;
        private int correctIndex;
        public Question(String question, List<String> options, int correctIndex) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
        }
        public String getQuestion() { return question; }
        public List<String> getOptions() { return options; }
        public int getCorrectIndex() { return correctIndex; }
    }
    public static class GameInfo {
        private String gameId;
        private String name;
        private String description;
        private String icon;
        public GameInfo(String gameId, String name, String description, String icon) {
            this.gameId = gameId;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }
        public String getGameId() { return gameId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
    }
} 