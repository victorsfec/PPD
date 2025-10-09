package br.com.victorsfec.halma.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// A classe ClientHandler herda de Thread, cada instância pode rodar em seu próprio fluxo de execução.
public class ClientHandler extends Thread {
    private final Socket clientSocket; // O socket que representa a conexão com um cliente específico.
    private PrintWriter out;  // O PrintWriter para enviar mensagens ao cliente.
    private BufferedReader in; // O BufferedReader para ler mensagens do cliente.
    private GameSession gameSession; // A referência para a sessão de jogo à qual este cliente pertence.
    private String playerName = "Jogador Anônimo"; // O nome do jogador, com um valor padrão.

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    // Definir o nome do jogador
    public void setPlayerName(String name) { this.playerName = name; }
    // Obter o nome do jogador.
    public String getPlayerName() { return playerName; }
    //Associar este handler a uma sessão de jogo.
    public void setGameSession(GameSession gameSession) { this.gameSession = gameSession; }

    //Obter o BufferedReader , inicializando-o se for nulo.
    public BufferedReader getInputStream() throws IOException {
        if (in == null) in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return in;
    }

    //Obter o PrintWriter , inicializando-o se for nulo.
    public PrintWriter getOutputStream() throws IOException {
        if (out == null) out = new PrintWriter(clientSocket.getOutputStream(), true);
        return out;
    }

    // É executado quando a thread é iniciada.
    @Override
    public void run() {
        try {
            // Garante que os streams de saída e entrada sejam inicializados
            getOutputStream();
            getInputStream();
            // Se o cliente ainda não está em uma sessão de jogo, envia uma mensagem de status.
            if (gameSession == null) sendMessage("INFO:Aguardando oponente...");
            // Variável para armazenar a linha lida do cliente.
            String inputLine;
            // Loop que lê continuamente as mensagens do cliente.
            while ((inputLine = in.readLine()) != null) {
                // Se o cliente já está em uma sessão de jogo encaminha a mensagem para a sessão de jogo para ser processada.
                if (gameSession != null) {
                    gameSession.processMessage(inputLine, this);
                }
            }
        } catch (IOException e) {
             // Se um IOException ocorrer, imprime uma mensagem no console.
            System.out.println("Cliente desconectado: " + playerName + " (" + clientSocket.getInetAddress() + ")");
            if (gameSession != null) {
                gameSession.handleDisconnect(this); //Notifica a sessão do jogo sobre a desconexão.
            }
        } finally {
            try {
                clientSocket.close(); // Fecha o socket do cliente para liberar os recursos.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        // Verifica se o stream de saída foi inicializado.
        if (out != null) {
            // Registo para depuração
            System.out.println("SERVER -> " + playerName + ": " + message);
            out.println(message);
        }
    }

    public void shutdown() {
        try {
            // Verifica se o socket não é nulo e não está fechado.
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            // Se ocorrer um erro durante o desligamento, imprime uma mensagem de erro.
            System.err.println("Erro durante o desligamento do cliente: " + e.getMessage());
        }
    }
}