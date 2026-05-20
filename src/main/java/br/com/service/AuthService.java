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

    private boolean validarUsuario(String usuario) {
        return usuario != null && usuario.matches("^[a-zA-Z0-9]{5,20}$"); 
    }

    private boolean validarSenha(String senha) {
        return senha != null && senha.matches("^[0-9]{6}$"); 
    }

    public synchronized boolean cadastrar(String nome, String usuario, String senha) {
        if (nome == null || usuario == null || senha == null) return false;
        if (!validarUsuario(usuario) || !validarSenha(senha)) return false;

        // Atualiza a lista em tempo real antes de checar duplicidade
        this.usuarios = repository.carregarUsuarios();
        for (Usuario u : usuarios) {
            if (u.getUsuario().equalsIgnoreCase(usuario)) {
                return false; 
            }
        }

        Usuario novo = new Usuario(nome, usuario, senha);
        usuarios.add(novo);
        repository.salvarUsuarios(usuarios);
        return true;
    }

    // Método aprimorado para permitir que o ADM atualize cadastros de terceiros
    public synchronized boolean atualizarUsuario(String usuarioAlvo, String novoNome, String novaSenha) {
        this.usuarios = repository.carregarUsuarios();
        for (Usuario u : usuarios) {
            if (u.getUsuario().equalsIgnoreCase(usuarioAlvo)) {
                if (novoNome != null && !novoNome.trim().isEmpty()) {
                    u.setNome(novoNome);
                }
                if (novaSenha != null) {
                    if (validarSenha(novaSenha)) {
                        u.setSenha(novaSenha);
                    } else {
                        return false; 
                    }
                }
                repository.salvarUsuarios(usuarios);
                return true;
            }
        }
        return false;
    }

    // Método aprimorado para permitir a remoção de usuários alvos
    public synchronized boolean deletarUsuario(String usuarioAlvo) {
        this.usuarios = repository.carregarUsuarios();
        Usuario remover = null;
        for (Usuario u : usuarios) {
            if (u.getUsuario().equalsIgnoreCase(usuarioAlvo)) {
                remover = u;
                break;
            }
        }
        if (remover != null) {
            // Regra implícita: não permitir deletar o próprio administrador padrão
            if (remover.getUsuario().equalsIgnoreCase("admin")) {
                return false;
            }
            usuarios.remove(remover);
            repository.salvarUsuarios(usuarios);
            return true;
        }
        return false;
    }

    public Usuario realizarLogin(String usuario, String senha) {
        this.usuarios = repository.carregarUsuarios();
        for (Usuario u : usuarios) {
            if (u.getUsuario().equalsIgnoreCase(usuario) && u.getSenha().equals(senha)) {
                if (u.getToken() == null) {
                    if (u.getUsuario().equalsIgnoreCase("admin")) {
                        u.setToken("adm"); 
                    } else {
                        u.setToken("usr_" + u.getUsuario()); 
                    }
                }
                return u;
            }
        }
        return null;
    }

    // Método auxiliar para buscar dados de um usuário pelo username (usado na Consulta do ADM)
    public Usuario buscarPorUsuario(String username) {
        this.usuarios = repository.carregarUsuarios();
        for (Usuario u : usuarios) {
            if (u.getUsuario().equalsIgnoreCase(username)) {
                return u;
            }
        }
        return null;
    }
    
    public List<Usuario> listarUsuarios() {
    	this.usuarios = repository.carregarUsuarios();
        return usuarios;
    }
}