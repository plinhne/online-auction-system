package com.auction.network;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;  // Nhãn của gói tin (ví dụ: AUCTION_UPDATE_NOTIFICATION)
    private String payload;    // Nội dung dữ liệu thực tế (Chuỗi JSON của đối tượng Auction hoặc User)

    public NetworkMessage() {}

    public NetworkMessage(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}