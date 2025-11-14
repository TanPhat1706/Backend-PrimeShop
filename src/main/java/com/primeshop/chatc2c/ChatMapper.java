package com.primeshop.chatc2c;

import org.springframework.stereotype.Component;

@Component
public class ChatMapper {
    public ConversationSummaryResponse toSummary(Conversation c) {
        return ConversationSummaryResponse.builder()
                .conversationId(c.getId())
                .sellerId(c.getSellerId())
                .customerId(c.getCustomerId())
                .lastMessage(
                    c.getMessages().isEmpty() ? null :
                    c.getMessages().get(c.getMessages().size() - 1).getContent()
                )
                .lastMessageAt(c.getLastMessageAt())
                .build();
    }
    
    public MessageResponse toMessageResponse(Message m) {
        return MessageResponse.builder()
                .messageId(m.getId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public ConversationDetailResponse toDetail(Conversation c) {
        return ConversationDetailResponse.builder()
                .conversationId(c.getId())
                .sellerId(c.getSellerId())
                .customerId(c.getCustomerId())
                .messages(c.getMessages()
                        .stream()
                        .map(this::toMessageResponse)
                        .toList())
                .build();
    }
}
