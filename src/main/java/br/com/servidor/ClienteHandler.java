package br.com.servidor;

import java.io.BufferedReader;
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

    private static List<ClienteHandler> clientes = Collections.synchronizedList(new ArrayList<>());

    public ClienteHandler(Socket socket, AuthService authService) {
        this.socket = socket;
        this.authService = authService;
    }

    @Override
    public void run() {
        try {
            // Comunicação explícita em UTF-8 conforme exigência do protocolo
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")); 
            out = new PrintWriter(socket.getOutputStream(), true); // Auto-flush ativo

            Gson gson = new Gson();
            String json;

            while ((json = in.readLine()) != null) {
                try {
                    Mensagem msg = gson.fromJson(json, Mensagem.class);

                    if (msg == null || msg.getOp() == null) {
                        System.out.println("[Servidor]: JSON inválido ou sem 'op' recebido.");
                        continue;
                    }

                    processarMensagem(msg, gson);

                } catch (Exception e) {
                    System.out.println("[Servidor]: Erro ao processar JSON: " + json);
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("[Servidor]: Conexão encerrada com cliente: " + e.getMessage()); 
        } finally {
            desconectar();
        }
    }

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

    private void tratarLogin(Mensagem msg, Gson gson) {
        Usuario u = authService.realizarLogin(msg.getUsuario(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        
        if (u != null) {
            this.usuario = u;
            if (!clientes.contains(this)) {
                clientes.add(this);
            }
            resposta.setResposta("200"); 
            resposta.setToken(u.getToken());
        } else {
            resposta.setResposta("401"); 
            resposta.setMensagem("Usuário ou senha inválidos");
        }

        out.println(gson.toJson(resposta));
        System.out.println("[" + socket.getInetAddress() + "]: Requisitou Login -> Resposta Servidor: " + gson.toJson(resposta));
    }

    private void tratarCadastro(Mensagem msg, Gson gson) {
        boolean sucesso = authService.cadastrar(msg.getNome(), msg.getUsuario(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        
        if (sucesso) {
            resposta.setResposta("200"); 
            resposta.setMensagem("Cadastrado com sucesso");
        } else {
            resposta.setResposta("401"); 
            resposta.setMensagem("Erro ao cadastrar usuário. Verifique as restrições.");
        }

        out.println(gson.toJson(resposta));
        System.out.println("[" + socket.getInetAddress() + "]: Requisitou Cadastro -> Resposta Servidor: " + gson.toJson(resposta) );
    }

    private void tratarMensagem(Mensagem msg, Gson gson) {
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) return;
        if (msg.getDestinatario() == null) return;

        synchronized (clientes) {
            for (ClienteHandler cliente : clientes) {
                if (cliente.usuario != null && msg.getDestinatario().equalsIgnoreCase(cliente.usuario.getUsuario())) {
                    Mensagem resposta = new Mensagem();
                    resposta.setResposta("200"); 
                    resposta.setMensagem("[" + usuario.getUsuario() + "]: " + msg.getMensagem());
                    cliente.out.println(gson.toJson(resposta));
                    return;
                }
            }
        }
    }

    private void tratarConsulta(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        
        // 1. Validação do Token básico
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401"); 
            resposta.setMensagem("Token inválido");
            out.println(gson.toJson(resposta));
            return;
        }

        boolean isAdmin = usuario.getToken().equals("adm"); 
        String alvo = msg.getUsuario();

        // Se o payload não especificar um alvo, assume o próprio usuário conectado
        if (alvo == null || alvo.trim().isEmpty()) {
            alvo = usuario.getUsuario();
        }

        // 2. Bloqueio Crítico: Usuário comum tentando consultar terceiros [cite: 53]
        if (!isAdmin && !usuario.getUsuario().equalsIgnoreCase(alvo)) {
            resposta.setResposta("401"); 
            resposta.setMensagem("Operação não permitida para usuário comum."); 
        } else {
            // ADM consultando qualquer um ou Usuário Comum consultando a si mesmo 
            Usuario encontrado = authService.buscarPorUsuario(alvo);
            if (encontrado != null) {
                resposta.setResposta("200"); 
                resposta.setNome(encontrado.getNome());
                resposta.setUsuario(encontrado.getUsuario());
            } else {
                resposta.setResposta("401");
                resposta.setMensagem("Usuário não encontrado.");
            }
        }
        
        out.println(gson.toJson(resposta));
    }

    private void tratarUpdate(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401"); 
            resposta.setMensagem("Token inválido");
            out.println(gson.toJson(resposta));
            return;
        }

        boolean isAdmin = usuario.getToken().equals("adm"); 
        String alvo = msg.getUsuario(); // Login de quem será alterado

        if (alvo == null || alvo.trim().isEmpty()) {
            alvo = usuario.getUsuario();
        }

        // Bloqueio Crítico: Usuário comum tentando editar terceiros [cite: 53, 67]
        if (!isAdmin && !usuario.getUsuario().equalsIgnoreCase(alvo)) {
            resposta.setResposta("401"); 
            resposta.setMensagem("Operação não permitida para usuário comum."); 
        } else {
            // ADM alterando terceiros OU Usuário comum alterando a si mesmo [cite: 49, 61]
            boolean sucesso = authService.atualizarUsuario(alvo, msg.getNome(), msg.getSenha());

            if (sucesso) {
                // Se o próprio usuário se editou, sincroniza a instância local da Thread
                if (usuario.getUsuario().equalsIgnoreCase(alvo)) {
                    if (msg.getNome() != null) usuario.setNome(msg.getNome());
                    if (msg.getSenha() != null) usuario.setSenha(msg.getSenha());
                }
                resposta.setResposta("200"); 
                resposta.setMensagem("Atualizado com sucesso");
            } else {
                resposta.setResposta("401");
                resposta.setMensagem("Erro ao atualizar dados (verifique a senha numérica).");
            }
        }
        
        out.println(gson.toJson(resposta));
    }

    private void tratarDelete(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        
        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401"); 
            resposta.setMensagem("Token inválido");
            out.println(gson.toJson(resposta));
            return;
        }

        boolean isAdmin = usuario.getToken().equals("adm"); 
        String alvo = msg.getUsuario();

        if (alvo == null || alvo.trim().isEmpty()) {
            alvo = usuario.getUsuario();
        }

        // Bloqueio Crítico: Usuário comum tentando apagar terceiros [cite: 55, 68]
        if (!isAdmin && !usuario.getUsuario().equalsIgnoreCase(alvo)) {
            resposta.setResposta("401"); 
            resposta.setMensagem("Operação não permitida para usuário comum."); 
        } else {
            // ADM excluindo terceiros OU Usuário comum se autodeletando [cite: 51, 63]
            boolean sucesso = authService.deletarUsuario(alvo);

            if (sucesso) {
                resposta.setResposta("200"); 
                resposta.setMensagem("Deletado com sucesso");
                
                // Se a conta apagada foi a do próprio cliente conectado, desconecta-o
                if (usuario.getUsuario().equalsIgnoreCase(alvo)) {
                    usuario = null;
                    clientes.remove(this);
                }
            } else {
                resposta.setResposta("401");
                resposta.setMensagem("Erro ao deletar usuário.");
            }
        }

        out.println(gson.toJson(resposta));
    }

    private void tratarLogout(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        
        if (usuario != null && usuario.getToken() != null && usuario.getToken().equals(msg.getToken())) {
            usuario = null;
            clientes.remove(this);
            resposta.setResposta("200"); 
            resposta.setMensagem("Logout efetuado");
        } else {
            resposta.setResposta("401"); 
            resposta.setMensagem("Erro ao efetuar logout");
        }

        out.println(gson.toJson(resposta));
    }

    private void desconectar() {
        try {
            clientes.remove(this);
            if (socket != null && !socket.isClosed()) {
                socket.close(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}