package br.com.victorsfec.halma.server;

import br.com.victorsfec.halma.common.IClientCallback;
import br.com.victorsfec.halma.common.IGameSession;
import br.com.victorsfec.halma.game.Board; // Lógica do tabuleiro reutilizada
import java.awt.Point;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

 // Esta classe substitui GameSession e ClientHandler.
 // Ela implementa a interface do jogo (IGameSession) para o cliente chamar.
 // Ela armazena os stubs de callback (IClientCallback) para chamar o cliente.
 // Ela implementa Runnable para ser iniciada em uma thread.
public class GameSessionRMI extends UnicastRemoteObject implements IGameSession, Runnable {

    // Stubs para "chamar de volta" os clientes
    private final IClientCallback player1;
    private final IClientCallback player2;
    private final Board board;
    private int currentPlayer;

    // Campos de estado reutilizados de GameSession
    private int player1MoveCount = 0;
    private int player2MoveCount = 0;
    private int player1InvalidAttempts = 0;
    private int player2InvalidAttempts = 0;
    private final List<String> chatHistory = new ArrayList<>();
    private String winnerInfo = "O jogo encerrou inesperadamente.";
    private boolean gameEnded = false;
    private boolean isChainJumpActive = false;
    private int chainJumpRow;
    private int chainJumpCol;
    private String player1Name;
    private String player2Name;

    public GameSessionRMI(WaitingPlayer p1, WaitingPlayer p2) throws RemoteException {
        super();
        this.player1 = p1.callback;
        this.player2 = p2.callback;
        this.player1Name = p1.name;
        this.player2Name = p2.name;
        this.board = new Board(); // Reutiliza a lógica do tabuleiro
        this.currentPlayer = 1;
    }

    @Override
    public void run() {
        try {
            // Lógica de verificação de nome (reutilizada)
            if (player1Name.equals(player2Name)) {
                player1Name += " (1)";
                player2Name += " (2)";
            }

            // Exporta este objeto (a sessão) e envia o stub aos clientes
            // O "this" é o stub da sessão de jogo que os clientes usarão
            player1.startGame(this, 1, player1Name, player2Name);
            player2.startGame(this, 2, player2Name, player1Name);

            updateTurn(); // Envia a primeira mensagem de turno
        } catch (RemoteException e) {
            System.err.println("Erro ao iniciar GameSessionRMI. Um jogador pode ter desconectado.");
            // Se falhar ao iniciar, ambos os jogadores são desconectados
            handleDisconnect(null); 
        }
    }
    
    // --- Implementação da Interface IGameSession (Ações do Cliente) ---
    // A lógica de 'processMessage' é dividida aqui

    @Override
    public synchronized void sendMove(int playerId, int startRow, int startCol, int endRow, int endCol) throws RemoteException {
        if (gameEnded) return;
        IClientCallback sender = (playerId == 1) ? player1 : player2;
        
        try {
            if (playerId != currentPlayer) {
                sender.receiveError("Não é o seu turno.");
                return;
            }
            // Chama a lógica de movimento (copiada de handleMove)
            handleMoveLogic(playerId, startRow, startCol, endRow, endCol);
            
        } catch (RemoteException e) {
            // Se a chamada RMI falhar (ex: sender.receiveError), o cliente caiu
            handleDisconnect(sender);
        }
    }

    @Override
    public synchronized void sendChatMessage(int playerId, String message) throws RemoteException {
        if (gameEnded) return;
        String senderName = (playerId == 1) ? player1Name : player2Name;
        String formattedMessage = senderName + ": " + message;
        
        chatHistory.add(formattedMessage);
        broadcastChat(formattedMessage); // Chama o método auxiliar refatorado
    }

    @Override
    public synchronized void sendForfeit(int playerId) throws RemoteException {
        if (gameEnded) return;
        System.out.println("SERVER: Recebido pedido de desistência do " + (playerId == 1 ? player1Name : player2Name));
        handleForfeit((playerId == 1) ? player1 : player2);
    }

    @Override
public synchronized void sendEndChainJump(int playerId) throws RemoteException {
    if (gameEnded) return;
    if (!isChainJumpActive || playerId != currentPlayer) return;

    isChainJumpActive = false;
    if (board.checkForWinner(currentPlayer)) {
        String winnerName = (currentPlayer == 1) ? player1Name : player2Name;
        winnerInfo = winnerName + " ganhou por chegar no destino!";
        endGame((playerId == 1) ? player1 : player2, (playerId == 1) ? player2 : player1, "VICTORY", "DEFEAT");
    } else {
        switchTurn();
    }
}

    @Override
    public synchronized void sendGetValidMoves(int playerId, int row, int col) throws RemoteException {
        if (gameEnded || playerId != currentPlayer) return;
        IClientCallback sender = (playerId == 1) ? player1 : player2;
        try {
            List<Point> moves = board.getValidMoves(row, col, isChainJumpActive);
            sender.receiveValidMovesList(moves);
        } catch (RemoteException e) {
            handleDisconnect(sender);
        }
    }

    // --- Métodos Auxiliares (Lógica de Jogo e Callbacks) ---
    // (A maioria é copiada de GameSession.java e adaptada para RMI)

