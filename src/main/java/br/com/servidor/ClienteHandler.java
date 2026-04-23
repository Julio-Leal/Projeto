package br.com.servidor;

import java.net.Socket;

import br.com.service.AuthService;

public class ClienteHandler implements Runnable{
	private Socket socket;
	private AuthService authService;
	
	public ClienteHandler(Socket socket, AuthService authService) {
		this.socket = socket;
		this.authService = authService;
	}
	
	@Override
	public void run() {
		//aqui acontece login + chat
	}
}
