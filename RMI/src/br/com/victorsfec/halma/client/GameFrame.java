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

// GameFrame herda de JFrame, representando a janela principal do jogo.
public class GameFrame extends JFrame {
    private final HalmaClientRMI client; // Referência ao cliente de Halma para enviar mensagens.
    private final BoardPanel boardPanel;  // O painel onde o tabuleiro do jogo é desenhado.
    private final JTextArea chatArea;  // A área de texto para exibir o histórico do chat.
    private final JTextField chatInput; // O campo de texto para o usuário digitar mensagens de chat.
    private final JLabel statusLabel; // O rótulo para exibir o status atual do jogo.
    private final Board board; // A instância local do tabuleiro, usada para desenhar o estado atual.
    private int selectedRow = -1;  // Coordenadas da peça selecionada pelo jogador.
    private int selectedCol = -1;
    private int playerId; // O ID do jogador recebido do servidor.
    private boolean myTurn = false;
    private String playerName = "Jogador";
    private String opponentName = "Oponente";

     // Rótulos para exibir o placar de movimentos.
    private JLabel player1ScoreLabel;
    private JLabel player2ScoreLabel;

     // Lista para armazenar e exibir os movimentos válidos para a peça selecionada.
    private List<Point> validMoves = new ArrayList<>();

    public String getPlayerName() {
        return this.playerName;
    }

