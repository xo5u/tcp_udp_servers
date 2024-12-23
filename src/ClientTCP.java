import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientTCP {

    private static List<String> commandHistory = new ArrayList<>();

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 111;

        try (Socket clientSocket = new Socket(serverAddress, serverPort);
             BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
             BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);

            // Step 1: Login
            System.out.print("Enter username: ");
            String username = inFromUser.readLine().trim();
            System.out.print("Enter password: ");
            String password = inFromUser.readLine().trim();

            // Send LOGIN request to the server
            String loginRequest = "LOGIN:" + username + ":" + password + "\n";
            outToServer.writeBytes(loginRequest);
            outToServer.flush(); // Ensure the message is sent immediately
            System.out.println("Sent LOGIN request: " + loginRequest);

            // Wait for server's response
            String authResponse = inFromServer.readLine();
            if (authResponse == null || authResponse.isEmpty()) {
                System.out.println("Server did not respond to authentication. Exiting...");
                return;
            }
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

                outToServer.writeBytes(userCommand + "\n");
                outToServer.flush(); // Ensure the message is sent immediately
                String serverResponse = inFromServer.readLine();
                if (serverResponse == null) {
                    System.out.println("Server closed connection unexpectedly.");
                    break;
                }
                System.out.println("Server response: " + serverResponse);
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
