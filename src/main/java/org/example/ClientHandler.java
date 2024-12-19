package org.example;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final State state;
    private DataOutputStream dataOutputStream;


    public ClientHandler(Socket socket, State state) {
        this.clientSocket = socket;
        this.state = state;
    }


    @Override
    public void run() {
        try {
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            InputStream inputStream = clientSocket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            OutputStream outputStream = clientSocket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // Continuously listen for client messages
            while (true) {
                // Blocking read to wait for the next message
                int messageType;
                try {
                    messageType = dataInputStream.readByte();
                } catch (EOFException eof) {
                    // Client has closed the connection
                    System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                    break;
                }

                switch (messageType) {
                    case 1:
                        handleVote(dataInputStream);
                        break;
                    case 2:
                        handleUpload(dataInputStream);
                        break;
                    default:
                        System.err.println("Unknown message type: " + messageType);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client (" + clientSocket.getInetAddress() + "): " + e.getMessage());
        } finally {
            // Clean up resources and remove this handler from the server's client list
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing connection (" + clientSocket.getInetAddress() + "): " + e.getMessage());
            }
            System.out.println("Connection closed with client: " + clientSocket.getInetAddress());
            Main.removeClient(this);
        }
    }


    private void handleVote(DataInputStream dataInputStream) throws IOException {
        try {
            String clientId = clientSocket.getInetAddress().toString(); // Unique client identifier

            int vote = dataInputStream.readInt();
            System.out.println("Vote received from " + clientId + ": " + vote);
            synchronized (State.votedClients){
                if (State.votedClients.contains(clientId)){
                    System.out.println("Client " + clientId + " has already voted.");
                    return;
                }
                state.addVote(vote);
                State.votedClients.add(clientId);
                System.out.println("Client " + clientId + " voted.");
            }
        } catch (Exception e) {
            System.err.println("Error handling vote: " + e.getMessage());
        }
    }

    private void handleUpload(DataInputStream dataInputStream) throws IOException {
        try{
            // Read the size of the incoming image
            int imageSize = dataInputStream.readInt();
            System.out.println("Expected Image Size: " + imageSize + " bytes");

            // Initialize a byte array with the specified size
            byte[] image = new byte[imageSize];

            // Read exactly 'imageSize' bytes from the stream
            dataInputStream.readFully(image);

            System.out.println("Image received successfully. Size: " + image.length + " bytes");

            // Process the image as needed
            Meme meme = new Meme(0,"berkek",image);
            state.addMeme(meme);
        }
        catch (Exception e){
            System.err.println("Error handling image: " + e.getMessage());
        }
    }

    public void sendMeme(byte[] imageData) {
        try {
            dataOutputStream.writeByte(3); // message type 3 for new meme
            dataOutputStream.writeInt(imageData.length);
            dataOutputStream.write(imageData);
            dataOutputStream.flush(); // sends the data
        } catch (IOException e) {
            System.err.println("Error sending meme to client: " + e.getMessage());
        }
    }

    public void sendLeaderboard(List<Meme> leaderboard) {
        try {
            dataOutputStream.writeByte(4); // message type 4 for leaderboard
            dataOutputStream.writeInt(leaderboard.size());

            for (Meme meme : leaderboard) {
                byte[] imageData = meme.getImage();
                dataOutputStream.writeInt(imageData.length);
                dataOutputStream.write(imageData);
                System.out.println(meme.calculateAverage());
                dataOutputStream.writeDouble(meme.calculateAverage());
            }

            dataOutputStream.flush(); //sends the leaderboard
        } catch (IOException e) {
            System.err.println("Error sending leaderboard to client: " + e.getMessage());
        }
    }

    public String getClientName() {
        return clientSocket.getInetAddress().toString();
    }


}

