package apiMensagem.processor.apiMenagemProcessor.dto;

import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.Contact;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.Message;

import java.util.List;

public class WhatsAppResponse {
    private String messaging_product;
    private List<Contact> contacts;
    private List<Message> messages;

    // Getters e Setters
    public String getMessaging_product() {
        return messaging_product;
    }

    public void setMessaging_product(String messaging_product) {
        this.messaging_product = messaging_product;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}