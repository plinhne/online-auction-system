package com.auction.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket socket;
    private final Gson gson = new Gson();
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new ObjectOutputStream(socket.getOutputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            out.println(gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "ERROR");
            error.addProperty("message", "Invalid request format");
            out.println(gson.toJson(error));
        }
    }

    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }
}
