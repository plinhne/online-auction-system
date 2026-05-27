package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 50;

    // CopyOnWriteArrayList: thread-safe khi iterate broadcast đồng thời add/remove client
    private static final CopyOnWriteArrayList<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        ExecutorService threadpool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket mainServer = new ServerSocket(PORT)){
            System.out.println("Server started on port: " + PORT);

            while (!mainServer.isClosed()){
                Socket clientSocket = mainServer.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                System.out.println("New client connected: " + clientIP);

                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                threadpool.submit(handler);
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }finally {
            threadpool.shutdown();
        }
    }
    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
    }

    //broadcast với toàn bộ clients
    public static void broadcast(String message) {
        for (ClientHandler handler : connectedClients) {
            handler.sendMessage(message);
        }
    }

    //broadcast trong một auction nhất định
    public static void broadcastToAuction(int id, String message) {
        for (ClientHandler handler : connectedClients) {
            if (handler.getCurrentAuctionId() == id) {
                handler.sendMessage(message);
            }
        }
    }
}
