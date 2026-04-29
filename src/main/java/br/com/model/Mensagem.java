package br.com.model;

public class Mensagem {
	private String op;
	private String nome;
	private String usuario;
	private String senha;
	private String token;
	private String destinatario;
	private String mensagem;
	private String resposta;
	
//	private String novoNome;
//	private String novoUsuario;
//	private String novaSenha;
	
	public Mensagem(String op, String nome, String usuario, String senha, String token, String destinatario,
					String mensagem, String resposta) {
		this.op = op;
		this.nome = nome;
		this.usuario = usuario;
		this.senha = senha;
		this.token = token;
		this.destinatario = destinatario;
		this.mensagem = mensagem;
		this.resposta = resposta;
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
	
	public String getMensagem() {
		return mensagem;
	}
	
	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}
	
	public String getResposta() {
		return resposta;
	}
	
	public void setResposta(String resposta) {
		this.resposta = resposta;
	}
	
//	public String getNovoNome() {
//		return novoNome;
//	}
	
//	public void setNovoNome(String novoNome) {
//		this.novoNome = novoNome;
//	}
	/* TURMA NÃO ESTÁ UTILIZANDO 🚩
	public String getNovousuario() {
		return novoUsuario;
	}
	
	public void setNovoUsuario(String novoUsuario) {
		this.novoUsuario = novoUsuario;
	}
	*/
//	public String getNovaSenha() {
//		return novaSenha;
//	}
	
//	public void setNovaSenha(String novaSenha) {
//		this.novaSenha = novaSenha;
//	}
}
