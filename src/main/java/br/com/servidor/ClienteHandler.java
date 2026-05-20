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
                System.out.println("[" + socket.getInetAddress() + "]: Recebido: " + json);
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

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: Login -> Resposta: " + json);
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

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: Cadastro -> Resposta: " + json);
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

        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
            out.println(gson.toJson(resposta));
            return;
        }

        Usuario encontrado = authService.buscarPorUsuario(usuario.getUsuario());
        if (encontrado != null) {
            resposta.setResposta("200");
            resposta.setNome(encontrado.getNome());
            resposta.setUsuario(encontrado.getUsuario());
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Usuário não encontrado.");
        }

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: Consulta -> Resposta: " + json);
    }

    private void tratarUpdate(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();

        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
            out.println(gson.toJson(resposta));
            return;
        }

        // Usuário comum só pode alterar seus próprios dados
        boolean sucesso = authService.atualizarUsuario(usuario.getUsuario(), msg.getNome(), msg.getSenha());

        if (sucesso) {
            if (msg.getNome() != null) usuario.setNome(msg.getNome());
            if (msg.getSenha() != null) usuario.setSenha(msg.getSenha());
            resposta.setResposta("200");
            resposta.setMensagem("Atualizado com sucesso");
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao atualizar dados (verifique a senha numérica).");
        }

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: Atualizar -> Resposta: " + json);
    }

    private void tratarDelete(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();

        if (usuario == null || usuario.getToken() == null || !usuario.getToken().equals(msg.getToken())) {
            resposta.setResposta("401");
            resposta.setMensagem("Token inválido");
            out.println(gson.toJson(resposta));
            return;
        }

        // Usuário comum só pode deletar a própria conta
        boolean sucesso = authService.deletarUsuario(usuario.getUsuario());

        if (sucesso) {
            resposta.setResposta("200");
            resposta.setMensagem("Deletado com sucesso");
            usuario = null;
            clientes.remove(this);
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao deletar usuário.");
        }

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: Deletar -> Resposta: " + json);
    }

    // =========== OPERAÇÕES ADM ===========

    private boolean verificarTokenAdmin(Mensagem msg, Mensagem resposta) {
        // O ADM usa o campo token_admin conforme o protocolo
        String tokenAdmin = msg.getToken_admin();
        if (tokenAdmin == null || !tokenAdmin.equals("adm")) {
            resposta.setResposta("401");
            resposta.setMensagem("Deve ser ADM para executar esta operação");
            return false;
        }
        // Verifica também se o usuário conectado na sessão é realmente o admin
        if (usuario == null || !usuario.getToken().equals("adm")) {
            resposta.setResposta("401");
            resposta.setMensagem("Deve ser ADM para executar esta operação");
            return false;
        }
        return true;
    }

    private void tratarConsultarTodosAdmin(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();

        if (!verificarTokenAdmin(msg, resposta)) {
            out.println(gson.toJson(resposta));
            System.out.println("[Servidor]: ConsultarTodosAdmin -> Resposta: " + gson.toJson(resposta));
            return;
        }

        List<Usuario> lista = authService.listarUsuarios();
        resposta.setResposta("200");
        resposta.setLista_usuarios(lista);

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: ConsultarTodosAdmin -> Resposta: " + json);
    }

    private void tratarConsultarUsuarioAdmin(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();

        if (!verificarTokenAdmin(msg, resposta)) {
            out.println(gson.toJson(resposta));
            System.out.println("[Servidor]: ConsultarUsuarioAdmin -> Resposta: " + gson.toJson(resposta));
            return;
        }

        String alvo = msg.getUsuario();
        if (alvo == null || alvo.trim().isEmpty()) {
            resposta.setResposta("401");
            resposta.setMensagem("Nome de usuário não informado.");
            out.println(gson.toJson(resposta));
            return;
        }

        Usuario encontrado = authService.buscarPorUsuario(alvo);
        if (encontrado != null) {
            resposta.setResposta("200");
            resposta.setNome(encontrado.getNome());
            resposta.setUsuario(encontrado.getUsuario());
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Usuário não encontrado.");
        }

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: ConsultarUsuarioAdmin -> Resposta: " + json);
    }

    private void tratarAtualizarUsuarioAdmin(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();

        if (!verificarTokenAdmin(msg, resposta)) {
            out.println(gson.toJson(resposta));
            System.out.println("[Servidor]: AtualizarUsuarioAdmin -> Resposta: " + gson.toJson(resposta));
            return;
        }

        String alvo = msg.getUsuario();
        if (alvo == null || alvo.trim().isEmpty()) {
            resposta.setResposta("401");
            resposta.setMensagem("Nome de usuário alvo não informado.");
            out.println(gson.toJson(resposta));
            return;
        }

        boolean sucesso = authService.atualizarUsuario(alvo, msg.getNome(), msg.getSenha());

        if (sucesso) {
            resposta.setResposta("200");
            resposta.setMensagem("Usuario atualizado com sucesso");
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao atualizar (verifique se usuário existe e se senha tem 6 dígitos numéricos).");
        }

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: AtualizarUsuarioAdmin -> Resposta: " + json);
    }

    private void tratarDeletarUsuarioAdmin(Mensagem msg, Gson gson) {
        Mensagem resposta = new Mensagem();

        if (!verificarTokenAdmin(msg, resposta)) {
            out.println(gson.toJson(resposta));
            System.out.println("[Servidor]: DeletarUsuarioAdmin -> Resposta: " + gson.toJson(resposta));
            return;
        }

        String alvo = msg.getUsuario();
        if (alvo == null || alvo.trim().isEmpty()) {
            resposta.setResposta("401");
            resposta.setMensagem("Nome de usuário alvo não informado.");
            out.println(gson.toJson(resposta));
            return;
        }

        boolean sucesso = authService.deletarUsuario(alvo);

        if (sucesso) {
            resposta.setResposta("200");
            resposta.setMensagem("Usuario deletado com sucesso");
        } else {
            resposta.setResposta("401");
            resposta.setMensagem("Erro ao deletar usuário (verifique se existe e não é o admin padrão).");
        }

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: DeletarUsuarioAdmin -> Resposta: " + json);
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

        String json = gson.toJson(resposta);
        out.println(json);
        System.out.println("[Servidor]: Logout -> Resposta: " + json);
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
