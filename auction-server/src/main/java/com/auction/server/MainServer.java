package com.auction.server;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.AuctionDAO;
import com.auction.service.AuctionScheduler;
import com.auction.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer {
    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);

    private static final int PORT            = 8080;
    private static final int THREAD_POOL_SIZE = 50;

    // CopyOnWriteArrayList: thread-safe khi iterate broadcast đồng thời add/remove client
    private static final CopyOnWriteArrayList<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        AuctionScheduler scheduler = new AuctionScheduler(new AuctionService(new AuctionDAO()));
        scheduler.start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port: {}", PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: {}", clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage());
        } finally {
            scheduler.stop();
            threadPool.shutdown();
            DatabaseConfig.close();
            logger.info("Server shutdown complete");
        }
    }

    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        logger.debug("Client removed. Active clients: {}", connectedClients.size());
    }

    /**
     * Broadcast tới tất cả client đang kết nối.
     * CopyOnWriteArrayList đảm bảo iterate an toàn dù có thread khác
     * đang add/remove cùng lúc.
     */
    public static void broadcast(String message) {
        for (ClientHandler handler : connectedClients) {
            handler.sendMessage(message);
        }
    }

    /**
     * Broadcast tới tất cả client đang xem một auction cụ thể.
     * Dùng khi có bid mới: chỉ notify đúng người quan tâm.
     */
    public static void broadcastToAuction(int auctionId, String message) {
        for (ClientHandler handler : connectedClients) {
            if (handler.getCurrentAuctionId() == auctionId) {
                handler.sendMessage(message);
            }
        }
    }
}