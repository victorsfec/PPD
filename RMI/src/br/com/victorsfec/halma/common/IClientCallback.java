package br.com.victorsfec.halma.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.awt.Point; // java.awt.Point é Serializável, pode ser passado por RMI

/**
 * Interface RMI para o cliente. O servidor chama estes métodos
 * para enviar atualizações ao cliente.
 * Substitui os comandos de Servidor para Cliente do Protocol.java
 * e a lógica de 'ServerListener'.
 */
public interface IClientCallback extends Remote {
    // Combina WELCOME, OPPONENT_FOUND e GAME_START
    void startGame(IGameSession gameSession, int playerId, String newPlayerName, String opponentName) throws RemoteException;

    // Substitui Protocol.UPDATE_SCORE
    void updateScore(int p1Moves, int p2Moves) throws RemoteException;

    // Substitui Protocol.VALID_MOVE e Protocol.OPPONENT_MOVED
    void receiveMove(int startRow, int startCol, int endRow, int endCol) throws RemoteException;

    // Substitui Protocol.JUMP_MOVE
    void receiveJumpMove(int startRow, int startCol, int endRow, int endCol) throws RemoteException;

    // Substitui Protocol.CHAIN_JUMP_OFFER
    void offerChainJump(int row, int col) throws RemoteException;

    // Substitui Protocol.VALID_MOVES_LIST
    void receiveValidMovesList(List<Point> moves) throws RemoteException;

    // Substitui Protocol.VICTORY
    void notifyVictory(String message) throws RemoteException;

    // Substitui Protocol.DEFEAT
    void notifyDefeat(String message) throws RemoteException;

    // Substitui Protocol.OPPONENT_FORFEIT
    void notifyOpponentForfeit(String message) throws RemoteException;

    // Substitui Protocol.GAME_OVER_STATS
    void receiveGameOverStats(String stats) throws RemoteException;

    // Substitui Protocol.SET_TURN
    void setTurn(boolean isMyTurn) throws RemoteException;

    // Substitui Protocol.ERROR
    void receiveError(String message) throws RemoteException;

    // Substitui Protocol.INFO
    void receiveInfo(String message) throws RemoteException;

    // Substitui Protocol.CHAT_MESSAGE
    void receiveChatMessage(String formattedMessage) throws RemoteException;
}