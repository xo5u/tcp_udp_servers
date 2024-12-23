
import java.io.*;
import java.net.*;

public class ServerTCP {

    private static final int PORT = 111; // Port number
    private static final String FILENAME = "src/students.txt"; // File to store student records
    private static final String CREDENTIALS_FILE = "src/credentials.txt"; // Credentials file

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket connectionSocket = serverSocket.accept();
                System.out.println("New client connected.");

                try (DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                     DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream())) {

                    // Step 1: Authentication
                    String authRequest = inFromClient.readLine();
                    System.out.println("Received auth request: " + authRequest);
                    if (authRequest != null && authRequest.startsWith("LOGIN:")) {
                        String[] authParts = authRequest.split(":");
                        if (authParts.length == 3) {
                            String username = authParts[1].trim();
                            String password = authParts[2].trim();
                            System.out.println("Processing login for: " + username);

                            if (authenticateUser(username, password)) {
                                outToClient.writeBytes("Authentication successful\n");
                                System.out.println("Authentication successful for: " + username);
                            } else {
                                outToClient.writeBytes("Authentication failed\n");
                                System.out.println("Authentication failed for: " + username);
                                connectionSocket.close();
                                continue; // Move to next client
                            }


                        } else {
                            outToClient.writeBytes("Invalid LOGIN format. Use LOGIN:username:password\n");
                            System.out.println("Invalid LOGIN format received.");
                            connectionSocket.close();
                            continue; // Move to next client
                        }
                    } else {
                        outToClient.writeBytes("Expected LOGIN command. Disconnecting...\n");
                        System.out.println("Expected LOGIN command. Disconnecting client.");
                        connectionSocket.close();
                        continue; // Move to next client
                    }

                    // Step 2: Handle Client Commands
                    String clientRequest;
                    while ((clientRequest = inFromClient.readLine()) != null) {
                        System.out.println("Received request: " + clientRequest);
                        String[] commandParts = clientRequest.split(":");

                        try {
                            switch (commandParts[0]) {
                                case "INQ":
                                    handleInquiry(commandParts[1], FILENAME, outToClient);
                                    break;
                                case "ADD":
                                    handleAdd(commandParts[1], FILENAME, outToClient);
                                    break;
                                case "UPD":
                                    handleUpdate(commandParts[1], FILENAME, outToClient);
                                    break;
                                case "DEL":
                                    handleDelete(commandParts[1], FILENAME, outToClient);
                                    break;
                                default:
                                    outToClient.writeBytes("Unknown request type.\n");
                                    System.out.println("Unknown request type: " + commandParts[0]);
                            }
                        } catch (Exception e) {
                            outToClient.writeBytes("Error processing request: " + e.getMessage() + "\n");
                            System.err.println("Error processing request: " + e.getMessage());
                        }
                    }

                } catch (IOException e) {
                    System.err.println("Error handling client request: " + e.getMessage());
                }

                System.out.println("Client disconnected.");
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static boolean authenticateUser(String username, String password) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(CREDENTIALS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] credentials = line.split(":");
                if (credentials[0].equals(username) && credentials[1].equals(password)) {
                    return true; // Authentication successful
                }
            }
        }
        return false; // Authentication failed
    }

    private static void handleInquiry(String studentId, String fileName, DataOutputStream outToClient) throws IOException {
        String result = searchStudentInFile(studentId, fileName);
        if (result != null) {
            outToClient.writeBytes("Student found: " + result + "\n");
        } else {
            outToClient.writeBytes("Student ID " + studentId + " not found.\n");
        }
    }

    private static void handleAdd(String studentData, String fileName, DataOutputStream outToClient) throws IOException {
        String[] data = studentData.split(",");


        if(data.length>5  || data.length<5 )
        {
            outToClient.writeBytes(" the filed must 5 filed    id,name,sex,law,birthdate\n");
            return;
        }

        String studentId = data[0].trim();
        if (searchStudentInFile(studentId, fileName) != null) {
            outToClient.writeBytes("Student ID " + studentId + " already exists.\n");
        }

        else {
            appendToFile(fileName, studentData);
            outToClient.writeBytes("Student added successfully.\n");
        }

    }



    private static void handleUpdate(String studentData, String fileName, DataOutputStream outToClient) throws IOException {

        String[] data = studentData.split(",");

        // Check if the number of fields is exactly 5
        if (data.length != 5) {
            outToClient.writeBytes("The record must have exactly 5 fields: id,name,sex,law,birthdate\n");
            return; // Exit the method if the data is invalid
        }

        String studentId = data[0].trim();

        // Try to update the student record
        if (updateStudentInFile(studentId, fileName, studentData)) {
            outToClient.writeBytes("Student updated successfully.\n");
        } else {
            outToClient.writeBytes("Student ID " + studentId + " not found.\n");
        }


    }




    private static void handleDelete(String studentId, String fileName, DataOutputStream outToClient) throws IOException {
        if (deleteStudentFromFile(studentId, fileName)) {
            outToClient.writeBytes("Student deleted successfully.\n");
        } else {
            outToClient.writeBytes("Student ID " + studentId + " not found.\n");
        }
    }

    private static String searchStudentInFile(String studentId, String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] record = line.split(",");
                if (record[0].trim().equals(studentId)) {
                    return String.join(",", record);
                }
            }
        }
        return null;
    }

    private static void appendToFile(String fileName, String studentData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(studentData);
            writer.newLine();
        }
    }

    private static boolean updateStudentInFile(String studentId, String fileName, String newStudentData) throws IOException {
        File file = new File(fileName);
        File tempFile = new File(fileName + "_temp");
        boolean updated = false;


        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] record = line.split(",");
                if (record[0].trim().equals(studentId)) {
                    writer.write(newStudentData);
                    updated = true;
                } else {
                    writer.write(line);
                }
                writer.newLine();
            }
        }

        if (updated) {
            file.delete();
            tempFile.renameTo(file);
        }
        return updated;
    }

    private static boolean deleteStudentFromFile(String studentId, String fileName) throws IOException {
        File file = new File(fileName);
        File tempFile = new File(fileName + "_temp");
        boolean deleted = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] record = line.split(",");
                if (record[0].trim().equals(studentId)) {
                    deleted = true;
                } else {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }

        if (deleted) {
            file.delete();
            tempFile.renameTo(file);
        }
        return deleted;
    }
}
