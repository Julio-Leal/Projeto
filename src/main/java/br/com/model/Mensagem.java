package br.com.model;

import java.util.List;

public class Mensagem {
    private String op;
    private String nome;
    private String usuario;
    private String senha;
    private String token;
    private String destinatario;
    private String resposta;
    private String mensagem;
    private List<Usuario> lista_usuarios;

    public Mensagem(String op, String nome, String usuario, String senha, String token, String destinatario,
                    String mensagem, String resposta) {
        this.op = op;
        this.nome = nome;
        this.usuario = usuario;
        this.senha = senha;
        this.token = token;
        this.destinatario = destinatario;
        this.resposta = resposta;
        this.mensagem = mensagem;
    }

    public Mensagem() {
    }

    public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getDestinatario() {
		return destinatario;
	}

	public void setDestinatario(String destinatario) {
		this.destinatario = destinatario;
	}
	
	public String getResposta() {
		return resposta;
	}

	public void setResposta(String resposta) {
		this.resposta = resposta;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

	public List<Usuario> getLista_usuarios() {
		return lista_usuarios;
	}

	public void setLista_usuarios(List<Usuario> lista_usuarios) {
		this.lista_usuarios = lista_usuarios;
	}

	@Override
    public String toString() {
        return "[Mensagem]: [op=" + op + ", nome=" + nome + ", usuario=" + usuario + ", senha=" + senha
                + ", token=" + token + ", destinatario=" + destinatario
                + ", resposta=" + resposta + ", mensagem=" + mensagem + "]";
    }
}
