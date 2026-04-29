package br.com.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;

import br.com.model.Mensagem;
import br.com.model.Usuario;
import br.com.service.AuthService;

public class ClienteHandler implements Runnable {

    private Socket socket;
    private AuthService authService;
    private PrintWriter out;
    private BufferedReader in;
    private Usuario usuario;

    // LISTA DE CLIENTES CONECTADOS (THREAD SAFE)
    private static List<ClienteHandler> clientes = Collections.synchronizedList(new ArrayList<>());

    public ClienteHandler(Socket socket, AuthService authService) {
        this.socket = socket;
        this.authService = authService;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Gson gson = new Gson();
            String json;

            while ((json = in.readLine()) != null) {
                try {
                    Mensagem msg = gson.fromJson(json, Mensagem.class);

                    if (msg == null || msg.getOp() == null) {
                        System.out.println("[Servidor]: JSON inválido recebido: " + json);
                        continue;
                    }

                    processarMensagem(msg, gson);

                } catch (Exception e) {
                    System.out.println("[Servidor]: Erro ao processar JSON: " + json);
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("[Servidor]: Erro com cliente: " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    // =========================
    // PROCESSADOR PRINCIPAL
    // =========================
    private void processarMensagem(Mensagem msg, Gson gson) {
        switch (msg.getOp()) {
            case "login":
                tratarLogin(msg, gson);
                break;
            case "cadastrarUsuario":
                tratarCadastro(msg, gson);
                break;
            case "enviarMensagem":
                tratarMensagem(msg, gson);
                break;
            case "consultarUsuario":
                tratarConsulta(msg, gson);
                break;
            case "atualizarUsuario":
                tratarUpdate(msg, gson);
                break;
            case "deletarUsuario":
                tratarDelete(msg, gson);
                break;
            case "logout":
                tratarLogout(msg, gson);
                break;
            default:
                System.out.println("[Servidor]: Operação desconhecida: " + msg.getOp());
        }
    }

    // =========================
    // LOGIN
    // =========================
    private void tratarLogin(Mensagem msg, Gson gson) {
        Usuario u = authService.realizarLogin(msg.getUsuario(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        Mensagem logServidor = new Mensagem();
        
        if (u != null) {
            this.usuario = u;
            clientes.add(this);

            resposta.setResposta("200");
            resposta.setToken(u.getToken());
//          resposta.setMensagem("Login efetuado com sucesso");
            
            logServidor.setOp("login");	
            logServidor.setUsuario(msg.getUsuario());
            logServidor.setSenha(msg.getSenha());
            

        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Usuário ou senha inválidos");
        }

        out.println(gson.toJson(resposta));
        System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(logServidor));
    }

    // =========================
    // CADASTRO
    // =========================
    private void tratarCadastro(Mensagem msg, Gson gson) {
        boolean sucesso = authService.cadastrar(msg.getNome(), msg.getUsuario(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        Mensagem logServidor = new Mensagem();
        
        if (sucesso) {
            resposta.setResposta("200");
            resposta.setMensagem("Cadastrado com sucesso");
            
            logServidor.setOp("cadastrarUsuario");
            logServidor.setNome(msg.getNome());
            logServidor.setUsuario(msg.getUsuario());
            logServidor.setSenha(msg.getSenha());
            
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao cadastrar usuário");
        }

        out.println(gson.toJson(resposta));
        System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(logServidor));
    }

    // =========================
    // CHAT 1 PARA 1
    // =========================
    private void tratarMensagem(Mensagem msg, Gson gson) {
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) return;
        if (msg.getDestinatario() == null) return;

        synchronized (clientes) {
            for (ClienteHandler cliente : clientes) {
                if (cliente.usuario != null &&
                    msg.getDestinatario().equals(cliente.usuario.getUsuario())) {

                    Mensagem resposta = new Mensagem();
                    resposta.setResposta("200");
                    resposta.setMensagem("Mensagem enviada com sucesso");
                    resposta.setMensagem("[" + usuario.getUsuario() + "]: " + msg.getMensagem());

                    cliente.out.println(gson.toJson(resposta));
                    System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(resposta));
                    return;
                }
            }
        }
    }

    // =========================
    // CONSULTAR
    // =========================
    private void tratarConsulta(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        Mensagem logServidor = new Mensagem();
        
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
            
        } else {
            resposta.setResposta("200");
            resposta.setNome(usuario.getNome());
            resposta.setUsuario(usuario.getUsuario());
            
            logServidor.setOp("consultarUsuario");
            logServidor.setToken(usuario.getToken());
        }

        out.println(gson.toJson(resposta));
        System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(logServidor));
    }

    // =========================
    // ATUALIZAR USUÁRIO
    // =========================
    private void tratarUpdate(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        Mensagem logServidor = new Mensagem();
        
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
        } else {
            boolean sucesso = authService.atualizarUsuario(usuario.getUsuario(), /*msg.getNovoNome(), msg.getNovaSenha()*/ msg.getNome(), msg.getSenha());

            if (sucesso) {
//              usuario.setNome(msg.getNovoNome());
//              usuario.setSenha(msg.getNovaSenha());
            	usuario.setNome(msg.getNome());
            	usuario.setSenha(msg.getSenha());
            	
                resposta.setResposta("200");
                resposta.setMensagem("Atualizado com sucesso");
                
                logServidor.setOp("atualizarUsuario");
                logServidor.setToken(usuario.getToken());
                logServidor.setNome(usuario.getNome());
                logServidor.setSenha(usuario.getSenha());
                
            } else {
                resposta.setResposta("401");
                resposta.setMensagem("Erro ao atualizar usuário");
            }
        }

        out.println(gson.toJson(resposta));
        System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(logServidor));
    }

    // =========================
    // DELETAR USUÁRIO
    // =========================
    private void tratarDelete(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        Mensagem logServidor = new Mensagem();
        
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
        } else {
            boolean sucesso = authService.deletarUsuario(usuario.getUsuario());

            if (sucesso) {
                resposta.setResposta("200");
                resposta.setMensagem("Deletado com sucesso");
                
                logServidor.setOp("deletarUsuario");
                logServidor.setToken(usuario.getToken());
                clientes.remove(this);
                usuario = null;
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            } else {
                resposta.setResposta("401");
                resposta.setMensagem("Erro ao deletar usuário");
            }
        }

        out.println(gson.toJson(resposta));
        System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(logServidor));
    }

    // =========================
    // REALIZAR LOGOUT
    // =========================
    private void tratarLogout(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        Mensagem logServidor = new Mensagem();
        
        if (usuario != null && usuario.getToken() != null && usuario.getToken().equals(msg.getToken())) {
        	logServidor.setOp("logout");
            logServidor.setToken(usuario.getToken());
        	
            clientes.remove(this);
            usuario = null;
            resposta.setResposta("200");
            resposta.setMensagem("Logout efetuado");
            
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao efetuar logout");
        }

        out.println(gson.toJson(resposta));
        System.out.println("["+socket.getInetAddress()+"]: "+gson.toJson(logServidor));
    }

    // =========================
    // DESCONECTAR
    // =========================
    private void desconectar() {
        try {
            clientes.remove(this);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
