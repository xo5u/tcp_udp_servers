


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientUDP {

    private static List<String> commandHistory = new ArrayList<>();
    private static final int SERVER_PORT = 54321; // Server's UDP port

    public static void main(String[] args) {
        String serverAddress = "localhost";

        try (DatagramSocket clientSocket = new DatagramSocket();
             BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in))) {

            InetAddress serverIP = InetAddress.getByName(serverAddress);
            System.out.println("Connected to server at " + serverAddress + ":" + SERVER_PORT);

            byte[] sendData;
            byte[] receiveData = new byte[1024];

            // Step 1: Login
            System.out.print("Enter username: ");
            String username = inFromUser.readLine().trim();
            System.out.print("Enter password: ");
            String password = inFromUser.readLine().trim();

            // Send LOGIN request to the server
            String loginRequest = "LOGIN:" + username + ":" + password;
            sendData = loginRequest.getBytes();
            sendPacket(clientSocket, sendData, serverIP, SERVER_PORT);

            // Wait for server's response
            String authResponse = receivePacket(clientSocket, receiveData);
            System.out.println("Server response: " + authResponse);

            if (!authResponse.contains("Authentication successful")) {
                System.out.println("Exiting...");
                return;
            }

            // Step 2: Send Commands
            while (true) {
                System.out.print("Enter command (INQ:<ID>, ADD:<Data>, UPD:<Data>, DEL:<ID>, HIST:, or EXIT to quit): ");
                String userCommand = inFromUser.readLine().trim();

                if (userCommand.equalsIgnoreCase("EXIT")) {
                    System.out.println("Exiting...");
                    break;
                }

                if (userCommand.equalsIgnoreCase("HIST:")) {
                    System.out.println("Command History:");
                    for (String cmd : commandHistory) {
                        System.out.println(cmd);
                    }
                    continue; // Skip sending to server
                }

                // Add command to history
                commandHistory.add(userCommand);

                sendData = userCommand.getBytes();
                sendPacket(clientSocket, sendData, serverIP, SERVER_PORT);

                String serverResponse = receivePacket(clientSocket, receiveData);
                System.out.println("Server response: " + serverResponse);
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void sendPacket(DatagramSocket socket, byte[] data, InetAddress serverIP, int serverPort) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverIP, serverPort);
        socket.send(sendPacket);
    }

    private static String receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivePacket);
        return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
    }




















}












