package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.ItemController;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final MessageRouter router;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;

        // Wiring: DAOs → Services → Controllers → Router
        UserDAO userDAO       = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO         = new BidDAO();
        ItemDAO itemDAO       = new ItemDAO();

        UserService userService         = new UserService(userDAO);
        AuthService authService         = new AuthService(userService);
        AuctionService auctionService   = new AuctionService(auctionDAO);
        BidService bidService           = new BidService(auctionDAO, bidDAO);
        ItemService itemService         = new ItemService(itemDAO);

        // AutoBidService lấy từ BidService để dùng chung auctionCache
        AutoBidService autoBidService = bidService.getAutoBidService();

        AuthController authController       = new AuthController(authService, userService);
        AuctionController auctionController = new AuctionController(auctionService);
        BidController bidController         = new BidController(bidService, autoBidService);
        ItemController itemController       = new ItemController(itemService);

        this.router = new MessageRouter(authController, auctionController, bidController, itemController);
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush=true

            String line;
            while ((line = in.readLine()) != null) {
                String response = router.route(line);
                out.println(response);
            }
        } catch (IOException e) {
            logger.info("Client disconnected: {}", socket.getInetAddress().getHostAddress());
        } finally {
            cleanup();
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public int getCurrentAuctionId() {
        return router.getCurrentAuctionId();
    }

    private void cleanup() {
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