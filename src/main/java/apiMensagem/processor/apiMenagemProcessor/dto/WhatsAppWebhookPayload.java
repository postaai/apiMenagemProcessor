package apiMensagem.processor.apiMenagemProcessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookPayload {
    public String object;
    public List<Entry> entry;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        public String id;
        public List<Change> changes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        public String field;
        public Value value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        @JsonProperty("messaging_product")
        public String messagingProduct;

        public Metadata metadata;
        public List<Contact> contacts;
        public List<Message> messages;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("display_phone_number")
        public String displayPhoneNumber;

        @JsonProperty("phone_number_id")
        public String phoneNumberId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        public Profile profile;

        @JsonProperty("wa_id")
        public String waId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        public String from;
        public String id;
        public String timestamp;
        public String type;

        // type = "text"
        public Text text;

        // type = "audio"
        public Audio audio;

        // type = "image"
        public Media image;

        // type = "video"
        public Media video;

        // type = "document"
        public Document document;

        // type = "location"
        public Location location;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Text {
        public String body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Audio {
        @JsonProperty("mime_type")
        public String mimeType;

        public String sha256;
        public String id;
        public String url;

        public Boolean voice;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Media {
        @JsonProperty("mime_type")
        public String mimeType;

        public String sha256;
        public String id;
        public String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        public String filename;

        @JsonProperty("mime_type")
        public String mimeType;

        public String sha256;
        public String id;
        public String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        public String address;
        public Double latitude;
        public Double longitude;
        public String name;
        public String url;
    }
}
