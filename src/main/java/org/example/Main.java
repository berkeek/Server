package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int PORT = 9922;
    private static final State state = new State();
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {

        Thread updaterThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(15000); // Sleep for 15 seconds
                    Meme currentMeme = state.getCurrentMeme();
                    if (currentMeme != null) {
                        double averageVote = currentMeme.calculateAverage();
                        System.out.println("Average vote for current meme '" + currentMeme.getName() + "': " + averageVote);
                        state.updateLeaderboard();
                    }
                    state.updateMeme();
                    broadcast();
                } catch (InterruptedException e) {
                    System.err.println("Updater thread interrupted: " + e.getMessage());
                    break; // Exit the loop if interrupted
                }
            }
        });


        // Start the updater thread
        updaterThread.setDaemon(true); // Set as daemon so it doesn't block JVM shutdown
        updaterThread.start();



        //listen for clients
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection established with " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, state);
                addHandler(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void addHandler(ClientHandler handler){
        synchronized (clients){
            clients.add(handler);
        }
    }

    public static void removeClient(ClientHandler handler) {
        synchronized (clients) {
            clients.remove(handler);
            System.out.println("Client removed: " + handler.getClientName());
        }
    }

    public static void broadcast(){
        Meme currentMeme = state.getCurrentMeme();
        byte[] imageData = null;

        if (currentMeme != null) {
            imageData = currentMeme.getImage();
        } else {
            System.out.println("No current meme to send.");
        }

        // Create a copy of the clients list to avoid holding the lock while sending
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        // First send the meme if it exists
        if (imageData != null) {
            for (ClientHandler client : clientsSnapshot) {
                client.sendMeme(imageData);
                System.out.println("Meme broadcast is complete.");
            }
        }

        // Then send the leaderboard regardless of meme existence
        for (ClientHandler client : clientsSnapshot) {
            client.sendLeaderboard(state.getLeaderboard());
            System.out.println("Leaderboard broadcast is complete.");
        }
    }
}

