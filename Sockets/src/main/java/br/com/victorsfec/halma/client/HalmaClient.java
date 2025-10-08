package br.com.victorsfec.halma.client;

import br.com.victorsfec.halma.shared.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HalmaClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final GameFrame gameFrame;
    private String lastGameStats;
    private volatile boolean gameIsOver = false;

    // Campos para guardar dados da conexão para reconexão
    private String playerName;
    private String serverAddress;
    private int serverPort;
    private volatile boolean isTryingToReconnect = false;

    public HalmaClient() {
        gameFrame = new GameFrame(this);

        // Painel para solicitar dados de conexão
        JTextField nameField = new JTextField("Jogador");
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("12345");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Seu Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Endereço IP do Servidor:"));
        panel.add(ipField);
        panel.add(new JLabel("Porta do Servidor:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Conectar ao Jogo Halma",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            this.playerName = nameField.getText();
            this.serverAddress = ipField.getText();
            String portStr = portField.getText();

            // Validação dos campos de entrada
            if (this.playerName.trim().isEmpty() || this.serverAddress.trim().isEmpty() || portStr.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Todos os campos devem ser preenchidos.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }

            try {
                this.serverPort = Integer.parseInt(portStr);
                if (this.serverPort <= 0 || this.serverPort > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "A porta deve ser um número válido entre 1 e 65535.", "Erro de Porta", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }

            gameFrame.setPlayerName(this.playerName);
            gameFrame.setVisible(true);
            connect(this.playerName, this.serverAddress, this.serverPort);
        } else {
            System.exit(0);
        }
    }

    /**
     * Tenta se reconectar ao servidor em um loop.
     */
    private void attemptReconnection() {
        if (isTryingToReconnect) {
            return;
        }
        isTryingToReconnect = true;

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && isTryingToReconnect) {
                try {
                    SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Conexão perdida. Tentando reconectar..."));
                    shutdown(); // Garante que a conexão antiga está fechada

                    Thread.sleep(5000); // Espera 5 segundos

                    socket = new Socket(serverAddress, serverPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
                    // Se reconectado, reenvia o nome para o servidor
                    out.println(Protocol.SET_NAME + Protocol.SEPARATOR + playerName);
                    
                    new Thread(new ServerListener()).start(); // Inicia um novo listener

                    SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Reconectado! Aguardando oponente..."));
                    
                    isTryingToReconnect = false; // Sai do loop de reconexão
                    break;

                } catch (IOException e) {
                    System.err.println("Falha na reconexão, tentando novamente em 5s...");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isTryingToReconnect = false;
                }
            }
        }).start();
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("CLIENT (" + gameFrame.getPlayerName() + "): Mensagem recebida: " + serverMessage);
                    final String messageForUI = serverMessage;
                    SwingUtilities.invokeLater(() -> processServerMessage(messageForUI));
                }
            } catch (IOException e) {
                // Se a conexão for perdida, inicia a tentativa de reconexão
                if (!gameIsOver && !isTryingToReconnect) {
                    System.out.println("Conexão com o servidor perdida. Iniciando tentativa de reconexão.");
                    attemptReconnection();
                }
            }
        }

        private void processServerMessage(String message) {
            if (gameIsOver) return;

            String[] parts = message.split(Protocol.SEPARATOR, 2);
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case Protocol.UPDATE_SCORE:
                    String[] scores = data.split(Protocol.SEPARATOR);
                    if (scores.length >= 2) {
                        int p1Moves = Integer.parseInt(scores[0]);
                        int p2Moves = Integer.parseInt(scores[1]);
                        gameFrame.updateScores(p1Moves, p2Moves);
                    }
                    break;
                case Protocol.VALID_MOVE:
                case Protocol.OPPONENT_MOVED:
                    String[] coords = data.split(Protocol.SEPARATOR);
                    if (coords.length >= 4) {
                        gameFrame.updateBoard(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                    }
                    break;
                case Protocol.JUMP_MOVE:
                    String[] jumpCoords = data.split(Protocol.SEPARATOR);
                    if (jumpCoords.length >= 4) {
                        gameFrame.updateBoardAndKeepSelection(Integer.parseInt(jumpCoords[0]), Integer.parseInt(jumpCoords[1]), Integer.parseInt(jumpCoords[2]), Integer.parseInt(jumpCoords[3]));
                    }
                    break;
                case Protocol.CHAIN_JUMP_OFFER:
                    String[] newCoords = data.split(Protocol.SEPARATOR);
                    if (newCoords.length >= 2) {
                        gameFrame.updateBoardAfterJumpAndPrompt(Integer.parseInt(newCoords[0]), Integer.parseInt(newCoords[1]));
                    }
                    break;
                case Protocol.VALID_MOVES_LIST:
                    List<Point> moves = new ArrayList<>();
                    if (!data.isEmpty()) {
                        String[] movePairs = data.split(";");
                        for (String pair : movePairs) {
                            String[] moveCoords = pair.split(",");
                            moves.add(new Point(Integer.parseInt(moveCoords[0]), Integer.parseInt(moveCoords[1])));
                        }
                    }
                    gameFrame.showValidMoves(moves);
                    break;
                case Protocol.VICTORY:
                    handleGameEnd("Parabéns, você ganhou!", "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.DEFEAT:
                    String defeatMessage = !data.isEmpty() ? data : "Você perdeu a partida.";
                    handleGameEnd(defeatMessage, "Fim de jogo", JOptionPane.WARNING_MESSAGE);
                    break;
                case Protocol.OPPONENT_FORFEIT:
                    handleGameEnd("Seu oponente desistiu. Você ganhou!", "Vitória", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.GAME_OVER_STATS:
                    lastGameStats = data;
                    break;
                case Protocol.WELCOME:
                    gameFrame.setPlayerId(Integer.parseInt(data));
                    break;
                case Protocol.OPPONENT_FOUND:
                    String opponentName = data.isEmpty() ? "Oponente" : data;
                    gameFrame.setOpponentName(opponentName);
                    gameFrame.updateStatus("Oponente encontrado: " + opponentName + ". Iniciando partida...");
                    break;
                case Protocol.SET_TURN:
                    gameFrame.setMyTurn("YOUR_TURN".equals(data));
                    break;
                case Protocol.ERROR:
                    JOptionPane.showMessageDialog(gameFrame, data, "Erro", JOptionPane.ERROR_MESSAGE);
                    break;
                case Protocol.INFO:
                    gameFrame.updateStatus(data);
                    break;
                case Protocol.CHAT_MESSAGE:
                    gameFrame.addChatMessage(data);
                    break;
            }
        }

        private void handleGameEnd(String message, String title, int messageType) {
            if (gameIsOver) return;
            gameIsOver = true;

            JOptionPane.showMessageDialog(gameFrame, message, title, messageType);
            if (lastGameStats != null) {
                new ResultsDialog(gameFrame, lastGameStats).setVisible(true);
            }
            gameFrame.closeApplication();
            shutdown();
        }
    }

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("CLIENT: Erro durante fechamento do socket: " + e.getMessage());
        }
    }

    public void connect(String playerName, String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(Protocol.SET_NAME + Protocol.SEPARATOR + playerName);
            new Thread(new ServerListener()).start();
            gameFrame.updateStatus("Conectado. Aguardando por um oponente...");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gameFrame, "Não foi possível se conectar ao servidor.", "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            // Inicia a tentativa de reconexão se a conexão inicial falhar
            if (!isTryingToReconnect) {
                attemptReconnection();
            }
        }
    }

    public void sendMove(int startRow, int startCol, int endRow, int endCol) {
        if (out != null) {
            out.println(Protocol.MOVE + Protocol.SEPARATOR + startRow + Protocol.SEPARATOR + startCol + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol);
        }
    }

    public void sendChatMessage(String message) {
        if (out != null) {
            out.println(Protocol.CHAT + Protocol.SEPARATOR + message);
        }
    }

    public void sendForfeit() {
        if (out != null) {
            out.println(Protocol.FORFEIT);
        }
    }

    public void sendEndChainJump() {
        if (out != null) {
            out.println(Protocol.END_CHAIN_JUMP);
        }
    }

    public void sendGetValidMoves(int row, int col) {
        if (out != null) {
            out.println(Protocol.GET_VALID_MOVES + Protocol.SEPARATOR + row + Protocol.SEPARATOR + col);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HalmaClient::new);
    }
}