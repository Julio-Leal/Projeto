package br.com.servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;

import br.com.model.Mensagem;
import br.com.model.Usuario;
import br.com.service.AuthService;

public class ClienteHandler implements Runnable{
	
	private Socket socket;
	private AuthService authService;
	private PrintWriter out;
	private BufferedReader in;
	private Usuario usuario;
	private String token;
	
	//LISTA DE CLIENTES CONECTADOS (THREAD SAFE)
	private static List<ClienteHandler> clientes = Collections.synchronizedList(new ArrayList<>());
	
	public ClienteHandler(Socket socket, AuthService authService) {
		this.socket = socket;
		this.authService = authService;
	}
	
	@Override
	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			
			Gson gson = new Gson();
			
			String json;
			
			while((json = in.readLine()) != null) {
				
				Mensagem msg = gson.fromJson(json, Mensagem.class);
				
				processarMensagem(msg, gson);
			}
		
		} catch(Exception e) {
			System.out.println("Erro com cloiente: " + e.getMessage());
		} finally {
			desconectar();
		}
	}

    // =========================
    // PROCESSADOR PRINCIPAL
    // =========================
	private void processarMensagem(Mensagem msg, Gson gson) {
		
		if(msg.getOp() == null)
			return;
		
		switch(msg.getOp()) {
			case "login":
				tratarLogin(msg, gson);
				break;
			case "cadastro":
				tratarCadastro(msg, gson);
				break;
			case "enviarMensagem": 
				tratarMensagem(msg, gson);
				break;
			default: 
				System.out.println("Operação desconhecida: " + msg.getOp());
		}
	}
	
    // =========================
    // LOGIN
    // =========================
	private void tratarLogin(Mensagem msg, Gson gson) {
		
		Usuario u = authService.realizarLogin(msg.getUsuario(), msg.getSenha());
		
		Mensagem resposta = new Mensagem();
		
		if(u != null) {
			this.usuario = u;
			
			token = "usr_" + u.getUsuario();
			
			clientes.add(this);
			
			resposta.setResposta("200");
			resposta.setToken(token);
			
		} else {
			resposta.setResposta("401");
		}
		
		out.println(gson.toJson(resposta));
	} 
	
    // =========================
    // CADASTRO
    // =========================
	private void tratarCadastro(Mensagem msg, Gson gson) {
		
		boolean sucesso  = authService.cadastrar(
				msg.getNome(),
				msg.getUsuario(),
				msg.getSenha()
		);
		
		Mensagem resposta = new Mensagem();
		
		if(sucesso) {
			resposta.setResposta("200");
		} else {
			resposta.setResposta("400");
		}
		
		out.println(gson.toJson(resposta));
	}
	

    // =========================
    // CHAT 1 PARA 1
    // =========================
	private void tratarMensagem(Mensagem msg, Gson gson) {
		
		//VALIDA TOKEN
		if(token == null || !token.equals(msg.getToken())) {
			return;
		}
		
		boolean enviado = false;
		
		synchronized (clientes) {
			for(ClienteHandler cliente : clientes) {
				if(cliente.usuario != null && cliente.usuario.getUsuario().equals(msg.getDestinatario())) {
					Mensagem resposta = new Mensagem();
					resposta.setMensagem("[" + usuario.getUsuario() + "]" + msg.getMensagem());
					
					cliente.out.println(gson.toJson(resposta));
					
					enviado = true;
					break;
				}
			}
		}
		
		//FEEDBACK CASO USUÁRIO NÃO SEJA ENCONTRADO
		if(!enviado) {
			Mensagem erro = new Mensagem();
			erro.setMensagem("❌ Usuário não encontrado");
			
			out.println(gson.toJson(erro));
		}
	}
	
    // =========================
    // DESCONECTAR
    // =========================
	private void desconectar() {
		try {
			clientes.remove(this);
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
