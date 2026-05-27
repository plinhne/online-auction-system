package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BidController;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.service.BidService;
import com.auction.service.UserService;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket socket;
    private final MessageRouter router;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;

        //tạo DAO -> services -> controller -> router
        UserDAO userDAO = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();

        //nhận DAO
        UserService userService = new UserService();
        AuthService authService = new AuthService(userService);
        AuctionService auctionService = new AuctionService(auctionDAO);
        BidService bidService = new BidService();

        AuthController authController = new AuthController(authService, userService);
        AuctionController auctionController = new AuctionController(auctionService);
        BidController bidController = new BidController(bidService, auctionService);

        this.router = new MessageRouter(authController,auctionController,bidController);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new ObjectOutputStream(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                String response = router.route(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
        }finally {
            cleanup();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public int getCurrentAuctionId() {
       return router.getCurrentAuctionId();
    }

    private void cleanup() {
        MainServer.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }
}
