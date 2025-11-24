package br.com.victorsfec.halma.server;

import br.com.victorsfec.halma.common.IClientCallback;
import br.com.victorsfec.halma.common.IServerOperations;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

// Classe auxiliar para armazenar dados do jogador na fila
class WaitingPlayer {
    public String name;
    public IClientCallback callback;
    public WaitingPlayer(String n, IClientCallback c) { name = n; callback = c; }
}

public class ServerOperationsImpl extends UnicastRemoteObject implements IServerOperations {
    
    // Lista sincronizada para a fila de espera (mesma lógica do HalmaServer)
    private static final List<WaitingPlayer> waitingClients = new ArrayList<>();

    public ServerOperationsImpl() throws RemoteException {
        super();
    }

    @Override
    public void connect(String playerName, IClientCallback callback) throws RemoteException {
        try {
            WaitingPlayer newPlayer = new WaitingPlayer(playerName, callback);
            
            // Lógica de matchmaking reutilizada de HalmaServer
            synchronized (waitingClients) {
                waitingClients.add(newPlayer);
                System.out.println("Jogador conectado: " + playerName + ". Esperando oponente.");
                callback.receiveInfo("Conectado. Aguardando oponente...");

                if (waitingClients.size() >= 2) {
                    WaitingPlayer player1 = waitingClients.remove(0);
                    WaitingPlayer player2 = waitingClients.remove(0);
                    System.out.println("Pareando jogadores: " + player1.name + " e " + player2.name);

                    // Cria e inicia a sessão de jogo RMI
                    GameSessionRMI gameSession = new GameSessionRMI(player1, player2);
                    // Inicia a sessão de jogo em uma nova thread para não bloquear o RMI
                    new Thread(gameSession).start();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao conectar jogador: " + e.getMessage());
            // Tenta notificar o cliente do erro
            try {
                callback.receiveError("Erro do servidor ao processar sua conexão.");
            } catch (RemoteException re) {
                // Cliente já caiu
            }
        }
    }
}