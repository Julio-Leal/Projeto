package br.com.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import br.com.service.AuthService;

public class ServidorMain {

    private static Scanner input = new Scanner(System.in);
    private static int portaPadrao;

    public static void main(String[] args) {
        AuthService authService = new AuthService();

        try {
            definirPorta();

            ServerSocket servidor = new ServerSocket(portaPadrao);
            System.out.println("[Servidor]: Executando na porta [" + portaPadrao + "]."); 

            while (true) {
                System.out.println("[Servidor]: Aguardando cliente...");

                Socket clienteSocket = servidor.accept();
                System.out.println("[Conexão]: Cliente [" + clienteSocket.getInetAddress() + "] conectado.");

                ClienteHandler handler = new ClienteHandler(clienteSocket, authService);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("[Erro de I/O]: " + e.getMessage());
        }
    }

    private static void definirPorta() {
        int tempPorta = 0;
        do {
            System.out.printf("[Servidor]: Informe o número da porta: "); 
            try {
                tempPorta = input.nextInt();
                if (tempPorta > 1024 && tempPorta < 65535) {
                    portaPadrao = tempPorta;
                } else {
                    System.out.println("[Servidor]: Porta inválida (Escolha entre 1025 e 65534)!");
                    tempPorta = 0;
                }
            } catch (Exception e) {
                System.out.println("[Servidor]: Entrada inválida!");
                input.next(); // Limpa buffer
                tempPorta = 0;
            }
        } while (tempPorta == 0);
    }
}