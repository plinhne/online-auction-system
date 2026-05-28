package com.auction.client.network;

import com.auction.model.user.User;
import com.auction.network.NetworkMessage;
import com.auction.client.util.LoggerUtil;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkService {
    private static final NetworkService INSTANCE = new NetworkService();

    private Socket socket;
    private ObjectOutputStream outStream;
    private ObjectInputStream inStream;
    private volatile User currentUser;
    private boolean isRunning = false;

    // Hàng đợi tin nhắn gửi đi (Outbox) chống xung đột thread UI
    private final BlockingQueue<NetworkMessage> outbox = new LinkedBlockingQueue<>();
    private Thread senderThread;
    private Thread listenerThread;

    private NetworkService() {}

    public static NetworkService getInstance() {
        return INSTANCE;
    }

    public synchronized void connect(String host, int port) throws Exception {
        if (isRunning) return;

        this.socket = new Socket(host, port);
        this.outStream = new ObjectOutputStream(socket.getOutputStream());
        this.inStream = new ObjectInputStream(socket.getInputStream());
        this.isRunning = true;

        // 1. Kích hoạt Thread gửi tin nhắn tuần tự từ hàng đợi
        startSenderWorker();

        // 2. ĐÃ SỬA: Truyền chính xác đối tượng Socket vào ServerListener theo đúng thiết kế của bạn
        ServerListener listener = new ServerListener(this.socket);
        this.listenerThread = new Thread(listener);
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    // Hàm gửi tin nhắn không chặn (Non-blocking Outbox)
    public void sendNetworkMessage(NetworkMessage msg) {
        if (!isRunning) {
            LoggerUtil.warning("Mất kết nối mạng. Không thể gửi tin nhắn.");
            return;
        }
        outbox.offer(msg);
    }

    private void startSenderWorker() {
        senderThread = new Thread(() -> {
            while (isRunning) {
                try {
                    NetworkMessage msg = outbox.take(); // Đợi đến khi có tin nhắn trong queue
                    if (outStream != null) {
                        outStream.writeObject(msg);
                        outStream.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi khi gửi gói tin từ hàng đợi", e);
                }
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();
    }

    // Thread-safe Getters/Setters cho User toàn cục
    public User getCurrentUser() { return this.currentUser; }
    public synchronized void setCurrentUser(User user) { this.currentUser = user; }

    // Dọn dẹp tài nguyên khi tắt ứng dụng
    public synchronized void close() {
        this.isRunning = false;
        if (senderThread != null) senderThread.interrupt();
        if (listenerThread != null) listenerThread.interrupt();
        try {
            if (outStream != null) outStream.close();
            if (inStream != null) inStream.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            LoggerUtil.error("Lỗi khi đóng kết nối NetworkService", e);
        }
    }
}
