package br.com.victorsfec.halma.server;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Font;
import br.com.victorsfec.halma.shared.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HalmaServer {
    // Lista sincronizada para armazenar clientes que estão aguardando um oponente.
    private static final List<ClientHandler> waitingClients = new ArrayList<>();

    public static void main(String[] args) {
         // Solicita ao usuário para digitar a porta. "12345" é o valor padrão.
        Object portStr = JOptionPane.showInputDialog(null, "Digite a porta para iniciar o servidor:", "Configuração do Servidor", JOptionPane.QUESTION_MESSAGE, null, null, "12345");

        // Se o usuário cancelar, o programa encerra.
        if (portStr == null) {
            System.exit(0);
        }

        try {
            // Converte a porta para um número inteiro e a valida.
            int port = Integer.parseInt(portStr.toString());
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }

            // Inicia a interface gráfica do servidor e a lógica principal
            SwingUtilities.invokeLater(() -> createAndShowGUI(port));
            runServerLogic(port);

        } catch (NumberFormatException e) {
             // Mostra um erro se a porta for inválida.
            JOptionPane.showMessageDialog(null, "Porta inválida. O servidor não será iniciado.", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // Cria uma janela simples para mostrar que o servidor está online.
    private static void createAndShowGUI(int port) {
        JFrame frame = new JFrame("Status do Servidor Halma");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        JLabel statusLabel = new JLabel("Servidor online na porta " + port + ". Aguardando jogadores...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(statusLabel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    //Lógica principal do servidor: ouvir por conexões e parear jogadores.
    private static void runServerLogic(int port) {
        System.out.println("Halma Server em execução na porta " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Loop infinito para aceitar conexões continuamente.
            while (true) {
                // Aguarda um cliente se conectar e cria um Socket para ele.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                // Thread dedicada para gerenciar a comunicação com este cliente.
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                try {
                    BufferedReader in = clientHandler.getInputStream();
                    String nameLine = in.readLine();

                    if (nameLine != null && nameLine.startsWith(Protocol.SET_NAME)) {
                        String playerName = nameLine.split(Protocol.SEPARATOR, 2)[1];
                        clientHandler.setPlayerName(playerName);
                        System.out.println("SERVER: Nome do jogador definido como: " + playerName);

                        // Garantir que a lista de espera seja acessada por uma thread de cada vez.
                        synchronized (waitingClients) {
                            waitingClients.add(clientHandler);
                            clientHandler.start(); // Inicia a thread do ClientHandler.
                            
                            // Se houver dois ou mais clientes esperando, uma nova partida é criada.
                            if (waitingClients.size() >= 2) {
                                ClientHandler player1 = waitingClients.remove(0);
                                ClientHandler player2 = waitingClients.remove(0);
                                System.out.println("Pareando jogadores '" + player1.getPlayerName() + "' e '" + player2.getPlayerName() + "'.");
                                
                                // Cria uma nova sessão de jogo para os dois jogadores.
                                GameSession gameSession = new GameSession(player1, player2);
                                new Thread(gameSession).start();
                            }
                        }
                    } else {
                        System.err.println("Erro: Primeira mensagem do cliente não foi SET_NAME. Desconectando.");
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao comunicar com o cliente. Desconectando. " + e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}