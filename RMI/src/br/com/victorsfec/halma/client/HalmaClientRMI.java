package br.com.victorsfec.halma.client;

import br.com.victorsfec.halma.common.IClientCallback;
import br.com.victorsfec.halma.common.IGameSession;
import br.com.victorsfec.halma.common.IServerOperations;
import javax.swing.*;
import java.awt.GridLayout;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class HalmaClientRMI {
    // Stubs para comunicação RMI
    private IServerOperations serverStub;
    private IGameSession gameSessionStub;
    private IClientCallback callbackStub;
    private ClientCallbackImpl callbackImpl;

    private final GameFrame gameFrame; // A UI é reutilizada
    
    // Dados da conexão (para reconexão)
    private String playerName;
    private String serverAddress;
    private int serverPort;
    private volatile boolean gameIsOver = false;

    public HalmaClientRMI() {
        // Passa this do tipo HalmaClientRMI para o GameFrame.
        // GameFrame não precisa saber que é RMI, só precisa que os métodos sendMove(), sendChatMessage() e outros existam.
        gameFrame = new GameFrame(this);

        // Lógica de UI do construtor original
        JTextField nameField = new JTextField("Jogador");
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("1099"); // Porta RMI padrão

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Seu Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Endereço IP do Servidor:"));
        panel.add(ipField);
        panel.add(new JLabel("Porta RMI do Servidor:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Conectar ao Jogo Halma (RMI)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            this.playerName = nameField.getText();
            this.serverAddress = ipField.getText();
            // Validação de campos
            try {
                this.serverPort = Integer.parseInt(portField.getText());
                gameFrame.setPlayerName(this.playerName);
                gameFrame.setVisible(true);
                connect(); // Inicia a conexão
            } catch (NumberFormatException e) {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    public void connect() {
        if (gameIsOver) return;
        try {
            // 1. Procura o servidor de matchmaking
            String url = "rmi://" + serverAddress + ":" + serverPort + "/HalmaServer";
            serverStub = (IServerOperations) Naming.lookup(url);
            
            // 2. Cria o objeto de callback
            callbackImpl = new ClientCallbackImpl(gameFrame, this);
            // Exporta o objeto para o RMI runtime
            callbackStub = (IClientCallback) UnicastRemoteObject.toStub(callbackImpl);

            // 3. Conecta-se à fila de espera (chamada assíncrona)
            serverStub.connect(playerName, callbackStub);
            
            gameFrame.updateStatus("Conectado ao RMI. Aguardando por um oponente...");
        } catch (Exception e) {
            gameFrame.updateStatus("Falha ao conectar ao servidor RMI.");
            JOptionPane.showMessageDialog(gameFrame, "Não foi possível se conectar ao servidor RMI:\n" + e.getMessage(), "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            // Tenta reconectar
            attemptReconnection();
        }
    }
    
    // Chamado pelo ClientCallbackImpl quando o jogo começa
    public void setGameSession(IGameSession gameSession, String newPlayerName) {
        this.gameSessionStub = gameSession;
        this.playerName = newPlayerName; // Atualiza o nome (caso tenha sido modificado)
        gameFrame.setPlayerName(newPlayerName);
    }
    
    public void setGameIsOver() {
        this.gameIsOver = true;
        // Garante que o objeto de callback seja desexportado
        if (callbackImpl != null) {
            try {
                UnicastRemoteObject.unexportObject(callbackImpl, true);
            } catch (Exception e) {
                // ignora
            }
        }
    }

    // --- Métodos chamados pela UI, GameFrame ---
    //A UI chama estes métodos, que por sua vez chamam o RMI

    public void sendMove(int startRow, int startCol, int endRow, int endCol) {
        if (gameSessionStub != null) {
            try {
                gameSessionStub.sendMove(gameFrame.getPlayerId(), startRow, startCol, endRow, endCol);
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }
    
    public void sendChatMessage(String message) {
        if (gameSessionStub != null) {
            try {
                gameSessionStub.sendChatMessage(gameFrame.getPlayerId(), message);
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void sendForfeit() {
        if (gameSessionStub != null) {
            try {
                gameSessionStub.sendForfeit(gameFrame.getPlayerId());
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void sendEndChainJump() {
        if (gameSessionStub != null) {
            try {
                gameSessionStub.sendEndChainJump(gameFrame.getPlayerId());
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void sendGetValidMoves(int row, int col) {
        if (gameSessionStub != null) {
            try {
                gameSessionStub.sendGetValidMoves(gameFrame.getPlayerId(), row, col);
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }
   
    //Tratamento de Erros e Reconexão
    private void handleRemoteException(RemoteException e) {
        if (gameIsOver) return;
        System.err.println("Erro de RMI: " + e.getMessage());
        gameFrame.updateStatus("Conexão perdida com o servidor.");
        // A lógica de reconexão simplificada.
        attemptReconnection();
    }
    
    private void attemptReconnection() {
        // Lógica de reconexão
        if (gameIsOver) return;
        
        // Zera os stubs
        this.gameSessionStub = null;
        this.serverStub = null;
        
        // Tenta reconectar em uma nova thread
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Espera 5 segundos
                SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Tentando reconectar..."));
                connect(); // Tenta o processo de conexão novamente
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Ponto de entrada do cliente
    public static void main(String[] args) {
         SwingUtilities.invokeLater(HalmaClientRMI::new);
    }
}