package br.com.servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import br.com.model.Usuario;
import br.com.service.AuthService;

public class ClienteHandler implements Runnable{
	
	private Socket socket;
	private AuthService authService;
	private PrintWriter out;
	private BufferedReader in;
	private Usuario usuario;
	
	//LISTA DE CLIENTES CONEDTADOS (COMPARTILHADO)
	private static List<ClienteHandler> clientes = new ArrayList<>();
	
	public ClienteHandler(Socket socket, AuthService authService) {
		this.socket = socket;
		this.authService = authService;
	}
	
	@Override
	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			
			autenticar();
			
			clientes.add(this);
			
			broadcast("🟢 " + usuario.getNome() + " entrou no chat!");
			
			String mensagem;
			
			while ((mensagem = in.readLine()) != null) {
				
				if(mensagem.equalsIgnoreCase("/sair")) {
					break;
				}
				
				broadcast("[" + usuario.getNome() + "]: " + mensagem);
			}
			
		} catch (Exception e) {
			System.out.println("Erro com cliente: " + e.getMessage());
		} finally {
			desconectar();
		}
	}
	
    // =========================
    // AUTENTICAÇÃO
    // =========================
	private void autenticar() throws Exception {
		
		while(usuario == null) {
			out.println("1 - Login");
			out.println("2 - Cadastro");
			
			String opcao = in.readLine();
			
			if(opcao.equals("1")) {
				
				out.println("Username: ");
				String username = in.readLine();
				
				out.println("Senha: ");
				String senha = in.readLine();
				
				Usuario u = authService.realizarLogin(username, senha);
				
				if(u != null) {
					usuario = u;
					out.println("✅ Login Realizado!");
				} else {
					out.println("❌ Login Inválido!");
				}
			
			} else if(opcao.equals("2")) {
				
				out.println("Nome: ");
				String nome = in.readLine();
				
				out.println("Username: ");
				String username = in.readLine();
				
				out.println("Senha: ");
				String senha = in.readLine();
				
				boolean sucesso = authService.cadastrar(nome, username, senha);
				
				if(sucesso) {
					out.println("✅ Cadastro realizado! Faça login.");
				} else {
					out.println("❌ Username já existe");
				}
			}
		}
	}
	
    // =========================
    // BROADCAST (CHAT)
    // =========================
	private void broadcast(String mensagem) {
		
		for(ClienteHandler cliente : clientes) {
			cliente.out.println(mensagem);
		}
	}
	
    // =========================
    // DESCONECTAR
    // =========================
	private void desconectar() {
		try {
			clientes.remove(this);
			
			if(usuario != null) {
				broadcast("🔴 " + usuario.getNome() + "Saiu do chat!");
			} 
			
			socket.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