    /**
     * Lida com a desconexão de um jogador.
     * @param disconnectedPlayer O callback do jogador que desconectou, ou null se for um erro geral.
     */
    public synchronized void handleDisconnect(IClientCallback disconnectedPlayer) {
        if (gameEnded) return;
        gameEnded = true;
        System.out.println("SERVER: Jogador desconectado.");
        
        IClientCallback winner = (disconnectedPlayer == player1) ? player2 : player1;
        IClientCallback loser = (disconnectedPlayer == player1) ? player1 : player2;

        if (winner == null || loser == null) {
            // Erro antes do jogo começar ou ambos caíram
            System.out.println("SERVER: Desconexão antes do jogo ou ambos caíram.");
            return;
        }

        try {
            winnerInfo = (winner == player1 ? player1Name : player2Name) + " ganhou por desconexão do oponente.";
            sendGameOverStats(); // Envia estatísticas primeiro
            winner.notifyOpponentForfeit("Seu oponente desconectou. Você ganhou!");
        } catch (RemoteException e) {
            // O vencedor também já caiu, não há o que fazer.
            System.err.println("O jogador vencedor também desconectou.");
        }
    }
    
    private void handleForfeit(IClientCallback forfeiter) {
        if (gameEnded) return;
        
        IClientCallback winner = (forfeiter == player1) ? player2 : player1;
        winnerInfo = (winner == player1 ? player1Name : player2Name) + " ganhou pela desistência do oponente.";
        
        endGame(winner, forfeiter, "OPPONENT_FORFEIT", "DEFEAT");
    }

    // Lógica principal de movimento, adaptada de handleMove
    private void handleMoveLogic(int senderId, int startRow, int startCol, int endRow, int endCol) throws RemoteException {
        IClientCallback sender = (senderId == 1) ? player1 : player2;
        IClientCallback opponent = (senderId == 1) ? player2 : player1;

        if (isChainJumpActive && (startRow != chainJumpRow || startCol != chainJumpCol)) {
            sender.receiveError("Você deve continuar pulando com a mesma peça.");
            return;
        }

        if (board.movePiece(startRow, startCol, endRow, endCol, currentPlayer, isChainJumpActive)) {
            if (senderId == 1) player1MoveCount++; else player2MoveCount++;
            broadcastScoreUpdate();

            boolean wasJump = Math.abs(startRow - endRow) > 1 || Math.abs(startCol - endCol) > 1;

            if (wasJump && board.canJumpFrom(endRow, endCol)) {
                isChainJumpActive = true;
                chainJumpRow = endRow;
                chainJumpCol = endCol;
                sender.receiveJumpMove(startRow, startCol, endRow, endCol);
                opponent.receiveMove(startRow, startCol, endRow, endCol); // Oponente vê como um movimento normal
                sender.offerChainJump(endRow, endCol);
            } else {
                isChainJumpActive = false;
                sender.receiveMove(startRow, startCol, endRow, endCol);
                opponent.receiveMove(startRow, startCol, endRow, endCol);
                
                if (board.checkForWinner(currentPlayer)) {
                    String winnerName = (currentPlayer == 1) ? player1Name : player2Name;
                    winnerInfo = winnerName + " ganhou por chegar no destino!";
                    endGame(sender, opponent, "VICTORY", "DEFEAT");
                } else {
                    switchTurn();
                }
            }
        } else {
            if (senderId == 1) player1InvalidAttempts++; else player2InvalidAttempts++;
            sender.receiveError("Movimento inválido.");
        }
    }

    private void endGame(IClientCallback winner, IClientCallback loser, String winMessage, String loseMessage) {
        if (gameEnded) return;
        gameEnded = true;

        System.out.println("SERVER: A finalizar o jogo. Vencedor: " + (winner == player1 ? player1Name : player2Name));
        
        try {
            sendGameOverStats(); // Envia estatísticas primeiro
            
            if ("VICTORY".equals(winMessage)) {
                winner.notifyVictory("Parabéns, você ganhou!");
            } else if ("OPPONENT_FORFEIT".equals(winMessage)) {
                winner.notifyOpponentForfeit("Seu oponente desistiu. Você ganhou!");
            }
            
            if ("DEFEAT".equals(loseMessage)) {
                loser.notifyDefeat("Você perdeu a partida.");
            } else if ("FORFEIT".equals(loseMessage)) {
                 loser.notifyDefeat("Você desistiu da partida.");
            }

        } catch (RemoteException e) {
            System.err.println("Erro ao notificar fim de jogo. Um cliente pode ter caído.");
            // Não precisa chamar handleDisconnect aqui, o jogo já terminou.
        }
    }

    private void updateTurn() {
        try {
            player1.setTurn(currentPlayer == 1);
            player2.setTurn(currentPlayer == 2);
        } catch (RemoteException e) {
            handleDisconnect(null); // Desconexão séria se isso falhar
        }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }

    private void broadcastChat(String formattedMessage) {
        try {
            player1.receiveChatMessage(formattedMessage);
        } catch (RemoteException e) { handleDisconnect(player1); }
        
        try {
            player2.receiveChatMessage(formattedMessage);
        } catch (RemoteException e) { handleDisconnect(player2); }
    }

    private void broadcastScoreUpdate() {
        try {
            player1.updateScore(player1MoveCount, player2MoveCount);
        } catch (RemoteException e) { handleDisconnect(player1); }
        
        try {
            player2.updateScore(player1MoveCount, player2MoveCount);
        } catch (RemoteException e) { handleDisconnect(player2); }
    }

    private void sendGameOverStats() throws RemoteException {
        // Lógica de stats reutilizada
        String chatLog = String.join("|", chatHistory);
        StringJoiner stats = new StringJoiner(":"); // Usando ":" como no Protocolo original
        stats.add(winnerInfo)
             .add(String.valueOf(player1MoveCount))
             .add(String.valueOf(player1InvalidAttempts))
             .add(String.valueOf(player2MoveCount))
             .add(String.valueOf(player2InvalidAttempts))
             .add(chatLog);
        
        String statsData = stats.toString();
        
        // Este método só é chamado por endGame ou handleDisconnect,
        // então o try/catch será feito lá.
        player1.receiveGameOverStats(statsData);
        player2.receiveGameOverStats(statsData);
    }
}