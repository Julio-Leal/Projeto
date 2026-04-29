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

    // =========================
    // VALIDAÇÕES
    // =========================
    private boolean validarUsuario(String usuario) {
        return usuario != null &&
               usuario.matches("^[a-zA-Z0-9]{5,20}$"); // letras e números, 5-20 caracteres
    }

    private boolean validarSenha(String senha) {
        return senha != null &&
               senha.matches("^[0-9]{6}$"); // apenas números, exatamente 6 dígitos
    }

    // =========================
    // CADASTRAR USUÁRIO
    // =========================
    public synchronized boolean cadastrar(String nome, String usuario, String senha) {
        if (nome == null || usuario == null || senha == null) return false;
        if (!validarUsuario(usuario) || !validarSenha(senha)) return false;

        for (Usuario u : usuarios) {
            if (u.getUsuario().equals(usuario)) {
                return false; // já existe
            }
        }

        Usuario novo = new Usuario(nome, usuario, senha);
        usuarios.add(novo);
        repository.salvarUsuarios(usuarios);
        return true;
    }

    // =========================
    // ATUALIZAR USUÁRIO
    // =========================
    public synchronized boolean atualizarUsuario(String usuario, String novoNome, String novaSenha) {
        for (Usuario u : usuarios) {
            if (u.getUsuario().equals(usuario)) {
                if (novoNome != null && !novoNome.isEmpty()) {
                    u.setNome(novoNome);
                }
                if (novaSenha != null && validarSenha(novaSenha)) {
                    u.setSenha(novaSenha);
                } else {
                    return false; // senha inválida
                }
                repository.salvarUsuarios(usuarios);
                return true;
            }
        }
        return false;
    }

    // =========================
    // DELETAR USUÁRIO
    // =========================
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

    // =========================
    // LOGIN
    // =========================
    public Usuario realizarLogin(String usuario, String senha) {
        for (Usuario u : usuarios) {
            if (u.getUsuario().equals(usuario) && u.getSenha().equals(senha)) {
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
}
