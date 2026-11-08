package com.tinyai2;

import java.util.Date;

public class Message {
    private String content;
    private int type;
    private long timestamp;
    private Date date;

    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;

    public Message(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.date = new Date(timestamp);
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
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
        } else {
            return android.text.format.DateFormat.format("MM/dd", date).toString();
        }
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.date = new Date(timestamp);
    }
}