package br.com.model;

public class Usuario {
	private String nome;
	private String usuario;
	private String senha;
	
	public Usuario(String nome, String usuario, String senha) {
		this.nome = nome;
		this.usuario = usuario;
		this.senha = senha;
	}

	public String getNome() {
		return nome;
	}

	public String getUsuario() {
		return usuario;
	}

	public String getSenha() {
		return senha;
	}
}
