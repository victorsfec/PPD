package br.com.victorsfec.halma.server;

import br.com.victorsfec.halma.game.Board;
import br.com.victorsfec.halma.shared.Protocol;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

// GameSession gerencia uma partida entre dois jogadores.
public class GameSession implements Runnable {
    private final ClientHandler player1;  // Referências para os handlers dos dois jogadores.
    private final ClientHandler player2;
    private final Board board; // Instância do tabuleiro para esta partida.
    private int currentPlayer; // Variável que armazena de quem é o turno

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

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new Board();
        this.currentPlayer = 1; // O jogador 1 sempre começa.

        this.player1.setGameSession(this);
        this.player2.setGameSession(this);
    }

    @Override
    public void run() {
        // Lógica para verificar e diferenciar nomes iguais
        if (player1.getPlayerName().equals(player2.getPlayerName())) {
            String originalName = player1.getPlayerName();
            player1.setPlayerName(originalName + " (1)");
            player2.setPlayerName(originalName + " (2)");
        }
        
        // Nomes da sessão são definidos aqui, após a verificação
        this.player1Name = player1.getPlayerName();
        this.player2Name = player2.getPlayerName();

        // Mensagem WELCOME agora envia o nome final do jogador
        player1.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "1" + Protocol.SEPARATOR + this.player1Name);
        player2.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "2" + Protocol.SEPARATOR + this.player2Name);
        
        player1.sendMessage(Protocol.OPPONENT_FOUND + Protocol.SEPARATOR + player2Name);
        player2.sendMessage(Protocol.OPPONENT_FOUND + Protocol.SEPARATOR + player1Name);
        player1.sendMessage(Protocol.GAME_START);
        player2.sendMessage(Protocol.GAME_START);
        updateTurn(); // Envia a primeira mensagem de turno.
    }

    // Método para enviar as atualizações de placar
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
    
            // Se um salto em cadeia está ativo, o jogador deve mover a mesma peça.
            if (isChainJumpActive && (startRow != chainJumpRow || startCol != chainJumpCol)) {
                sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Você deve continuar pulando com a mesma peça.");
                return;
            }

            // Tenta realizar o movimento no tabuleiro.
            if (board.movePiece(startRow, startCol, endRow, endCol, currentPlayer, isChainJumpActive)) {
                if (senderId == 1) player1MoveCount++; else player2MoveCount++;
                
                // Envia a atualização do placar para ambos os jogadores
                broadcastScoreUpdate();

                boolean wasJump = Math.abs(startRow - endRow) > 1 || Math.abs(startCol - endCol) > 1;
                ClientHandler opponent = (sender == player1) ? player2 : player1;

                // Se foi um salto e a peça pode saltar novamente da nova posição
                if (wasJump && board.canJumpFrom(endRow, endCol)) {
                    isChainJumpActive = true;
                    chainJumpRow = endRow;
                    chainJumpCol = endCol;
                    sender.sendMessage(Protocol.JUMP_MOVE + Protocol.SEPARATOR + moveData);
                    opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);
                    sender.sendMessage(Protocol.CHAIN_JUMP_OFFER + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol);
                } else {
                    // Se não foi um salto ou não há mais saltos possíveis, desativa o modo de salto em cadeia.
                    isChainJumpActive = false;
                    sender.sendMessage(Protocol.VALID_MOVE + Protocol.SEPARATOR + moveData);
                    opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);
                    
                    // Verifica se o jogador venceu a partida após o movimento.
                    if (board.checkForWinner(currentPlayer)) {
                        String winnerName = (currentPlayer == 1) ? player1Name : player2Name;
                        winnerInfo = winnerName + " ganhou por chegar no destino!";
                        endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        // Se ninguém venceu, passa o turno para o próximo jogador.
                        switchTurn();
                    }
                }
            } else {
                // Se o movimento foi inválido, incrementa o contador de tentativas inválidas.
                if (senderId == 1) player1InvalidAttempts++; else player2InvalidAttempts++;
                sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Movimento inválido.");
            }
        } catch (Exception e) {
            // Se ocorrer um erro ao processar os dados do movimento
            sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Comando de movimento malformado.");
        }
    }
    
    private void handleForfeit(ClientHandler forfeiter) {
        // Se o jogo já terminou, não faz nada.
        if (gameEnded) return;
        System.out.println("SERVER: Recebido pedido de desistência do " + forfeiter.getPlayerName());
        
        // Determina quem é o vencedor.
        ClientHandler winner = (forfeiter == player1) ? player2 : player1;
        String winnerName = winner.getPlayerName();
        winnerInfo = winnerName + " ganhou pela desistência do oponente.";
        
        // Prepara as mensagens de vitória e derrota.
        String loseMessage = Protocol.DEFEAT + Protocol.SEPARATOR + "Você desistiu da partida.";
        String winMessage = Protocol.OPPONENT_FORFEIT;
        
        // Encerra o jogo.
        endGame(winner, forfeiter, winMessage, loseMessage);
    }

    //Método para lidar com a desconexão de um jogador no meio do jogo.
    public synchronized void handleDisconnect(ClientHandler disconnectedPlayer) {
        if (gameEnded) return;
        System.out.println("SERVER: Jogador desconectado a meio do jogo.");
        
        // O jogador que permaneceu conectado é o vencedor.
        ClientHandler winner = (disconnectedPlayer == player1) ? player2 : player1;
        endGame(winner, disconnectedPlayer, Protocol.OPPONENT_FORFEIT, "");
    }


    private void endGame(ClientHandler winner, ClientHandler loser, String winMessage, String loseMessage) {
        if (gameEnded) return;
        gameEnded = true; // Define a flag de jogo terminado como true para evitar ações futuras.

        System.out.println("SERVER: A finalizar o jogo. Vencedor: " + winner.getPlayerName());

        // Envia as estatísticas finais para ambos os jogadores.
        sendGameOverStats();

        System.out.println("SERVER: Enviando mensagem de vitória para " + winner.getPlayerName() + ": " + winMessage);
        winner.sendMessage(winMessage); // Envia a mensagem de vitória para o vencedor.

        // Se houver uma mensagem de derrota a ser enviada.
        if (loseMessage != null && !loseMessage.isEmpty()) {
            System.out.println("SERVER: Enviando mensagem de derrota para " + loser.getPlayerName() + ": " + loseMessage);
            loser.sendMessage(loseMessage);
        }
    }

    // Processa uma mensagem recebida de um dos jogadores.
    public synchronized void processMessage(String message, ClientHandler sender) {
        if (gameEnded) return; // Se o jogo já terminou, ignora a mensagem.

        // Divide a mensagem em comando e dados.
        String[] parts = message.split(Protocol.SEPARATOR, 2);
        String command = parts[0];
        int senderId = (sender == player1) ? 1 : 2;

        // Tratar os diferentes comandos do protocolo.
        switch (command) {
            case Protocol.MOVE:
                // Se o comando for MOVE e for o turno do jogador atual, processa a jogada.
                if (senderId == currentPlayer) handleMove(parts[1], sender);
                else sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Não é o seu turno.");
                break;
            case Protocol.CHAT:
                // Se for uma mensagem de chat, encaminha para ambos os jogadores.
                broadcastChat(parts[1], senderId);
                break;
            case Protocol.FORFEIT:
                // Se um jogador desistir, encerra o jogo.
                handleForfeit(sender);
                break;
            case Protocol.END_CHAIN_JUMP:
                // Se um salto em cadeia estiver ativo e for o turno do jogador
                if (isChainJumpActive && senderId == currentPlayer) {
                    isChainJumpActive = false;

                     // Verifica novamente por uma condição de vitória
                    if (board.checkForWinner(currentPlayer)) {
                        String winnerName = (currentPlayer == 1) ? player1Name : player2Name;
                        winnerInfo = winnerName + " ganhou por chegar no destino!";
                        endGame(sender, (sender == player1) ? player2 : player1, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        switchTurn();
                    }
                }
                break;

                // Se os movimentos do jogador atual são válidos
            case Protocol.GET_VALID_MOVES:
                // Se for o turno do jogador
                if (senderId == currentPlayer) {
                    //Processa as coordenadas, obtém os movimentos válidos do tabuleiro
                    String[] coords = parts[1].split(Protocol.SEPARATOR);
                    int row = Integer.parseInt(coords[0]);
                    int col = Integer.parseInt(coords[1]);
                    
                    //Converte a lista de movimentos para uma string e a envia de volta ao cliente.
                    List<Point> moves = board.getValidMoves(row, col, isChainJumpActive);
                    String movesStr = moves.stream().map(p -> p.x + "," + p.y).collect(Collectors.joining(";"));
                    sender.sendMessage(Protocol.VALID_MOVES_LIST + Protocol.SEPARATOR + movesStr);
                }
                break;
        }
    }
    
    //Definir e comunicar o turno atual
    private void updateTurn() {
        if (currentPlayer == 1) {
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        } else {
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        }
    }

    //Alternar o turno
    private void switchTurn() {
        // Alterna o jogador atual de 1 para 2 ou de 2 para 1.
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }
    
    //Transmitir uma mensagem de chat a ambos os jogadores.
    private void broadcastChat(String chatMessage, int senderId) {
        String senderName = (senderId == 1) ? player1Name : player2Name;
        String formattedMessage = Protocol.CHAT_MESSAGE + Protocol.SEPARATOR + senderName + ": " + chatMessage;
        player1.sendMessage(formattedMessage);
        player2.sendMessage(formattedMessage);
        chatHistory.add(senderName + ": " + chatMessage);
    }
    
    //Compila e enviaa as estatísticas finais do jogo.
    private void sendGameOverStats() {

        // Junta o histórico do chat em uma única string, separada por '|'.
        String chatLog = String.join("|", chatHistory);
        
        //Construir a string de estatísticas
        StringJoiner stats = new StringJoiner(Protocol.SEPARATOR);
        
        // Adiciona todas as informações das estatísticas.
        stats.add(winnerInfo)
             .add(String.valueOf(player1MoveCount))
             .add(String.valueOf(player1InvalidAttempts))
             .add(String.valueOf(player2MoveCount))
             .add(String.valueOf(player2InvalidAttempts))
             .add(chatLog);
        
        // Monta a mensagem final e a envia para ambos os jogadores.
        String message = Protocol.GAME_OVER_STATS + Protocol.SEPARATOR + stats.toString();
        player1.sendMessage(message);
        player2.sendMessage(message);
    }
}