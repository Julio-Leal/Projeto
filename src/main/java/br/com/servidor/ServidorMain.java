package br.com.servidor;

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
			System.out.println("[servidor]: Executando na porta [" + portaPadrao + "].");
			
			while(true) {
				System.out.println("[servidor]: Aguardando cliente...");
				
				Socket clienteSocket = servidor.accept();
				
				System.out.println("[servidor]: Cliente [" + clienteSocket.getInetAddress() + "] conectado.");
				
				ClienteHandler handler = new ClienteHandler(clienteSocket, authService);
				
				new Thread(handler).start();
			}
		} catch (Exception e) {
			System.out.println("[servidor]: Erro!");
			e.printStackTrace();
		}
	}

	private static void definirPorta() {
		int tempPorta = 0;
		
		do {
			System.out.println("[servidor]: Informe o número da porta: ");
			tempPorta = input.nextInt();
			
			if(tempPorta > 1024 && tempPorta < 49000)
				portaPadrao = tempPorta;
			else {
				System.out.println("[servidor]: A porta informada já está em uso!");
				tempPorta = 0;
			}
		} while(tempPorta == 0);
	}
}
