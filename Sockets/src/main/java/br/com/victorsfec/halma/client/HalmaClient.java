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
    private Socket socket; // O socket para a comunicação com o servidor.
    private PrintWriter out; // O PrintWriter para enviar mensagens ao servidor.
    private BufferedReader in;  // O BufferedReader para receber mensagens do servidor.
    private final GameFrame gameFrame; // A referência final para a janela principal do jogo.
    private String lastGameStats; // Armazena as últimas estatísticas recebidas do jogo.
    private volatile boolean gameIsOver = false; //Indicar se o jogo terminou. Garante que as alterações sejam visíveis por todas as threads.

    // Campos para guardar dados da conexão para reconexão
    private String playerName;
    private String serverAddress;
    private int serverPort;
    private volatile boolean isTryingToReconnect = false; //Evitar múltiplas tentativas de reconexão simultâneas.

    public HalmaClient() {
        gameFrame = new GameFrame(this); // Cria a instância da janela do jogo, passando uma referência a este cliente.

        // Painel para solicitar dados de conexão
        JTextField nameField = new JTextField("Jogador");
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("12345");

        //Painel com layout de grade para organizar os componentes da caixa de diálogo.
        JPanel panel = new JPanel(new GridLayout(0, 1));

        // Adiciona os rótulos e campos de texto ao painel.
        panel.add(new JLabel("Seu Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Endereço IP do Servidor:"));
        panel.add(ipField);
        panel.add(new JLabel("Porta do Servidor:"));
        panel.add(portField);

        // Exibe a caixa de diálogo de confirmação com o painel.
        int result = JOptionPane.showConfirmDialog(null, panel, "Conectar ao Jogo Halma",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // Verifica se o usuário clicou em "OK".
        if (result == JOptionPane.OK_OPTION) {
            // Obtém os valores dos campos de texto e os armazena nas variáveis de instância.
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
                
                // Valida se o número da porta está no intervalo permitido.
                if (this.serverPort <= 0 || this.serverPort > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                // Se a porta for inválida, exibe um erro e encerra.
                JOptionPane.showMessageDialog(null, "A porta deve ser um número válido entre 1 e 65535.", "Erro de Porta", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }
            
            gameFrame.setPlayerName(this.playerName); // Define o nome do jogador na janela do jogo.
            gameFrame.setVisible(true);  // Torna a janela do jogo visível.
            connect(this.playerName, this.serverAddress, this.serverPort);
        } else {
            System.exit(0); // Se o usuário clicar em "Cancelar", encerra a aplicação.
        }
    }

    /**
     * Tenta se reconectar ao servidor em um loop.
     */
    private void attemptReconnection() {
        // Se já houver uma tentativa de reconexão em andamento, não faz nada.
        if (isTryingToReconnect) {
            return;
        }
        isTryingToReconnect = true;
        // Cria e inicia uma nova thread para lidar com a reconexão em segundo plano.
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && isTryingToReconnect) {
                try {
                    // Atualiza o status na GUI para informar o usuário.
                    SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Conexão perdida. Tentando reconectar..."));
                    shutdown(); // Garante que a conexão antiga está fechada

                    Thread.sleep(5000); // Espera 5 segundos

                    // Tenta estabelecer uma nova conexão com o servidor.
                    socket = new Socket(serverAddress, serverPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
                    // Se reconectado, reenvia o nome para o servidor
                    out.println(Protocol.SET_NAME + Protocol.SEPARATOR + playerName);
                    
                    // Inicia um novo listener para ouvir as mensagens do servidor na nova conexão.
                    new Thread(new ServerListener()).start(); // Inicia um novo listener

                    SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Reconectado! Aguardando oponente..."));
                    
                    // Desativa a flag de reconexão e sai do loop.
                    isTryingToReconnect = false; // Sai do loop de reconexão
                    break;

                } catch (IOException e) {
                    // Se a reconexão falhar, exibe uma mensagem no console e o loop continua.
                    System.err.println("Falha na reconexão, tentando novamente em 5s...");
                } catch (InterruptedException e) {
                    // Se a thread for interrompida, restaura o status de interrupção e para de tentar reconectar.
                    Thread.currentThread().interrupt();
                    isTryingToReconnect = false;
                }
            }
        }).start();
    }

    //Implementa Runnable para ouvir as mensagens do servidor em uma thread separada.
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                //Lê continuamente as mensagens do servidor. readLine() bloqueia até receber uma mensagem.
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    // Imprime a mensagem recebida no console do cliente para depuração.
                    System.out.println("CLIENT (" + gameFrame.getPlayerName() + "): Mensagem recebida: " + serverMessage);
                    
                    // Armazena a mensagem em uma variável final para ser usada dentro da expressão lambda.
                    final String messageForUI = serverMessage;

                    // Agenda o processamento da mensagem para garantir a segurança da thread da GUI.
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
            if (gameIsOver) return; // Se o jogo já terminou, ignora a mensagem.

            // Divide a mensagem em comando e dados.
            String[] parts = message.split(Protocol.SEPARATOR, 2);
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case Protocol.UPDATE_SCORE:
                    // Processa a atualização do placar.
                    String[] scores = data.split(Protocol.SEPARATOR);
                    if (scores.length >= 2) {
                        int p1Moves = Integer.parseInt(scores[0]);
                        int p2Moves = Integer.parseInt(scores[1]);
                        gameFrame.updateScores(p1Moves, p2Moves);
                    }
                    break;
                case Protocol.VALID_MOVE:
                case Protocol.OPPONENT_MOVED:
                    // Processa um movimento normal no tabuleiro.
                    String[] coords = data.split(Protocol.SEPARATOR);
                    if (coords.length >= 4) {
                        gameFrame.updateBoard(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                    }
                    break;
                case Protocol.JUMP_MOVE:
                     // Processa um movimento de salto, mantendo a peça selecionada.
                    String[] jumpCoords = data.split(Protocol.SEPARATOR);
                    if (jumpCoords.length >= 4) {
                        gameFrame.updateBoardAndKeepSelection(Integer.parseInt(jumpCoords[0]), Integer.parseInt(jumpCoords[1]), Integer.parseInt(jumpCoords[2]), Integer.parseInt(jumpCoords[3]));
                    }
                    break;
                case Protocol.CHAIN_JUMP_OFFER:
                    // Processa a oferta de continuar um salto em cadeia.
                    String[] newCoords = data.split(Protocol.SEPARATOR);
                    if (newCoords.length >= 2) {
                        gameFrame.updateBoardAfterJumpAndPrompt(Integer.parseInt(newCoords[0]), Integer.parseInt(newCoords[1]));
                    }
                    break;
                case Protocol.VALID_MOVES_LIST:
                    // Processa a lista de movimentos válidos recebida.
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
                    // Lida com o fim do jogo em caso de vitória.
                    handleGameEnd("Parabéns, você ganhou!", "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.DEFEAT:
                    // Lida com o fim do jogo em caso de derrota.
                    String defeatMessage = !data.isEmpty() ? data : "Você perdeu a partida.";
                    handleGameEnd(defeatMessage, "Fim de jogo", JOptionPane.WARNING_MESSAGE);
                    break;
                case Protocol.OPPONENT_FORFEIT:
                    // Lida com a vitória por desistência do oponente.
                    handleGameEnd("Seu oponente desistiu. Você ganhou!", "Vitória", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.GAME_OVER_STATS:
                    // Armazena as estatísticas do final do jogo.
                    lastGameStats = data;
                    break;
                case Protocol.WELCOME:
                    // Lida com o novo formato da mensagem WELCOME (ID:Nome)
                    String[] welcomeParts = data.split(Protocol.SEPARATOR, 2);
                    gameFrame.setPlayerId(Integer.parseInt(welcomeParts[0]));
                    if (welcomeParts.length > 1) {
                        String newPlayerName = welcomeParts[1];
                        // Atualiza o nome no cliente e na interface gráfica
                        HalmaClient.this.playerName = newPlayerName;
                        gameFrame.setPlayerName(newPlayerName);
                    }
                    break;
                case Protocol.OPPONENT_FOUND:
                    // Define o nome do oponente e atualiza o status.
                    String opponentName = data.isEmpty() ? "Oponente" : data;
                    gameFrame.setOpponentName(opponentName);
                    gameFrame.updateStatus("Oponente encontrado: " + opponentName + ". Iniciando partida...");
                    break;
                case Protocol.SET_TURN:
                    // Define de quem é o turno
                    gameFrame.setMyTurn("YOUR_TURN".equals(data));
                    break;
                case Protocol.ERROR:
                    // Exibe uma mensagem de erro.
                    JOptionPane.showMessageDialog(gameFrame, data, "Erro", JOptionPane.ERROR_MESSAGE);
                    break;
                case Protocol.INFO:
                    // Atualiza a barra de status.
                    gameFrame.updateStatus(data);
                    break;
                case Protocol.CHAT_MESSAGE:
                     // Adiciona uma mensagem ao chat.
                    gameFrame.addChatMessage(data);
                    break;
            }
        }

        private void handleGameEnd(String message, String title, int messageType) {
            if (gameIsOver) return;
            gameIsOver = true; //flag de fim de jogo.

            // Exibe a mensagem de fim de jogo
            JOptionPane.showMessageDialog(gameFrame, message, title, messageType);
            // Se houver estatísticas, exibe a caixa de diálogo de resultados.
            if (lastGameStats != null) {
                new ResultsDialog(gameFrame, lastGameStats).setVisible(true);
            }
            gameFrame.closeApplication();
            shutdown();
        }
    }

    public void shutdown() {
        try {
            // Se o socket existir e não estiver fechado, fecha-o.
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Se ocorrer um erro, imprime no console.
            System.err.println("CLIENT: Erro durante fechamento do socket: " + e.getMessage());
        }
    }

    public void connect(String playerName, String serverAddress, int port) {
        try {
            // Cria um novo socket para o endereço e porta especificados.
            socket = new Socket(serverAddress, port);

            // Inicializa os streams de entrada e saída.
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Envia a primeira mensagem com o nome do jogador.
            out.println(Protocol.SET_NAME + Protocol.SEPARATOR + playerName);

             // Inicia a thread listener do servidor.
            new Thread(new ServerListener()).start();

            // Atualiza o status na GUI.
            gameFrame.updateStatus("Conectado. Aguardando por um oponente...");
        } catch (IOException e) {
            // Se a conexão inicial falhar, exibe um erro.
            JOptionPane.showMessageDialog(gameFrame, "Não foi possível se conectar ao servidor.", "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            // Inicia a tentativa de reconexão se a conexão inicial falhar
            if (!isTryingToReconnect) {
                attemptReconnection();
            }
        }
    }

    // Método para enviar um comando de movimento ao servidor.
    public void sendMove(int startRow, int startCol, int endRow, int endCol) {
        // Se o stream de saída estiver ativo, envia a mensagem formatada.
        if (out != null) {
            out.println(Protocol.MOVE + Protocol.SEPARATOR + startRow + Protocol.SEPARATOR + startCol + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol);
        }
    }
    
    // Método para enviar uma mensagem de chat ao servidor.
    public void sendChatMessage(String message) {
        if (out != null) {
            out.println(Protocol.CHAT + Protocol.SEPARATOR + message);
        }
    }

    // Método para enviar um comando de desistência ao servidor.
    public void sendForfeit() {
        if (out != null) {
            out.println(Protocol.FORFEIT);
        }
    }

    // Método para enviar um comando para encerrar um salto em cadeia.
    public void sendEndChainJump() {
        if (out != null) {
            out.println(Protocol.END_CHAIN_JUMP);
        }
    }

    // Método para solicitar os movimentos válidos de uma peça ao servidor.
    public void sendGetValidMoves(int row, int col) {
        if (out != null) {
            out.println(Protocol.GET_VALID_MOVES + Protocol.SEPARATOR + row + Protocol.SEPARATOR + col);
        }
    }

    // Ponto de entrada da aplicação cliente.
    public static void main(String[] args) {
         // Agenda a criação da instância do HalmaClient na Event Dispatch Thread do Swing.
        SwingUtilities.invokeLater(HalmaClient::new);
    }
}