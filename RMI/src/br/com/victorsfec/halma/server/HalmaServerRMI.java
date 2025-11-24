package br.com.victorsfec.halma.server;

import br.com.victorsfec.halma.common.IServerOperations;
import javax.swing.*;
import java.awt.Font;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class HalmaServerRMI {

    public static void main(String[] args) {
        // Reutiliza a lógica de UI do original
        Object portStr = JOptionPane.showInputDialog(null, "Digite a porta RMI:", "Configuração do Servidor", JOptionPane.QUESTION_MESSAGE, null, null, "1099");

        if (portStr == null) System.exit(0);

        try {
            int port = Integer.parseInt(portStr.toString());
            if (port <= 0 || port > 65535) throw new NumberFormatException();

            // 1. Inicia o Registro RMI
            LocateRegistry.createRegistry(port);
            
            // 2. Cria a implementação do serviço de matchmaking
            IServerOperations serverOps = new ServerOperationsImpl();

            // 3. Registra o serviço
            String url = "//localhost:" + port + "/HalmaServer";
            Naming.rebind(url, serverOps);
            
            // Mostra a UI de status
            SwingUtilities.invokeLater(() -> createAndShowGUI(port, url));
            System.out.println("Servidor RMI do Halma pronto em: " + url);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Porta inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao iniciar servidor RMI:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // UI reutilizada do HalmaServer original
    private static void createAndShowGUI(int port, String url) {
        JFrame frame = new JFrame("Status do Servidor Halma (RMI)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 200);
        JLabel statusLabel = new JLabel("<html>Servidor RMI online.<br>URL: " + url + "</html>", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(statusLabel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}