package br.com.model;

public class Usuario {
	private String nome;
	private String username;
	private String senha;
	
	public Usuario(String nome, String username, String senha) {
		this.nome = nome;
		this.username = username;
		this.senha = senha;
	}

	public String getNome() {
		return nome;
	}

	public String getUsername() {
		return username;
	}

	public String getSenha() {
		return senha;
	}
}
