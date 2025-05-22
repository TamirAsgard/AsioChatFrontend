package com.example.asiochatfrontend.core.model.dto;

import java.util.List;

public class ChunkUploadResponse {
    private String uploadId;
    private String messageId;
    private String chatId;
    private String jid;
    private String fileName;
    private String contentType;
    private long totalSize;
    private List<String> waitingMembersList;

    // getters and setters
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getJid() { return jid; }
    public void setJid(String jid) { this.jid = jid; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public List<String> getWaitingMembersList() { return waitingMembersList; }
    public void setWaitingMembersList(List<String> waitingMembersList) { this.waitingMembersList = waitingMembersList; }
}
