package com.tinyai2;

import java.util.Date;
import java.util.List;

public class Conversation {
    private long timestamp;
    private String title;
    private List<Message> messages;
    private Date date;

    public Conversation(long timestamp, String title, List<Message> messages) {
        this.timestamp = timestamp;
        this.title = title;
        this.messages = messages;
        this.date = new Date(timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTitle() {
        return title;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public Date getDate() {
        return date;
    }

    public String getFormattedTime() {
        // Format time based on current date
        Date now = new Date();
        long diff = now.getTime() - date.getTime();

        if (diff < 60000) { // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // Less than 1 hour
            return (diff / 60000) + "m ago";
        } else if (diff < 86400000) { // Less than 1 day
            return (diff / 3600000) + "h ago";
        } else if (diff < 604800000) { // Less than 1 week
            return (diff / 86400000) + "d ago";
        } else {
            return android.text.format.DateFormat.format("MM/dd/yyyy", date).toString();
        }
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.date = new Date(timestamp);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }

    public String getPreview() {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.get(0).getContent();
    }
}