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

	    for (Usuario u : usuarios) {

	        if (u.getUsuario() != null && u.getUsuario().equals(usuario)) {
	            return false;
	        }
	    }

	    Usuario novo = new Usuario(nome, usuario, senha);
	    usuarios.add(novo);

	    repository.salvarUsuarios(usuarios);

	    return true;
	}
	
	public synchronized boolean atualizarUsuario(String username, String novoNome, String novaSenha) {

	    for (Usuario u : usuarios) {
	        if (u.getUsuario().equals(username)) {

	            if (novoNome != null && !novoNome.isEmpty()) {
	                u.setNome(novoNome);
	            }

	            if (novaSenha != null && !novaSenha.isEmpty()) {
	                u.setSenha(novaSenha);
	            }

	            repository.salvarUsuarios(usuarios);
	            return true;
	        }
	    }

	    return false;
	}
	
	public synchronized boolean deletarUsuario(String username) {

	    Usuario remover = null;

	    for (Usuario u : usuarios) {
	        if (u.getUsuario().equals(username)) {
	            remover = u;
	            break;
	        }
	    }

	    if (remover != null) {
	        usuarios.remove(remover);
	        repository.salvarUsuarios(usuarios);
	        return true;
	    }

	    return false;
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
