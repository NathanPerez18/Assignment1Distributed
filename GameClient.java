// Nathan Perez 100754066
//Date: 2024-10-25

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 54321)) {
            System.out.println("Connected to the game server");

            // Prompt the user to enter a username
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();

            // If the user presses enter without typing anything, generate a guest username
            if (username.isEmpty()) {
                username = "guest" + (int)(Math.random() * 1000);
            }

            // Send the username to the server
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Sending username to server: " + username);
            writer.println(username);

            // Create a BufferedReader to read messages from the server
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Continuously read messages from the server
            String serverMessage;
            while ((serverMessage = reader.readLine()) != null) {
                System.out.println("Server: " + serverMessage);

                // If it's the client's turn to guess, prompt for an integer input and send it to the server
                if (serverMessage.equals("It's your turn to guess.") || serverMessage.equals("It's your turn to guess first.")) {
                    System.out.print("Enter your guess (1-10): ");
                    int guess = scanner.nextInt();
                    scanner.nextLine(); // Consume the newline character
                    writer.println(guess);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}