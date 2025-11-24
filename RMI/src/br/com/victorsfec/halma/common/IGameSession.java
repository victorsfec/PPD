package br.com.victorsfec.halma.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

 //Interface RMI para uma sessão de jogo ativa.
 //O cliente recebe um stub desta interface do servidor.
 //Substitui os comandos de Cliente para Servidor do Protocol.java.
public interface IGameSession extends Remote {
    // Substitui Protocol.MOVE
    void sendMove(int playerId, int startRow, int startCol, int endRow, int endCol) throws RemoteException;

    // Substitui Protocol.CHAT
    void sendChatMessage(int playerId, String message) throws RemoteException;

    // Substitui Protocol.FORFEIT
    void sendForfeit(int playerId) throws RemoteException;

    // Substitui Protocol.END_CHAIN_JUMP
    void sendEndChainJump(int playerId) throws RemoteException;

    // Substitui Protocol.GET_VALID_MOVES
    void sendGetValidMoves(int playerId, int row, int col) throws RemoteException;
}