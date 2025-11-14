package com.primeshop.chatc2c;

import java.nio.file.AccessDeniedException;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.primeshop.user.User;
import com.primeshop.user.UserRepo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ChatWsController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserRepo userRepo;
    private static final Logger logger = LoggerFactory.getLogger(ChatWsController.class);

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO request,
                            Principal principal) throws AccessDeniedException {
        if (principal == null) {
            throw new AccessDeniedException("Principal is null. Interceptor failed.");
        }
        UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) principal;
        User user = (User) authToken.getPrincipal();
        System.out.println("[CONTROLLER] Đã lấy User: " + user.getUsername() + ", ID: " + user.getId());
        MessageResponse saved = chatService.saveMessage(request, user);
        Long conversationId = saved.getConversationId();
        messagingTemplate.convertAndSend(
            "/topic/conversation/" + conversationId,
            saved
        );
        System.out.println("MESSAGES RECEIVED: " + saved);
        String senderUsername = user.getUsername();
        Long receiverId = saved.getReceiverId();
        String receiverUsername = userRepo.findById(receiverId)
                                        .map(User::getUsername)
                                        .orElse(null);

        System.out.println("PAYLOAD MESSAGEEEEEEEEEEEEEEEEE: " + request.getContent());
        logger.info("Đang gửi cho sender: {} trên kênh /queue/messages", senderUsername);
        messagingTemplate.convertAndSendToUser(
            senderUsername,
            "/queue/messages",
            saved
        );
        logger.info("Đang gửi cho receiver: {} trên kênh /queue/messages", receiverUsername);
        messagingTemplate.convertAndSendToUser(
            receiverUsername,
            "/queue/messages",
            saved
        );
    }
}
