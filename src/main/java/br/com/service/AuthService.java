package br.com.service;

import java.util.List;

import br.com.model.Usuario;
import br.com.repository.UsuarioRepository;

public class AuthService {
	private UsuarioRepository repository;
	private List<Usuario> usuarios;
	
	public AuthService() {
		this.repository = new UsuarioRepository();
		this.usuarios = repository.carregarUsuarios();
	}
	
	public synchronized boolean cadastrar(String nome, String userName, String senha) {
		
		//VERIFICA SE JÁ EXISTE
		for(Usuario u : usuarios) {
			if(u.getUsername().equals(userName))
				return false;
		}
		
		Usuario novo = new Usuario(nome, userName, senha);
		usuarios.add(novo);
		
		repository.salvarUsuarios(usuarios);
		
		return true;
	}
	
	public Usuario realizarLogin(String username, String senha) {
		for(Usuario u : usuarios) {
			if(u.getUsername().equals(username) && u.getSenha().equals(senha)) {
				return u;
			}
		}
		
		return null;
	}
}
