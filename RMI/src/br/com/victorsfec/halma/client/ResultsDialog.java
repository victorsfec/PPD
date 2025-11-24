package br.com.victorsfec.halma.client;

import javax.swing.*;
import java.awt.*;

// A classe ResultsDialog herda de JDialog
public class ResultsDialog extends JDialog {

    // Construtor que recebe a janela e a string com os dados das estatísticas.
    public ResultsDialog(Frame owner, String statsData) {
        // Chama o construtor da superclasse, definindo o dono, o título e se a caixa de diálogo é modal
        super(owner, "Tela de resultados da partida", true);

        // Converte os dados brutos da partida, dividindo a string pelo separador do protocolo.
        String[] parts = statsData.split(":", 6);
        String winnerInfo = parts[0];
        String p1Moves = parts[1];
        String p1Invalid = parts[2];
        String p2Moves = parts[3];
        String p2Invalid = parts[4];
        String chatLog;

        // Verifica se há um histórico de chat e o formata, substituindo '|' por quebras de linha.
        if (parts.length > 5 && !parts[5].isEmpty()) {
            chatLog = parts[5].replace("|", "\n");
        } else {
            // Se não houver histórico, define uma mensagem padrão.
            chatLog = "Sem histórico de conversas.";
        }

        // Cria o painel principal com BorderLayout.
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        // Adiciona uma borda para espaçamento interno.
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Rótulo com as informações do vencedor, centralizado e com fonte em negrito.
        JLabel winnerLabel = new JLabel(winnerInfo, SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        // Adiciona o rótulo do vencedor na parte superior do painel principal.
        mainPanel.add(winnerLabel, BorderLayout.NORTH);

        // Cria o painel de estatísticas com GridLayout.
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        // Adiciona os rótulos com as estatísticas de movimentos e tentativas inválidas.
        statsPanel.add(new JLabel("Movimentos do jogador 1: " + p1Moves));
        statsPanel.add(new JLabel("Movimentos do jogador 2: " + p2Moves));
        statsPanel.add(new JLabel("Tentativas inválidas do jogador 1: " + p1Invalid));
        statsPanel.add(new JLabel("Tentativas inválidas do jogador 2: " + p2Invalid));
        // Adiciona o painel de estatísticas no centro do painel principal.
        mainPanel.add(statsPanel, BorderLayout.CENTER);

        // Cria a área de texto para o histórico de mensagens.
        JTextArea chatArea = new JTextArea(10, 30);
        chatArea.setText(chatLog);
        chatArea.setEditable(false); // Impede a edição do histórico.
        chatArea.setLineWrap(true); // Habilita a quebra de linha automática.
        JScrollPane chatScrollPane = new JScrollPane(chatArea);  // Coloca a área de texto dentro de um painel de rolagem.
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Histórico de mensagens")); // Adiciona um título ao painel de rolagem.
        // Adiciona o painel de chat na parte inferior do painel principal.
        mainPanel.add(chatScrollPane, BorderLayout.SOUTH);

        JButton closeButton = new JButton("OK");
        closeButton.addActionListener(actionEvent -> dispose());

        // Cria um painel para o botão, para que ele fique centralizado.
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        // Adiciona o painel principal e o painel do botão ao conteúdo da caixa de diálogo.
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

         // Ajusta o tamanho da caixa de diálogo para caber todos os seus componentes.
        pack();
        setLocationRelativeTo(owner);
    }
}
