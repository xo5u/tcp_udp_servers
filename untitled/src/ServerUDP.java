package package1;


import java.io.*;
import java.net.*;

public class ServerUDP {

    private static final int PORT = 54321; // Port number
    private static final String FILENAME = "src/students.txt"; // File to store student records
    private static final String CREDENTIALS_FILE = "src/credentials.txt"; // Credentials file

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("UDP Server is listening on port " + PORT);

            byte[] receiveBuffer = new byte[1024];
            byte[] sendBuffer;

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Received: " + clientMessage);

                String response;

                try {
                    if (clientMessage.startsWith("LOGIN:")) {
                        String[] authParts = clientMessage.split(":");
                        if (authParts.length == 3) {
                            String username = authParts[1].trim();
                            String password = authParts[2].trim();

                            if (authenticateUser(username, password)) {
                                response = "Authentication successful\n";
                            } else {
                                response = "Authentication failed\n";
                            }
                        } else {
                            response = "Invalid LOGIN format. Use LOGIN:username:password\n";
                        }
                    } else {
                        String[] commandParts = clientMessage.split(":");
                        switch (commandParts[0]) {
                            case "INQ":
                                response = handleInquiry(commandParts[1], FILENAME);
                                break;
                            case "ADD":
                                response = handleAdd(commandParts[1], FILENAME);
                                break;
                            case "UPD":
                                response = handleUpdate(commandParts[1], FILENAME);
                                break;
                            case "DEL":
                                response = handleDelete(commandParts[1], FILENAME);
                                break;
                            default:
                                response = "Unknown request type.\n";
                        }
                    }
                } catch (Exception e) {
                    response = "Error processing request: " + e.getMessage() + "\n";
                    System.err.println("Error processing request: " + e.getMessage());
                }

                sendBuffer = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
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

    private static String handleInquiry(String studentId, String fileName) throws IOException {
        String result = searchStudentInFile(studentId, fileName);
        return (result != null) ? "Student found: " + result + "\n" : "Student ID " + studentId + " not found.\n";
    }

    private static String handleAdd(String studentData, String fileName) throws IOException {
        String[] data = studentData.split(",");
        if (data.length != 5) {
            return "The record must have exactly 5 fields: id,name,sex,law,birthdate\n";
        }
        String studentId = data[0].trim();
        if (searchStudentInFile(studentId, fileName) != null) {
            return "Student ID " + studentId + " already exists.\n";
        }
        appendToFile(fileName, studentData);
        return "Student added successfully.\n";
    }

    private static String handleUpdate(String studentData, String fileName) throws IOException {
        String[] data = studentData.split(",");
        if (data.length != 5) {
            return "The record must have exactly 5 fields: id,name,sex,law,birthdate\n";
        }
        String studentId = data[0].trim();
        return updateStudentInFile(studentId, fileName, studentData)
                ? "Student updated successfully.\n"
                : "Student ID " + studentId + " not found.\n";
    }

    private static String handleDelete(String studentId, String fileName) throws IOException {
        return deleteStudentFromFile(studentId, fileName)
                ? "Student deleted successfully.\n"
                : "Student ID " + studentId + " not found.\n";
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


