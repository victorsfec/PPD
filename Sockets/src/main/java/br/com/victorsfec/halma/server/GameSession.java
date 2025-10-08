package br.com.victorsfec.halma.server;

import br.com.victorsfec.halma.game.Board;
import br.com.victorsfec.halma.shared.Protocol;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Board board;
    private int currentPlayer;

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

    private final String player1Name;
    private final String player2Name;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new Board();
        this.currentPlayer = 1;

        this.player1Name = player1.getPlayerName();
        this.player2Name = player2.getPlayerName();

        this.player1.setGameSession(this);
        this.player2.setGameSession(this);
    }

    @Override
    public void run() {
        player1.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "1");
        player2.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "2");
        player1.sendMessage(Protocol.OPPONENT_FOUND + Protocol.SEPARATOR + player2Name);
        player2.sendMessage(Protocol.OPPONENT_FOUND + Protocol.SEPARATOR + player1Name);
        player1.sendMessage(Protocol.GAME_START);
        player2.sendMessage(Protocol.GAME_START);
        updateTurn();
    }
    
    // --- NOVO MÉTODO PARA ENVIAR ATUALIZAÇÕES DE PLACAR ---
    private void broadcastScoreUpdate() {
        String message = Protocol.UPDATE_SCORE + Protocol.SEPARATOR + player1MoveCount + Protocol.SEPARATOR + player2MoveCount;
        player1.sendMessage(message);
        player2.sendMessage(message);
    }

    private void handleMove(String moveData, ClientHandler sender) {
        try {
            String[] coords = moveData.split(Protocol.SEPARATOR);
            int startRow = Integer.parseInt(coords[0]);
            int startCol = Integer.parseInt(coords[1]);
            int endRow = Integer.parseInt(coords[2]);
            int endCol = Integer.parseInt(coords[3]);
            int senderId = (sender == player1) ? 1 : 2;

            if (isChainJumpActive && (startRow != chainJumpRow || startCol != chainJumpCol)) {
                sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Você deve continuar pulando com a mesma peça.");
                return;
            }

            if (board.movePiece(startRow, startCol, endRow, endCol, currentPlayer, isChainJumpActive)) {
                if (senderId == 1) player1MoveCount++; else player2MoveCount++;
                
                // Envia a atualização do placar para ambos os jogadores
                broadcastScoreUpdate();

                boolean wasJump = Math.abs(startRow - endRow) > 1 || Math.abs(startCol - endCol) > 1;
                ClientHandler opponent = (sender == player1) ? player2 : player1;

                if (wasJump && board.canJumpFrom(endRow, endCol)) {
                    isChainJumpActive = true;
                    chainJumpRow = endRow;
                    chainJumpCol = endCol;
                    sender.sendMessage(Protocol.JUMP_MOVE + Protocol.SEPARATOR + moveData);
                    opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);
                    sender.sendMessage(Protocol.CHAIN_JUMP_OFFER + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol);
                } else {
                    isChainJumpActive = false;
                    sender.sendMessage(Protocol.VALID_MOVE + Protocol.SEPARATOR + moveData);
                    opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);

                    if (board.checkForWinner(currentPlayer)) {
                        String winnerName = (currentPlayer == 1) ? player1Name : player2Name;
                        winnerInfo = winnerName + " ganhou por chegar no destino!";
                        endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        switchTurn();
                    }
                }
            } else {
                if (senderId == 1) player1InvalidAttempts++; else player2InvalidAttempts++;
                sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Movimento inválido.");
            }
        } catch (Exception e) {
            sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Comando de movimento malformado.");
        }
    }
    
    // --- O RESTANTE DA CLASSE (run, processMessage, etc.) PERMANECE IGUAL ---
    // ...
    // (Cole o restante do código de GameSession.java aqui)
    // ...
    private void handleForfeit(ClientHandler forfeiter) {
        if (gameEnded) return;
        System.out.println("SERVER: Recebido pedido de desistência do " + forfeiter.getPlayerName());
        
        ClientHandler winner = (forfeiter == player1) ? player2 : player1;
        String winnerName = winner.getPlayerName();
        winnerInfo = winnerName + " ganhou pela desistência do oponente.";
        
        String loseMessage = Protocol.DEFEAT + Protocol.SEPARATOR + "Você desistiu da partida.";
        String winMessage = Protocol.OPPONENT_FORFEIT;
        
        endGame(winner, forfeiter, winMessage, loseMessage);
    }

    public synchronized void handleDisconnect(ClientHandler disconnectedPlayer) {
        if (gameEnded) return;
        System.out.println("SERVER: Jogador desconectado a meio do jogo.");
        
        ClientHandler winner = (disconnectedPlayer == player1) ? player2 : player1;
        endGame(winner, disconnectedPlayer, Protocol.OPPONENT_FORFEIT, "");
    }

    private void endGame(ClientHandler winner, ClientHandler loser, String winMessage, String loseMessage) {
        if (gameEnded) return;
        gameEnded = true;

        System.out.println("SERVER: A finalizar o jogo. Vencedor: " + winner.getPlayerName());

        sendGameOverStats();

        System.out.println("SERVER: Enviando mensagem de vitória para " + winner.getPlayerName() + ": " + winMessage);
        winner.sendMessage(winMessage);

        if (loseMessage != null && !loseMessage.isEmpty()) {
            System.out.println("SERVER: Enviando mensagem de derrota para " + loser.getPlayerName() + ": " + loseMessage);
            loser.sendMessage(loseMessage);
        }
    }

    public synchronized void processMessage(String message, ClientHandler sender) {
        if (gameEnded) return;

        String[] parts = message.split(Protocol.SEPARATOR, 2);
        String command = parts[0];
        int senderId = (sender == player1) ? 1 : 2;

        switch (command) {
            case Protocol.MOVE:
                if (senderId == currentPlayer) handleMove(parts[1], sender);
                else sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Não é o seu turno.");
                break;
            case Protocol.CHAT:
                broadcastChat(parts[1], senderId);
                break;
            case Protocol.FORFEIT:
                handleForfeit(sender);
                break;
            case Protocol.END_CHAIN_JUMP:
                if (isChainJumpActive && senderId == currentPlayer) {
                    isChainJumpActive = false;
                    if (board.checkForWinner(currentPlayer)) {
                        String winnerName = (currentPlayer == 1) ? player1Name : player2Name;
                        winnerInfo = winnerName + " ganhou por chegar no destino!";
                        endGame(sender, (sender == player1) ? player2 : player1, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        switchTurn();
                    }
                }
                break;
            case Protocol.GET_VALID_MOVES:
                if (senderId == currentPlayer) {
                    String[] coords = parts[1].split(Protocol.SEPARATOR);
                    int row = Integer.parseInt(coords[0]);
                    int col = Integer.parseInt(coords[1]);
                    List<Point> moves = board.getValidMoves(row, col, isChainJumpActive);
                    String movesStr = moves.stream().map(p -> p.x + "," + p.y).collect(Collectors.joining(";"));
                    sender.sendMessage(Protocol.VALID_MOVES_LIST + Protocol.SEPARATOR + movesStr);
                }
                break;
        }
    }
    
    private void updateTurn() {
        if (currentPlayer == 1) {
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        } else {
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }
    
    private void broadcastChat(String chatMessage, int senderId) {
        String senderName = (senderId == 1) ? player1Name : player2Name;
        String formattedMessage = Protocol.CHAT_MESSAGE + Protocol.SEPARATOR + senderName + ": " + chatMessage;
        player1.sendMessage(formattedMessage);
        player2.sendMessage(formattedMessage);
        chatHistory.add(senderName + ": " + chatMessage);
    }
    
    private void sendGameOverStats() {
        String chatLog = String.join("|", chatHistory);
        StringJoiner stats = new StringJoiner(Protocol.SEPARATOR);
        stats.add(winnerInfo)
             .add(String.valueOf(player1MoveCount))
             .add(String.valueOf(player1InvalidAttempts))
             .add(String.valueOf(player2MoveCount))
             .add(String.valueOf(player2InvalidAttempts))
             .add(chatLog);
        
        String message = Protocol.GAME_OVER_STATS + Protocol.SEPARATOR + stats.toString();
        player1.sendMessage(message);
        player2.sendMessage(message);
    }
}