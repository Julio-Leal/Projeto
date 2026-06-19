package br.com.servidor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import br.com.service.AuthService;

public class ServidorGUI extends JFrame {
    private JTextField txtPorta;
    private JButton btnIniciar;
    private JTextArea txtLog;
    private JTable tblUsuarios;
    private DefaultTableModel modelUsuarios;
    private ServerSocket servidor;
    private AuthService authService;
    private boolean executando = false;

    public ServidorGUI() {
        authService = new AuthService();
        setTitle("Servidor de Chat - EP3");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Painel Superior: Configuração da Porta
        JPanel pnlSuperior = new JPanel();
        pnlSuperior.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        pnlSuperior.add(new JLabel("Porta:"));
        
        txtPorta = new JTextField();
        txtPorta.setText("12345");
        txtPorta.setColumns(5);
        pnlSuperior.add(txtPorta);
        
        btnIniciar = new JButton("Iniciar Servidor");
        pnlSuperior.add(btnIniciar);
        getContentPane().add(pnlSuperior, BorderLayout.NORTH);

        // Painel Central: Log e Lista de Usuários
        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        JScrollPane scpLog = new JScrollPane(txtLog);
        splitPane.setLeftComponent(scpLog);

        String[] colunas = {"Usuário", "IP", "Porta"};
        modelUsuarios = new DefaultTableModel(colunas, 0);
        tblUsuarios = new JTable(modelUsuarios);
        JScrollPane scpTabela = new JScrollPane(tblUsuarios);
        splitPane.setRightComponent(scpTabela);

        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Ação do Botão Iniciar
        btnIniciar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!executando) {
                    iniciarServidor();
                }
            }
        });
    }

    private void iniciarServidor() {
        try {
            int porta = Integer.parseInt(txtPorta.getText());
            if (porta <= 1024 || porta > 65535) {
                JOptionPane.showMessageDialog(this, "Porta inválida (1025-65534)");
                return;
            }

            servidor = new ServerSocket(porta);
            executando = true;
            btnIniciar.setEnabled(false);
            txtPorta.setEditable(false);
            log("[Servidor]: Iniciado na porta " + porta);

            new Thread(() -> {
                while (executando) {
                    try {
                        Socket socket = servidor.accept();
                        log("[Conexão]: Cliente " + socket.getInetAddress() + " conectado.");
                        ClienteHandler handler = new ClienteHandler(socket, authService, this);
                        new Thread(handler).start();
                    } catch (IOException ex) {
                        if (executando) log("[Erro]: " + ex.getMessage());
                    }
                }
            }).start();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta deve ser um número.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao iniciar servidor: " + ex.getMessage());
        }
    }

    public void log(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(mensagem + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    public void atualizarUsuariosLogados(List<String[]> usuarios) {
        SwingUtilities.invokeLater(() -> {
            modelUsuarios.setRowCount(0);
            for (String[] u : usuarios) {
                modelUsuarios.addRow(u);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServidorGUI().setVisible(true);
        });
    }
}
