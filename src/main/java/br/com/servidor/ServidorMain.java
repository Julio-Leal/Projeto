package br.com.servidor;

import java.net.ServerSocket;
import java.net.Socket;

import br.com.service.AuthService;

public class ServidorMain {
	
	private static final int PORTA_PADRAO = 5000; //PORTA PADRÃO
	
	public static void main(String[] args) {
		
		System.out.println("▶️ Servidor iniciando...");
		
		AuthService authService = new AuthService();
		
		try(ServerSocket servidor = new ServerSocket(PORTA_PADRAO)) {
			
			System.out.println("✅ Servidor rodando na porta " + PORTA_PADRAO);
			
			while(true) {
				System.out.println("⌛ Aguardando cliente...");
				
				Socket clienteSocket = servidor.accept();
				
				System.out.println("🔌 Cliente conectado: " + clienteSocket.getInetAddress());
				
				ClienteHandler handler = new ClienteHandler(clienteSocket, authService);
				
				new Thread(handler).start();
			}
		} catch (Exception e) {
			System.out.println("❌ Erro no servidor:");
			e.printStackTrace();
		}
	}
}
