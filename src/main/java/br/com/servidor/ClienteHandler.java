package br.com.servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import br.com.model.Mensagem;
import br.com.model.Usuario;
import br.com.service.AuthService;

public class ClienteHandler implements Runnable {

    private Socket socket;
    private AuthService authService;
    private ServidorGUI gui;
    private PrintWriter out;
    private BufferedReader in;
    private Usuario usuario;

    private static List<ClienteHandler> clientes = Collections.synchronizedList(new ArrayList<>());

    public ClienteHandler(Socket socket, AuthService authService, ServidorGUI gui) {
        this.socket = socket;
        this.authService = authService;
        this.gui = gui;
        clientes.add(this);
        atualizarGUI();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true);

            Gson gson = new Gson();
            String json;

            while ((json = in.readLine()) != null) {
                gui.log("[" + socket.getInetAddress() + "]: " + json);
                try {
                    Mensagem msg = gson.fromJson(json, Mensagem.class);
                    if (msg != null && msg.getOp() != null) {
                        processarMensagem(msg, gson);
                    }
                } catch (Exception e) {
                    gui.log("[Erro JSON]: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            gui.log("[Conexão]: Encerrada com " + socket.getInetAddress());
        } finally {
            desconectar();
        }
    }

    private void processarMensagem(Mensagem msg, Gson gson) {
        switch (msg.getOp()) {
            case "login": 
            	tratarLogin(msg, gson); 
            	break;
            case "logout": 
            	tratarLogout(msg, gson); 
            	break;
            case "cadastrarUsuario": 
            	tratarCadastro(msg, gson); 
            	break;
            case "enviarMensagem": 
            	tratarMensagem(msg, gson); 
            	break;
            case "listarUsuariosLogados": 
            	tratarListarLogados(msg, gson); 
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
            case "consultarUsuariosAdmin": 
            	tratarConsultarTodosAdmin(msg, gson); 
            	break;
            case "consultarUsuarioAdmin": 
            	tratarConsultarUsuarioAdmin(msg, gson); 
            	break;
            case "atualizarUsuarioAdmin": 
            	tratarAtualizarUsuarioAdmin(msg, gson); 
            	break;
            case "deletarUsuarioAdmin": 
            	tratarDeletarUsuarioAdmin(msg, gson); 
            	break;
            default: 
            	gui.log("[Aviso]: Operação desconhecida: " + msg.getOp());
        }
    }

    private void tratarLogin(Mensagem msg, Gson gson) {
        Usuario u = authService.realizarLogin(msg.getUsuario(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        if (u != null) {
            this.usuario = u;
            resposta.setResposta("200");
            resposta.setToken(u.getToken());
            atualizarGUI();
            notificarTodosUsuariosLogados();
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Usuário ou senha inválidos");
        }
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarLogout(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        if (this.usuario != null) {
            gui.log("[Logout]: " + this.usuario.getUsuario());
            this.usuario = null;
            resposta.setResposta("200");
            resposta.setMensagem("Logout efetuado");
            atualizarGUI();
            notificarTodosUsuariosLogados();
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao efetuar logout");
            gui.log(gson.toJson(resposta));
        }
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarMensagem(Mensagem msg, Gson gson) {
        if (this.usuario == null) return;
        
        Mensagem encaminhada = new Mensagem();
        encaminhada.setOp("receberMensagem");
        encaminhada.setUsuario(this.usuario.getUsuario());
        encaminhada.setMensagem(msg.getMensagem());

        if ("todos".equalsIgnoreCase(msg.getDestinatario())) {
            gui.log("[Broadcast] de " + usuario.getUsuario() + ": " + msg.getMensagem());
            synchronized (clientes) {
                for (ClienteHandler c : clientes) {
                    if (c.usuario != null) {
                        c.out.println(gson.toJson(encaminhada));
                    }
                }
            }
        } else {
            gui.log("[Mensagem] de " + usuario.getUsuario() + " para " + msg.getDestinatario() + ": " + msg.getMensagem());
            synchronized (clientes) {
                for (ClienteHandler c : clientes) {
                    if (c.usuario != null && c.usuario.getUsuario().equalsIgnoreCase(msg.getDestinatario())) {
                        c.out.println(gson.toJson(encaminhada));
                        return;
                    }
                }
            }
        }
    }

    private void tratarListarLogados(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();
        resposta.setOp("listaUsuariosLogados");
        List<Usuario> logados = clientes.stream()
                .filter(c -> c.usuario != null)
                .map(c -> c.usuario)
                .collect(Collectors.toList());
        resposta.setLista_usuarios(logados);
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void notificarTodosUsuariosLogados() {
        Gson gson = new Gson();
        Mensagem msg = new Mensagem();
        msg.setOp("listaUsuariosLogados");
        List<Usuario> logados = clientes.stream()
                .filter(c -> c.usuario != null)
                .map(c -> c.usuario)
                .collect(Collectors.toList());
        msg.setLista_usuarios(logados);
        String json = gson.toJson(msg);
        synchronized (clientes) {
            for (ClienteHandler c : clientes) {
                if (c.usuario != null) {
                    c.out.println(json);
                }
            }
        }
    }

    private void atualizarGUI() {
        List<String[]> dados = new ArrayList<>();
        synchronized (clientes) {
            for (ClienteHandler c : clientes) {
                String nome = (c.usuario != null) ? c.usuario.getUsuario() : "Conectado (Sem Login)";
                String ip = c.socket.getInetAddress().getHostAddress();
                String porta = String.valueOf(c.socket.getPort());
                dados.add(new String[]{nome, ip, porta});
            }
        }
        gui.atualizarUsuariosLogados(dados);
    }

    private void desconectar() {
        clientes.remove(this);
        atualizarGUI();
        notificarTodosUsuariosLogados();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {}
    }

    // Métodos administrativos e outros mantidos para compatibilidade
    private void tratarCadastro(Mensagem msg, Gson gson) {
        boolean sucesso = authService.cadastrar(msg.getNome(), msg.getUsuario(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        resposta.setResposta(sucesso ? "200" : "401");
        resposta.setMensagem(sucesso ? "Cadastrado com sucesso" : "Erro ao cadastrar");
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarConsulta(Mensagem msg, Gson gson) {
        if (usuario == null) return;
        Usuario encontrado = authService.buscarPorUsuario(usuario.getUsuario());
        Mensagem resposta = new Mensagem();
        if (encontrado != null) {
            resposta.setResposta("200");
            resposta.setNome(encontrado.getNome());
            resposta.setUsuario(encontrado.getUsuario());
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
        }
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarUpdate(Mensagem msg, Gson gson) {
        if (usuario == null) return;
        boolean sucesso = authService.atualizarUsuario(usuario.getUsuario(), msg.getNome(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        resposta.setResposta(sucesso ? "200" : "401");
        resposta.setMensagem(sucesso ? "Atualizado com sucesso" : "Erro ao atualizar");
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarDelete(Mensagem msg, Gson gson) {
        if (usuario == null) return;
        boolean sucesso = authService.deletarUsuario(usuario.getUsuario());
        Mensagem resposta = new Mensagem();
        if (sucesso) {
            resposta.setResposta("200");
            resposta.setMensagem("Deletado com sucesso");
            this.usuario = null;
            atualizarGUI();
            notificarTodosUsuariosLogados();
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao deletar");
        }
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarConsultarTodosAdmin(Mensagem msg, Gson gson) {
    	Mensagem resposta = new Mensagem();
        if (usuario == null || !usuario.getUsuario().equalsIgnoreCase("admin")) {
        	resposta.setResposta("401");
        	resposta.setMensagem("Deve ser ADM para consultar a lista");
        } else {
        	resposta.setResposta("200");
            resposta.setLista_usuarios(authService.listarUsuarios());
        }
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarConsultarUsuarioAdmin(Mensagem msg, Gson gson) {
        if (usuario == null || !usuario.getUsuario().equalsIgnoreCase("admin")) return;
        Usuario u = authService.buscarPorUsuario(msg.getUsuario());
        Mensagem resposta = new Mensagem();
        if (u != null) {
            resposta.setResposta("200");
            resposta.setNome(u.getNome());
            resposta.setUsuario(u.getUsuario());
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
        }
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarAtualizarUsuarioAdmin(Mensagem msg, Gson gson) {
        if (usuario == null || !usuario.getUsuario().equalsIgnoreCase("admin")) return;
        boolean sucesso = authService.atualizarUsuario(msg.getUsuario(), msg.getNome(), msg.getSenha());
        Mensagem resposta = new Mensagem();
        resposta.setResposta(sucesso ? "200" : "401");
        resposta.setMensagem(sucesso ? "Usuário atualizado com sucesso" : "Erro ao atualizar usuário");
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }

    private void tratarDeletarUsuarioAdmin(Mensagem msg, Gson gson) {
        if (usuario == null || !usuario.getUsuario().equalsIgnoreCase("admin")) return;
        boolean sucesso = authService.deletarUsuario(msg.getUsuario());
        Mensagem resposta = new Mensagem();
        resposta.setResposta(sucesso ? "200" : "401");
        resposta.setMensagem(sucesso ? "Usuário deletado com sucesso" : "Erro ao deletar usuário");
        gui.log("[Servidor]: " + gson.toJson(resposta));
        out.println(gson.toJson(resposta));
    }
}