    public GameFrame(HalmaClientRMI client) {
        this.client = client; // Armazena a referência ao cliente.
        this.board = new Board();
        setTitle("Halma Game"); // Define o título da janela.

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Impede que a janela feche automaticamente ao clicar no "X".

        // Adiciona um WindowListener para controlar o evento de fechamento.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Mostra uma caixa de diálogo de confirmação, similar ao botão de desistir.
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

        // Define o layout principal da janela como BorderLayout.
        setLayout(new BorderLayout(10, 10));

        // Cria e adiciona o painel do tabuleiro ao centro da janela.
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        // Cria o painel direito para status, placar e chat.
        JPanel eastPanel = new JPanel();

        // Define o layout do painel direito para organizar componentes verticalmente.
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));

        // Adiciona uma borda vazia para espaçamento.
        eastPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

        // Cria o rótulo de status com uma mensagem inicial.
        statusLabel = new JLabel("Conecte a um servidor para iniciar.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Alinha o rótulo ao centro do painel.
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        eastPanel.add(statusLabel);

         // Adiciona um espaçamento vertical rígido.
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Cria o painel do placar.
        JPanel scorePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Placar de Movimentos"));
        
        // Cria os rótulos do placar com valores iniciais.
        player1ScoreLabel = new JLabel(playerName + ": 0");
        player2ScoreLabel = new JLabel(opponentName + ": 0");
        player1ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        player2ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Adiciona os rótulos ao painel do placar.
        scorePanel.add(player1ScoreLabel);
        scorePanel.add(player2ScoreLabel);
        eastPanel.add(scorePanel);
        
        // Adiciona mais um espaçamento vertical.
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Cria a área de texto para o chat.
        chatArea = new JTextArea(15, 25);
        chatArea.setEditable(false);

        // Cria um painel de rolagem para a área de chat.
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        eastPanel.add(chatScrollPane);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Cria o painel para a entrada de chat
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendButton = new JButton("Enviar");
        
        sendButton.addActionListener(this::sendChat); // Adiciona um ActionListener ao botão para chamar o método sendChat.
        chatInput.addActionListener(this::sendChat);  // Adiciona um ActionListener ao campo de texto para enviar ao pressionar Enter.
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        eastPanel.add(chatInputPanel);

        add(eastPanel, BorderLayout.EAST); // Adiciona o painel direito (eastPanel) à direita da janela.

        // Cria o painel inferior para o botão de desistir.
        JPanel bottomPanel = new JPanel();

        JButton forfeitButton = new JButton("Desistir do jogo");
        // Adiciona um ActionListener ao botão de desistir.
        forfeitButton.addActionListener(action -> {
            // Mostra a caixa de diálogo de confirmação
            int choice = JOptionPane.showConfirmDialog(this, "Você tem certeza que deseja desistir?", "Desistência", JOptionPane.YES_NO_OPTION);
             // Se o usuário confirmar, envia o comando de desistência.
            if (choice == JOptionPane.YES_OPTION) {
                client.sendForfeit();
            }
        });
        bottomPanel.add(forfeitButton);
        // Adiciona o painel inferior ao sul da janela.
        add(bottomPanel, BorderLayout.SOUTH);

        pack(); // Ajusta o tamanho da janela para caber todos os componentes.
        setLocationRelativeTo(null); // Centraliza a janela na tela.
    }

    public void updateScores(int p1Moves, int p2Moves) {
        // Atualiza os rótulos de acordo com o ID do jogador para exibir "Você" e "Oponente".
        if (playerId == 1) {
            player1ScoreLabel.setText("Você (" + playerName + "): " + p1Moves);
            player2ScoreLabel.setText("Oponente (" + opponentName + "): " + p2Moves);
        } else {
            player1ScoreLabel.setText("Oponente (" + opponentName + "): " + p1Moves);
            player2ScoreLabel.setText("Você (" + playerName + "): " + p2Moves);
        }
    }

     // Método para armazenar e redesenhar o tabuleiro com os movimentos válidos destacados.
    public void showValidMoves(List<Point> moves) {
        this.validMoves = moves;
        boardPanel.repaint();
    }

    // Método para definir o nome do jogador e atualizar o título da janela.
    public void setPlayerName(String name) { 
        this.playerName = name;
        setTitle("Halma Game - " + this.playerName);
    }
    
    // Método para definir o nome do oponente e atualizar o placar inicial.
    public void setOpponentName(String name) { 
        this.opponentName = name;
        updateScores(0, 0); 
    }

    // Método para definir se é o turno do jogador e atualizar o status.
    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        updateStatus(myTurn ? "Seu turno, " + playerName + "." : "Turno de " + opponentName + ".");
        if (!myTurn) {
            validMoves.clear();
            boardPanel.repaint();
        }
    }
    
    // Método para lidar com a oferta de salto em cadeia.
    public void updateBoardAfterJumpAndPrompt(int endRow, int endCol) {

        // Exibe uma caixa de diálogo perguntando se o jogador deseja continuar saltando.
        int choice = JOptionPane.showConfirmDialog( this, "Outro pulo está disponível. Você deseja continuar pulando?", "Sequência de pulos", JOptionPane.YES_NO_OPTION );
        // Se o jogador escolher "Sim", mantém a peça selecionada na nova posição.
        if (choice == JOptionPane.YES_OPTION) {
            this.selectedRow = endRow;
            this.selectedCol = endCol;
            
            // Solicita os novos movimentos válidos a partir da nova posição.
            client.sendGetValidMoves(endRow, endCol); 
            updateStatus("Seu turno: Continue pulando com a peça selecionada.");
        } else {
            // Se o jogador escolher "Não", envia o comando para encerrar a cadeia de saltos.
            client.sendEndChainJump();
            this.selectedRow = -1;
            this.selectedCol = -1;
            validMoves.clear();
        }
        boardPanel.repaint(); // Redesenha o tabuleiro para refletir o estado atual.
    }

    //Define o ID do jogador.
    public void setPlayerId(int id) {
        this.playerId = id;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    //Atualizar a GUI.
    public void updateStatus(String text) { statusLabel.setText(text); }
    public void addChatMessage(String message) { chatArea.append(message + "\n"); }

    //Método que atualiza o tabuleiro após um movimento e limpa a seleção.
    public void updateBoard(int startRow, int startCol, int endRow, int endCol) {
        board.performMove(startRow, startCol, endRow, endCol);
        this.selectedRow = -1;
        this.selectedCol = -1;
        validMoves.clear(); 
        boardPanel.repaint();
    }

    // Método para atualizar o tabuleiro após um salto, mas mantendo a peça selecionada.
    public void updateBoardAndKeepSelection(int startRow, int startCol, int endRow, int endCol) {
        board.performMove(startRow, startCol, endRow, endCol);
        this.selectedRow = endRow;
        this.selectedCol = endCol;
        validMoves.clear(); 
        boardPanel.repaint();
    }

    // Método chamado pelos ActionListeners do chat.
    private void sendChat(ActionEvent e) {
        // Obtém a mensagem do campo de texto e remove espaços em branco extras.
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendChatMessage(message);
             // Limpa o campo de texto.
            chatInput.setText("");
        }
    }

    public void closeApplication() { dispose(); }

    // Classe que representa o painel do tabuleiro e lida com o desenho e os cliques do mouse.
    private class BoardPanel extends JPanel {
        private static final int MARGIN = 30;

        BoardPanel() {
            setPreferredSize(new Dimension(660, 660)); // Define o tamanho preferencial do painel.
            addMouseListener(new MouseAdapter() {
                // Adiciona um listener para eventos de mouse.
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myTurn) return; // Ignora o clique se não for o turno do jogador.

                    int boardWidth = getWidth() - MARGIN * 2;
                    int boardHeight = getHeight() - MARGIN * 2;
                    int cellWidth = boardWidth / Board.SIZE;
                    int cellHeight = boardHeight / Board.SIZE;

                    // Converte as coordenadas do clique do mouse para coordenadas do tabuleiro.
                    int mouseX = e.getX() - MARGIN;
                    int mouseY = e.getY() - MARGIN;

                    // Ignora o clique se for fora da área do tabuleiro.
                    if (mouseX < 0 || mouseX >= boardWidth || mouseY < 0 || mouseY >= boardHeight) {
                        return;
                    }

                    // Calcula a linha e a coluna clicadas.
                    int col = mouseX / cellWidth;
                    int row = mouseY / cellHeight;
                    
                    // Obtém a peça (se houver) na posição clicada.
                    Piece clickedPiece = board.getPieceAt(row, col);

                    if (selectedRow == -1) { 
                        // Se nenhuma peça estiver selecionada e o jogador clicou em uma de suas próprias peças, seleciona a peça
                        if (clickedPiece != null && clickedPiece.getPlayerId() == playerId) {
                            selectedRow = row;
                            selectedCol = col;
                             // Solicita os movimentos válidos para esta peça ao servidor.
                            client.sendGetValidMoves(row, col); 
                        }
                    } else { // Se uma peça já estiver selecionada verifica se o clique foi em um alvo válido.
                        boolean isValidTarget = validMoves.stream().anyMatch(p -> p.x == row && p.y == col);
                        if (isValidTarget) {
                            //Se for um alvo válido, envia o movimento para o servidor.
                            client.sendMove(selectedRow, selectedCol, row, col);
                        } else {
                            // Se não for um alvo válido, limpa a seleção.
                            selectedRow = -1;
                            selectedCol = -1;
                            validMoves.clear();
                        }
                    }
                    repaint(); // Redesenha o painel para refletir a mudança de estado (seleção/movimento).
                }
            });
        }
        
        //Indica o sombreado da área de início dos jogadores.
        private boolean isGoalZone(int row, int col) {
            // Área inicial do Jogador 1
            if ((row == 0 && col <= 4) ||
                (row == 1 && col <= 3) ||
                (row == 2 && col <= 2) ||
                (row == 3 && col <= 1) ||
                (row == 4 && col == 0)) {
                return true;
            }
        
            // Área inicial do Jogador 2
            final int SIZE = Board.SIZE;
            if ((row == SIZE - 1 && col >= SIZE - 5) ||
                (row == SIZE - 2 && col >= SIZE - 4) ||
                (row == SIZE - 3 && col >= SIZE - 3) ||
                (row == SIZE - 4 && col >= SIZE - 2) ||
                (row == SIZE - 5 && col >= SIZE - 1)) {
                return true;
            }
            
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g; // Converte o objeto Graphics para Graphics2D.
            // Habilita o anti-aliasing para desenhos mais suaves.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

             // Obtém as dimensões do painel
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            // Calcula as dimensões do tabuleiro dentro da margem.
            int boardWidth = panelWidth - MARGIN * 2;
            int boardHeight = panelHeight - MARGIN * 2;

            // Calcula o tamanho de cada célula.
            int cellWidth = boardWidth / Board.SIZE;
            int cellHeight = boardHeight / Board.SIZE;

            // Define a fonte e a cor para as coordenadas.
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(Color.BLACK); // Alterado de WHITE para BLACK
            
            // Desenha as letras das colunas (A-J).
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

            // Define um gradiente para o fundo do tabuleiro.
            g2d.setPaint(new GradientPaint(MARGIN, MARGIN, new Color(160, 82, 45), 
                                          MARGIN + boardWidth, MARGIN + boardHeight, new Color(139, 69, 19)));
            g2d.fillRect(MARGIN, MARGIN, boardWidth, boardHeight); // Desenha o retângulo do fundo do tabuleiro.

             // Itera sobre cada célula do tabuleiro para desenhar as peças.
            for (int row = 0; row < Board.SIZE; row++) {
                for (int col = 0; col < Board.SIZE; col++) {
                    
                    // Calcula as coordenadas x, y da célula.
                    int x = MARGIN + col * cellWidth;
                    int y = MARGIN + row * cellHeight;
                    
                    // ALTERAÇÃO: Desenha o sombreamento para as zonas de objetivo
                    if (isGoalZone(row, col)) {
                        g2d.setColor(new Color(0, 0, 0, 40)); // Preto com 40 de opacidade
                        g2d.fillRect(x, y, cellWidth, cellHeight);
                    }

                    // Calcula a margem e o diâmetro da peça dentro da célula.
                    int margin_piece = cellWidth / 10;
                    int diameter = cellWidth - (2 * margin_piece);
                    
                    // Desenha uma sombra sutil para cada casa do tabuleiro.
                    Point2D center = new Point2D.Float(x + cellWidth / 2f, y + cellHeight / 2f);
                    float radius = diameter / 2f;
                    g2d.setPaint(new RadialGradientPaint(center, radius, new float[]{0.0f, 1.0f}, new Color[]{new Color(0,0,0,0), new Color(0,0,0,60)}));
                    g2d.fillOval(x + margin_piece, y + margin_piece, diameter, diameter);

                    // Obtém a peça na célula atual.
                    Piece piece = board.getPieceAt(row, col);
                    if (piece != null) {
                        //Determina as cores com base no jogador.
                        boolean isPlayer1 = piece.getPlayerId() == 1; //
                        Color primaryColor = isPlayer1 ? Color.WHITE : Color.BLACK;
                        Color secondaryColor = isPlayer1 ? Color.LIGHT_GRAY : new Color(50, 50, 50);
                        
                        // Cria um gradiente para dar um efeito 3D à peça.
                        Point2D gradientCenter = new Point2D.Float(x + margin_piece + diameter / 3f, y + margin_piece + diameter / 3f);
                        g2d.setPaint(new RadialGradientPaint(gradientCenter, radius, new float[]{0.0f, 1.0f}, new Color[]{primaryColor, secondaryColor}));
                        g2d.fillOval(x + margin_piece, y + margin_piece, diameter, diameter);  // Desenha a peça.

                        // Adiciona um pequeno brilho à peça.
                        g2d.setColor(new Color(255, 255, 255, 100));
                        g2d.fillOval(x + margin_piece + diameter / 4, y + margin_piece + diameter / 4, diameter / 3, diameter / 3);
                    }
                }
            }

            // Se uma peça estiver selecionada, desenha um retângulo vermelho ao redor dela.
            if (selectedRow != -1 && selectedCol != -1) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(MARGIN + selectedCol * cellWidth + 2, MARGIN + selectedRow * cellHeight + 2, cellWidth - 4, cellHeight - 4);
            }

            // Define a cor e o traço para destacar os movimentos válidos.
            g2d.setColor(new Color(0, 255, 0, 150));
            g2d.setStroke(new BasicStroke(3));
            for (Point move : validMoves) {
                // Para cada movimento, desenha uma elipse verde na casa de destino.
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