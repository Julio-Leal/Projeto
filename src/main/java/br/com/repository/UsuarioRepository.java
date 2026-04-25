package br.com.repository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.model.Usuario;

public class UsuarioRepository {
	
	private static final String CAMINHO_ARQUIVO = "usuarios.json";
	
	private Gson gson;
	
	public UsuarioRepository() {
		this.gson = new Gson();
		criarArquivoSeNaoExistir();
	}
	
	//================================
	// CRIAR ARQUIVO SE NÃO EXISTIR
	//================================
	private void criarArquivoSeNaoExistir() {
		try {
			File file = new File(CAMINHO_ARQUIVO);
			
			if(!file.exists()) {
				file.createNewFile();
				
				//INICIALIZA COM LISTA VAZIA
				FileWriter writer = new FileWriter(file);
				writer.write("[]");
				writer.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//================================
	// CARREGAR USUÁRIOS
	//================================
	
	public List<Usuario> carregarUsuarios() {
		try {
			FileReader reader = new FileReader(CAMINHO_ARQUIVO);
			
			Type tipoLista = new TypeToken<List<Usuario>>() {}.getType();
			
			List<Usuario> usuarios = gson.fromJson(reader, tipoLista);
			
			reader.close();
			
			if(usuarios == null) {
				return new ArrayList<>();
			}
			
			return usuarios;
			
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	//================================
	// CARREGAR USUÁRIOS
	//================================
	
	public void salvarUsuarios(List<Usuario> usuarios) {
		try {
			FileWriter writer = new FileWriter(CAMINHO_ARQUIVO);
			
			gson.toJson(usuarios, writer);
			
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
 }
