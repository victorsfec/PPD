package br.com.victorsfec.halma.shared;

public class Protocol {
    // Define o caractere que separará os comandos de seus argumentos.
    public static final String SEPARATOR = ":";
    
    // Comandos do Cliente para o Servidor
    public static final String MOVE = "MOVE"; // Envio de uma jogada.
    public static final String CHAT = "CHAT"; // Envia uma mensagem de chat.
    public static final String FORFEIT = "FORFEIT"; // Informa a desistência da partida.
    public static final String END_CHAIN_JUMP = "END_CHAIN_JUMP"; // Informa que o jogador não quer continuar um salto em cadeia.
    public static final String SET_NAME = "SET_NAME"; // Define o nome do jogador no servidor.
    public static final String GET_VALID_MOVES = "GET_VALID_MOVES"; // Solicita os movimentos válidos para uma peça.

    // Comandos do Servidor para o Cliente
    public static final String GAME_OVER_STATS = "GAME_OVER_STATS"; // Envia as estatísticas finais do jogo.
    public static final String WELCOME = "WELCOME"; // Mensagem de boas-vindas, atribuindo o ID do jogador.
    public static final String GAME_START = "GAME_START";  // Informa que a partida vai começar.
    public static final String OPPONENT_FOUND = "OPPONENT_FOUND"; // Informa o nome do oponente encontrado.
    public static final String VALID_MOVE = "VALID_MOVE"; // Confirma que um movimento foi válido.
    public static final String JUMP_MOVE = "JUMP_MOVE"; // Confirma um movimento de salto.
    public static final String OPPONENT_MOVED = "OPPONENT_MOVED"; // Informa sobre o movimento do oponente.
    public static final String SET_TURN = "SET_TURN"; // Define de quem é o turno de jogar, YOUR_TURN ou OPPONENT_TURN.
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE"; // Encaminha uma mensagem de chat recebida.
    public static final String VICTORY = "VICTORY"; // Informa o cliente que ele venceu.
    public static final String DEFEAT = "DEFEAT"; // Informa o cliente que ele perdeu.
    public static final String OPPONENT_FORFEIT = "OPPONENT_FORFEIT"; // Informa que o oponente desistiu.
    public static final String CHAIN_JUMP_OFFER = "CHAIN_JUMP_OFFER"; // Oferece a opção de continuar um salto em cadeia.
    public static final String INFO = "INFO"; // Envia uma mensagem informativa geral para o status do cliente.
    public static final String ERROR = "ERROR"; // Envia uma mensagem de erro.
    public static final String VALID_MOVES_LIST = "VALID_MOVES_LIST"; // Envia a lista de movimentos válidos solicitada.
    public static final String UPDATE_SCORE = "UPDATE_SCORE"; // Envia o placar atualizado de movimentos.
}