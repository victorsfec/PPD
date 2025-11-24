package br.com.victorsfec.halma.client;

import br.com.victorsfec.halma.common.IClientCallback;
import br.com.victorsfec.halma.common.IGameSession;
import java.awt.Point;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

 // Esta é a implementação do objeto de callback.
 // O RMI runtime chamará estes métodos em threads de RMI.
 // Usar SwingUtilities.invokeLater para atualizar a UI com segurança.
public class ClientCallbackImpl extends UnicastRemoteObject implements IClientCallback {
    
    private final GameFrame gameFrame;
    private final HalmaClientRMI client; // Referência para setar o gameSessionStub
    private String lastGameStats = null;

    public ClientCallbackImpl(GameFrame frame, HalmaClientRMI client) throws RemoteException {
        super();
        this.gameFrame = frame;
        this.client = client;
    }

    //Chamada de métodos do RMI runtime
    @Override
    public void startGame(IGameSession gameSession, int playerId, String newPlayerName, String opponentName) throws RemoteException {
        // Armazena o stub da sessão de jogo para futuras chamadas
        client.setGameSession(gameSession, newPlayerName); 
        
        SwingUtilities.invokeLater(() -> {
            gameFrame.setPlayerId(playerId);
            gameFrame.setOpponentName(opponentName);
            gameFrame.updateStatus("Oponente encontrado: " + opponentName + ". Iniciando partida...");
        });
    }

    @Override
    public void updateScore(int p1Moves, int p2Moves) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateScores(p1Moves, p2Moves));
    }

    @Override
    public void receiveMove(int startRow, int startCol, int endRow, int endCol) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateBoard(startRow, startCol, endRow, endCol));
    }

    @Override
    public void receiveJumpMove(int startRow, int startCol, int endRow, int endCol) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateBoardAndKeepSelection(startRow, startCol, endRow, endCol));
    }

    @Override
    public void offerChainJump(int row, int col) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateBoardAfterJumpAndPrompt(row, col));
    }

    @Override
    public void receiveValidMovesList(List<Point> moves) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.showValidMoves(moves));
    }

    @Override
    public void notifyVictory(String message) throws RemoteException {
        handleGameEnd(message, "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void notifyDefeat(String message) throws RemoteException {
        handleGameEnd(message, "Fim de jogo", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void notifyOpponentForfeit(String message) throws RemoteException {
        handleGameEnd(message, "Vitória", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void receiveGameOverStats(String stats) throws RemoteException {
        this.lastGameStats = stats;
        // O diálogo de stats é mostrado pelo handleGameEnd
    }

    @Override
    public void setTurn(boolean isMyTurn) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.setMyTurn(isMyTurn));
    }

    @Override
    public void receiveError(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(gameFrame, message, "Erro", JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void receiveInfo(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateStatus(message));
    }

    @Override
    public void receiveChatMessage(String formattedMessage) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.addChatMessage(formattedMessage));
    }
    
    // Lógica de UI de fim de jogo (reutilizada de ServerListener)
    private void handleGameEnd(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            client.setGameIsOver(); // Sinaliza o fim do jogo
            
            JOptionPane.showMessageDialog(gameFrame, message, title, messageType);
            
            if (lastGameStats != null) {
                // Reutiliza o ResultsDialog
                new ResultsDialog(gameFrame, lastGameStats).setVisible(true);
            }
            gameFrame.closeApplication();
        });
    }
}