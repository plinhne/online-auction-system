package com.auction.client.network;

import com.auction.model.user.User;
import com.auction.network.NetworkMessage;
import com.auction.client.util.LoggerUtil;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkService {
    private static final NetworkService INSTANCE = new NetworkService();

    private Socket socket;

    // ĐÃ CHUYỂN ĐỔI: Dùng PrintWriter và BufferedReader để truyền văn bản
    private PrintWriter outStream;
    private BufferedReader inStream;

    private volatile User currentUser;
    private boolean isRunning = false;
    private final Gson gson = new Gson(); // Công cụ chuyển đổi Object <-> JSON

    private final BlockingQueue<NetworkMessage> outbox = new LinkedBlockingQueue<>();
    private Thread senderThread;
    private Thread listenerThread;

    private ServerListener serverListener;

    private NetworkService() {}

    public static NetworkService getInstance() {
        return INSTANCE;
    }

    public synchronized void connect(String host, int port) throws Exception {
        if (isRunning) return;

        this.socket = new Socket(host, port);

        // Khởi tạo luồng văn bản (Ghi có autoFlush = true)
        this.outStream = new PrintWriter(socket.getOutputStream(), true);
        this.inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.isRunning = true;

        startSenderWorker();

        // Truyền luồng đọc vào ServerListener
        this.serverListener = new ServerListener(this.socket, this.inStream);
        this.listenerThread = new Thread(this.serverListener);
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

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
                    NetworkMessage msg = outbox.take();
                    if (outStream != null) {
                        // ĐÃ CHUYỂN ĐỔI: Ép đối tượng NetworkMessage thành chuỗi JSON và gửi đi dạng Text
                        String jsonPayload = gson.toJson(msg);
                        outStream.println(jsonPayload);
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

    public User getCurrentUser() { return this.currentUser; }
    public synchronized void setCurrentUser(User user) { this.currentUser = user; }
    public ServerListener getServerListener() { return this.serverListener; }

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
    public synchronized void attachConnection(Socket socket, java.io.PrintWriter out, java.io.BufferedReader in) {
        this.socket = socket;
        this.outStream = out;
        this.inStream = in;
        this.isRunning = true;

        startSenderWorker();

        this.serverListener = new ServerListener(this.socket, this.inStream);
        this.listenerThread = new Thread(this.serverListener);
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }
}