package br.com.victorsfec.halma.game;

/**
 * Cada peça tem o playerId o qual pertence.
 */
public class Piece {
    // Declaração de uma variável privada para armazenar o ID do jogador dono da peça.
    private final int playerId;

    public Piece(int playerId) {
        this.playerId = playerId; // Atribui o ID do jogador recebido à variável de instância.
    }

    public int getPlayerId() {
        return playerId;
    }
}
