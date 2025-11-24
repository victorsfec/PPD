package br.com.victorsfec.halma.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

 //Interface RMI para o servidor principal de matchmaking.
 //Substitui a lógica de conexão inicial do HalmaServer.
public interface IServerOperations extends Remote {
    /**
     * Cliente chama este método para entrar na fila de espera.
     * O servidor chamará de volta o callback quando um jogo for encontrado.
     * @param playerName O nome do jogador.
     * @param callback O stub do objeto de callback do cliente.
     */
    void connect(String playerName, IClientCallback callback) throws RemoteException;
}