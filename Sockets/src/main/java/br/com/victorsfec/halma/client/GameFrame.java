package br.com.victorsfec.halma.client;

import br.com.victorsfec.halma.game.Board;
import br.com.victorsfec.halma.game.Piece;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class GameFrame extends JFrame {
    // O código desta parte da classe permanece o mesmo...
    private final HalmaClient client;
    private final BoardPanel boardPanel;
    private final JTextArea chatArea;
    private final JTextField chatInput;
    private final JLabel statusLabel;
    private final Board board;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int playerId;
    private boolean myTurn = false;
    private String playerName = "Jogador";
    private String opponentName = "Oponente";

    private JLabel player1ScoreLabel;
    private JLabel player2ScoreLabel;

    private List<Point> validMoves = new ArrayList<>();

    public String getPlayerName() {
        return this.playerName;
    }

    public GameFrame(HalmaClient client) {
        this.client = client;
        this.board = new Board();
        setTitle("Halma Game");

        // --- INÍCIO DA MODIFICAÇÃO ---
        // A linha original "setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);" foi substituída por este bloco.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Permite que nosso código controle o fechamento
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Mostra a mesma caixa de diálogo do botão de desistir para consistência.
                int choice = JOptionPane.showConfirmDialog(GameFrame.this, "Você tem certeza que deseja desistir?", "Desistência", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    client.sendForfeit(); // Envia a mensagem de desistência
                    // Aguarda um instante para garantir o envio da mensagem
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // Fecha a aplicação
                    System.exit(0);
                }
            }
        });
        // --- FIM DA MODIFICAÇÃO ---

        setLayout(new BorderLayout(10, 10));

        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
        eastPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

        statusLabel = new JLabel("Conecte a um servidor para iniciar.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        eastPanel.add(statusLabel);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel scorePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Placar de Movimentos"));
        player1ScoreLabel = new JLabel(playerName + ": 0");
        player2ScoreLabel = new JLabel(opponentName + ": 0");
        player1ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        player2ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        scorePanel.add(player1ScoreLabel);
        scorePanel.add(player2ScoreLabel);
        eastPanel.add(scorePanel);
        
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        chatArea = new JTextArea(15, 25);
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        eastPanel.add(chatScrollPane);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(this::sendChat);
        chatInput.addActionListener(this::sendChat);
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        eastPanel.add(chatInputPanel);

        add(eastPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        JButton forfeitButton = new JButton("Desistir do jogo");
        forfeitButton.addActionListener(action -> {
            int choice = JOptionPane.showConfirmDialog(this, "Você tem certeza que deseja desistir?", "Desistência", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                client.sendForfeit();
            }
        });
        bottomPanel.add(forfeitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public void updateScores(int p1Moves, int p2Moves) {
        if (playerId == 1) {
            player1ScoreLabel.setText("Você (" + playerName + "): " + p1Moves);
            player2ScoreLabel.setText("Oponente (" + opponentName + "): " + p2Moves);
        } else {
            player1ScoreLabel.setText("Oponente (" + opponentName + "): " + p1Moves);
            player2ScoreLabel.setText("Você (" + playerName + "): " + p2Moves);
        }
    }

    public void showValidMoves(List<Point> moves) {
        this.validMoves = moves;
        boardPanel.repaint();
    }

    public void setPlayerName(String name) { 
        this.playerName = name;
        setTitle("Halma Game - " + this.playerName);
    }
    
    public void setOpponentName(String name) { 
        this.opponentName = name;
        updateScores(0, 0); 
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        updateStatus(myTurn ? "Seu turno, " + playerName + "." : "Turno de " + opponentName + ".");
        if (!myTurn) {
            validMoves.clear();
            boardPanel.repaint();
        }
    }
    
    public void updateBoardAfterJumpAndPrompt(int endRow, int endCol) {
        int choice = JOptionPane.showConfirmDialog( this, "Outro pulo está disponível. Você deseja continuar pulando?", "Sequência de pulos", JOptionPane.YES_NO_OPTION );
        if (choice == JOptionPane.YES_OPTION) {
            this.selectedRow = endRow;
            this.selectedCol = endCol;
            client.sendGetValidMoves(endRow, endCol); 
            updateStatus("Seu turno: Continue pulando com a peça selecionada.");
        } else {
            client.sendEndChainJump();
            this.selectedRow = -1;
            this.selectedCol = -1;
            validMoves.clear();
        }
        boardPanel.repaint();
    }

    public void setPlayerId(int id) {
        this.playerId = id;
    }

    public void updateStatus(String text) { statusLabel.setText(text); }
    public void addChatMessage(String message) { chatArea.append(message + "\n"); }

    public void updateBoard(int startRow, int startCol, int endRow, int endCol) {
        board.performMove(startRow, startCol, endRow, endCol);
        this.selectedRow = -1;
        this.selectedCol = -1;
        validMoves.clear(); 
        boardPanel.repaint();
    }

    public void updateBoardAndKeepSelection(int startRow, int startCol, int endRow, int endCol) {
        board.performMove(startRow, startCol, endRow, endCol);
        this.selectedRow = endRow;
        this.selectedCol = endCol;
        validMoves.clear(); 
        boardPanel.repaint();
    }

    private void sendChat(ActionEvent e) {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendChatMessage(message);
            chatInput.setText("");
        }
    }

    public void closeApplication() { dispose(); }

    private class BoardPanel extends JPanel {
        private static final int MARGIN = 30;

        BoardPanel() {
            setPreferredSize(new Dimension(660, 660));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myTurn) return;

                    int boardWidth = getWidth() - MARGIN * 2;
                    int boardHeight = getHeight() - MARGIN * 2;
                    int cellWidth = boardWidth / Board.SIZE;
                    int cellHeight = boardHeight / Board.SIZE;

                    int mouseX = e.getX() - MARGIN;
                    int mouseY = e.getY() - MARGIN;

                    if (mouseX < 0 || mouseX >= boardWidth || mouseY < 0 || mouseY >= boardHeight) {
                        return;
                    }

                    int col = mouseX / cellWidth;
                    int row = mouseY / cellHeight;
                    
                    Piece clickedPiece = board.getPieceAt(row, col);

                    if (selectedRow == -1) { 
                        if (clickedPiece != null && clickedPiece.getPlayerId() == playerId) {
                            selectedRow = row;
                            selectedCol = col;
                            client.sendGetValidMoves(row, col); 
                        }
                    } else { 
                        boolean isValidTarget = validMoves.stream().anyMatch(p -> p.x == row && p.y == col);
                        if (isValidTarget) {
                            client.sendMove(selectedRow, selectedCol, row, col);
                        } else {
                            selectedRow = -1;
                            selectedCol = -1;
                            validMoves.clear();
                        }
                    }
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int boardWidth = panelWidth - MARGIN * 2;
            int boardHeight = panelHeight - MARGIN * 2;
            int cellWidth = boardWidth / Board.SIZE;
            int cellHeight = boardHeight / Board.SIZE;

            // --- CORREÇÃO APLICADA AQUI ---
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(Color.BLACK); // Alterado de WHITE para BLACK
            
            String[] cols = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
            for (int i = 0; i < Board.SIZE; i++) {
                String letter = cols[i];
                FontMetrics fm = g2d.getFontMetrics();
                int letterWidth = fm.stringWidth(letter);
                int x = MARGIN + i * cellWidth + (cellWidth - letterWidth) / 2;
                g2d.drawString(letter, x, MARGIN - 10);

                String number = String.valueOf(i + 1);
                int numberWidth = fm.stringWidth(number);
                int y = MARGIN + i * cellHeight + (cellHeight + fm.getAscent() / 2) / 2;
                g2d.drawString(number, MARGIN - 15 - numberWidth, y);
            }

            g2d.setPaint(new GradientPaint(MARGIN, MARGIN, new Color(160, 82, 45), 
                                          MARGIN + boardWidth, MARGIN + boardHeight, new Color(139, 69, 19)));
            g2d.fillRect(MARGIN, MARGIN, boardWidth, boardHeight);

            for (int row = 0; row < Board.SIZE; row++) {
                for (int col = 0; col < Board.SIZE; col++) {
                    int x = MARGIN + col * cellWidth;
                    int y = MARGIN + row * cellHeight;
                    int margin_piece = cellWidth / 10;
                    int diameter = cellWidth - (2 * margin_piece);
                    
                    Point2D center = new Point2D.Float(x + cellWidth / 2f, y + cellHeight / 2f);
                    float radius = diameter / 2f;
                    g2d.setPaint(new RadialGradientPaint(center, radius, new float[]{0.0f, 1.0f}, new Color[]{new Color(0,0,0,0), new Color(0,0,0,60)}));
                    g2d.fillOval(x + margin_piece, y + margin_piece, diameter, diameter);

                    Piece piece = board.getPieceAt(row, col);
                    if (piece != null) {
                        boolean isPlayer1 = piece.getPlayerId() == 1; //
                        Color primaryColor = isPlayer1 ? Color.WHITE : Color.BLACK;
                        Color secondaryColor = isPlayer1 ? Color.LIGHT_GRAY : new Color(50, 50, 50);
                        
                        Point2D gradientCenter = new Point2D.Float(x + margin_piece + diameter / 3f, y + margin_piece + diameter / 3f);
                        g2d.setPaint(new RadialGradientPaint(gradientCenter, radius, new float[]{0.0f, 1.0f}, new Color[]{primaryColor, secondaryColor}));
                        g2d.fillOval(x + margin_piece, y + margin_piece, diameter, diameter);

                        g2d.setColor(new Color(255, 255, 255, 100));
                        g2d.fillOval(x + margin_piece + diameter / 4, y + margin_piece + diameter / 4, diameter / 3, diameter / 3);
                    }
                }
            }

            if (selectedRow != -1 && selectedCol != -1) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(MARGIN + selectedCol * cellWidth + 2, MARGIN + selectedRow * cellHeight + 2, cellWidth - 4, cellHeight - 4);
            }

            g2d.setColor(new Color(0, 255, 0, 150));
            g2d.setStroke(new BasicStroke(3));
            for (Point move : validMoves) {
                int moveRow = move.x;
                int moveCol = move.y;
                int x = MARGIN + moveCol * cellWidth;
                int y = MARGIN + moveRow * cellHeight;
                int margin_piece = cellWidth / 10;
                int diameter = cellWidth - (2 * margin_piece);
                g2d.drawOval(x + margin_piece, y + margin_piece, diameter, diameter);
            }
        }
    }
}