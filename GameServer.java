// Nathan Perez 100754066
// Date: 2024-10-25

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameServer {
    private static ArrayList<Socket> clientSockets = new ArrayList<>();
    private static ArrayList<String> usernames = new ArrayList<>();
    private static Map<String, Integer> leaderboard = new HashMap<>();
    private static Random random = new Random();
    private static int targetNumber = 0;
    private static Map<String, Integer> previousGuesses = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(54321)) {
            System.out.println("Game server started and waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Client connected");

                // Read the username from the client
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String username = reader.readLine();
                usernames.add(username);
                leaderboard.putIfAbsent(username, 0);
                previousGuesses.put(username, null); // Initialize previous guess to null
                System.out.println(username + " has joined the game server");

                // Print the current list of usernames
                System.out.println("Current users: " + usernames);

                // Check if there are 2 players
                if (usernames.size() == 2) {
                    startNewGame();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startNewGame() {
        targetNumber = random.nextInt(10) + 1;
        System.out.println("The game has started! The target number is " + targetNumber);

        // Notify the players that the game has started
        for (int i = 0; i < clientSockets.size(); i++) {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(clientSockets.get(i).getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer.println("The game has started! The target number is between 1 and 10.");
            if (i == 0) {
                writer.println("It's your turn to guess first.");
            }
        }

        // Start a new thread to handle the game logic
        new Thread(new GameHandler()).start();
    }

    private static class GameHandler implements Runnable {
        @Override
        public void run() {
            int currentPlayerIndex = 0;

            try {
                while (true) {
                    Socket currentPlayerSocket = clientSockets.get(currentPlayerIndex);
                    PrintWriter writer = new PrintWriter(currentPlayerSocket.getOutputStream(), true);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(currentPlayerSocket.getInputStream()));

                    // Add a delay before prompting the current player to guess
                    Thread.sleep(2000); // 2-second delay
                    
                    // Prompt the current player to guess
                    writer.println("It's your turn to guess.");

                    // Read the guess from the current player
                    String guessStr = reader.readLine();
                    if (guessStr != null) {
                        int guess = Integer.parseInt(guessStr);
                        String currentPlayer = usernames.get(currentPlayerIndex);
                        System.out.println(currentPlayer + " guessed: " + guess);

                        if (guess == targetNumber) {
                            writer.println("Congratulations! You guessed the correct number.");
                            leaderboard.put(currentPlayer, leaderboard.get(currentPlayer) + 1);
                            notifyAllPlayers("Game over! " + currentPlayer + " guessed the correct number.");
                            notifyAllPlayers(displayLeaderboard());
                            break; // Break out of the loop after a correct guess
                        } else {
                            writer.println("Incorrect guess. Try again.");
                            if (previousGuesses.get(currentPlayer) != null) {
                                provideHint(writer, guess);
                            }
                            previousGuesses.put(currentPlayer, guess);
                        }
                    }

                    // Switch to the next player
                    currentPlayerIndex = (currentPlayerIndex + 1) % clientSockets.size();
                }

                // Add a delay before starting a new game to ensure all players see the messages
                Thread.sleep(5000);
                startNewGame(); // Start a new game after the current game finishes

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void provideHint(PrintWriter writer, int guess) {
            if (guess < targetNumber) {
                writer.println("Hint: The target number is higher than your previous guess.");
            } else {
                writer.println("Hint: The target number is lower than your previous guess.");
            }
        }

        private void notifyAllPlayers(String message) {
            for (Socket socket : clientSockets) {
                try {
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String displayLeaderboard() {
            StringBuilder leaderboardMessage = new StringBuilder("Leaderboard:\n");
            for (Map.Entry<String, Integer> entry : leaderboard.entrySet()) {
                leaderboardMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append(" points\n");
            }
            return leaderboardMessage.toString();
        }
    }
}