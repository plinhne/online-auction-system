package com.auction.server;

import com.auction.model.auction.Auction;
import com.auction.model.pattern.observer.BidObserver;
import com.auction.server.controller.*;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.service.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler: lo I/O + implement BidObserver để nhận realtime update.
 * Khi client JOIN_AUCTION → đăng ký làm observer của auction đó.
 * Khi có bid mới → updateNewBid() được gọi → sendMessage() tới client.
 */
public class ClientHandler implements Runnable, BidObserver {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final MessageRouter router;
    private final AuctionService auctionService;
    private final Gson gson = new Gson();
    private BufferedReader in;
    private PrintWriter out;

    // Auction đang được observe — cần để removeObserver khi leave/disconnect
    private Auction observedAuction = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;

        // Wiring: DAOs → Services → Controllers → Router
        UserDAO userDAO       = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO         = new BidDAO();
        ItemDAO itemDAO       = new ItemDAO();

        UserService userService         = new UserService(userDAO);
        AuthService authService         = new AuthService(userService);
        this.auctionService             = new AuctionService(auctionDAO);
        BidService bidService           = new BidService(auctionDAO, bidDAO);
        ItemService itemService         = new ItemService(itemDAO);
        ImageService imageService       = new ImageService();

        AutoBidService autoBidService = bidService.getAutoBidService();

        AuthController authController       = new AuthController(authService, userService);
        AuctionController auctionController = new AuctionController(auctionService, itemService);
        BidController bidController         = new BidController(bidService, autoBidService, auctionService);
        ItemController itemController       = new ItemController(itemService, imageService);
        UserController userController       = new UserController(userService, auctionService,gson);

        this.router = new MessageRouter(authController, auctionController, bidController, itemController, userController);
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                String response = router.route(line);

                // Sau JOIN_AUCTION → đăng ký observer
                syncObserver();

                out.println(response);
            }
        } catch (IOException e) {
            logger.info("Client disconnected: {}", socket.getInetAddress().getHostAddress());
        } finally {
            cleanup();
        }
    }

    /**
     * Đồng bộ observer khi currentAuctionId thay đổi.
     * JOIN_AUCTION → addObserver, LEAVE_AUCTION → removeObserver.
     */
    private void syncObserver() {
        int currentId = router.getCurrentAuctionId();

        // Đã đăng ký đúng auction rồi → không cần làm gì
        if (observedAuction != null && observedAuction.getId() == currentId) return;

        // Remove khỏi auction cũ
        if (observedAuction != null) {
            observedAuction.removeObserver(this);
            observedAuction = null;
        }

        // Add vào auction mới
        if (currentId != -1) {
            try {
                Auction auction = auctionService.getAuctionById(currentId);
                if (auction != null) {
                    auction.addObserver(this);
                    observedAuction = auction;
                    logger.debug("ClientHandler registered as observer: auctionId={}", currentId);
                }
            } catch (Exception e) {
                logger.error("Failed to register observer for auctionId={}", currentId, e);
            }
        }
    }

    /**
     * Được gọi bởi Auction.notifyObservers() khi có bid mới.
     * Gửi BID_UPDATE event tới client này.
     */
    @Override
    public void updateNewBid(int auctionId, double newAmount, int bidderId) {
        JsonObject event = new JsonObject();
        event.addProperty("event", "BID_UPDATE");
        event.addProperty("auctionId", auctionId);
        event.addProperty("newPrice", newAmount);
        event.addProperty("bidderId", bidderId);
        sendMessage(gson.toJson(event));
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public int getCurrentAuctionId() {
        return router.getCurrentAuctionId();
    }

    private void cleanup() {
        // Remove observer trước khi disconnect
        if (observedAuction != null) {
            observedAuction.removeObserver(this);
        }
        MainServer.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.error("Cleanup error: {}", e.getMessage());
        }
    }
}