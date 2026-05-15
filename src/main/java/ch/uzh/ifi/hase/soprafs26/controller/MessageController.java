package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Message;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessagePostDTO;
import ch.uzh.ifi.hase.soprafs26.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class MessageController {

    private final MessageService messageService;

    MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/groups/{groupId}/messages")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MessageGetDTO> getMessages(@PathVariable Long groupId,
                                            @RequestHeader("Authorization") String token) {
        List<Message> messages = messageService.getMessages(groupId, token);
        List<MessageGetDTO> dtos = new ArrayList<>();
        for (Message message : messages) {
            MessageGetDTO dto = new MessageGetDTO();
            dto.setId(message.getId());
            dto.setText(message.getText());
            dto.setSenderName(message.getSender().getUsername());
            dto.setCreatedAt(message.getCreatedAt());
            dtos.add(dto);
        }
        return dtos;
    }

    @PostMapping("/groups/{groupId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public MessageGetDTO sendMessage(@PathVariable Long groupId,
                                      @RequestBody MessagePostDTO messagePostDTO,
                                      @RequestHeader("Authorization") String token) {
        Message message = messageService.sendMessage(groupId, messagePostDTO.getText(), token);
        MessageGetDTO dto = new MessageGetDTO();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setSenderName(message.getSender().getUsername());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
