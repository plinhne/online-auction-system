package com.auction.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket socket;
    private final String clientIP;
    private final Gson gson = new Gson();
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientIP = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new ObjectOutputStream(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + clientIP);
        }finally {
            cleanup();
        }
    }

    private void handleMessage(String rawJson) {
        try {
            JsonObject request = JsonParser.parseString(rawJson).getAsJsonObject();
            String action = request.get("action").getAsString();

            JsonObject response = new JsonObject();

            switch (action) {
                case "PING" -> {
                    response.addProperty("status","OK");
                    response.addProperty("message", "PONG");
                }

                default -> {
                    response.addProperty("status","ERROR");
                    response.addProperty("message", "Unknown action: " + action);
                }
            }
            sendMessage(gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "ERROR");
            error.addProperty("message", "Invalid request format");
            sendMessage(gson.toJson(error));
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void cleanup() {
        MainServer.removeClient(clientIP);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }
}
