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
	
	public synchronized boolean cadastrar(String nome, String usuario, String senha) {
		
		//VERIFICA SE JÁ EXISTE
		for(Usuario u : usuarios) {
			if(u.getUsuario().equals(usuario))
				return false;
		}
		
		Usuario novo = new Usuario(nome, usuario, senha);
		usuarios.add(novo);
		
		repository.salvarUsuarios(usuarios);
		
		return true;
	}
	
	public Usuario realizarLogin(String usuario, String senha) {
		for(Usuario u : usuarios) {
			if(u.getUsuario().equals(usuario) && u.getSenha().equals(senha)) {
				return u;
			}
		}
		
		return null;
	}
}
