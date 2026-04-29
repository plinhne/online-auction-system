package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 50;

    public static final Map<String, ClientHandler> clientHandlers = new HashMap<>();

    public static void main(String[] args) {
        ExecutorService threadpool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket mainServer = new ServerSocket(PORT)){
            System.out.println("Server started on port: " + PORT);

            while (!mainServer.isClosed()){
                Socket clientSocket = mainServer.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                System.out.println("New client connected: " + clientIP);

                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.put(clientIP, handler);
                threadpool.submit(handler);
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }finally {
            threadpool.shutdown();
        }
    }
    public static void removeClient(String clientIP) {
        clientHandlers.remove(clientIP);
    }

    public static void broadcast(String message) {
        for (ClientHandler handler : clientHandlers.values()) {
            handler.sendMessage(message);
        }
    }
}
