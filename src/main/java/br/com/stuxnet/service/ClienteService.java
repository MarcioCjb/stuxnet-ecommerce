package br.com.stuxnet.service;

import br.com.stuxnet.model.Cliente;
import br.com.stuxnet.model.Endereco;
import br.com.stuxnet.dto.ClienteDTO;
import br.com.stuxnet.dto.EnderecoClienteDTO;
import br.com.stuxnet.dto.EnderecoDTO;
import br.com.stuxnet.exception.ApiException;
import br.com.stuxnet.dao.ClienteDAO;
import br.com.stuxnet.dao.PedidoDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClienteService {
    private final ClienteDAO clientes;
    private final PedidoDAO pedidos;
    private final ConsultaCep consultaCep;
    private final SenhaService senhas;
    private final Object transacao;

    public ClienteService(ClienteDAO clientes, PedidoDAO pedidos, ConsultaCep consultaCep,
                          SenhaService senhas, Object transacao) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.consultaCep = consultaCep;
        this.senhas = senhas;
        this.transacao = transacao;
    }
    public List<ClienteDTO.Resposta> listar() {
        synchronized (transacao) { return clientes.listar().stream().map(ClienteDTO.Resposta::de).toList(); }
    }
    public ClienteDTO.Resposta buscar(long id) {
        synchronized (transacao) { return ClienteDTO.Resposta.de(encontrar(id)); }
    }
    public ClienteDTO.Resposta criar(ClienteDTO.Cadastro dto) {
        String nome = Validacao.texto(dto.nome(), "nome", 150);
        String email = email(dto.email());
        String hash = senhas.gerarHash(dto.senha());
        synchronized (transacao) {
            emailDisponivel(email, null);
            return ClienteDTO.Resposta.de(clientes.salvar(new Cliente(clientes.proximoId(), nome, email, hash, List.of())));
        }
    }
    public ClienteDTO.Resposta atualizar(long id, ClienteDTO.Atualizacao dto) {
        String nome = Validacao.texto(dto.nome(), "nome", 150);
        String email = email(dto.email());
        synchronized (transacao) {
            Cliente anterior = encontrar(id);
            emailDisponivel(email, id);
            return ClienteDTO.Resposta.de(clientes.salvar(new Cliente(id, nome, email, anterior.senhaHash(), anterior.enderecos())));
        }
    }
    public void excluir(long id) {
        synchronized (transacao) {
            encontrar(id);
            if (pedidos.listar().stream().anyMatch(p -> p.clienteId() == id)) {
                throw ApiException.conflito("Cliente possui histórico de pedidos e não pode ser excluído");
            }
            clientes.excluir(id);
        }
    }
    public List<EnderecoClienteDTO.Resposta> enderecos(long id) {
        synchronized (transacao) {
            return encontrar(id).enderecos().stream().map(EnderecoClienteDTO.Resposta::de).toList();
        }
    }
    public EnderecoClienteDTO.Resposta adicionarEndereco(long id, EnderecoClienteDTO.Requisicao dto) {
        synchronized (transacao) { encontrar(id); }
        String numero = Validacao.texto(dto.numero(), "numero", 30);
        String complemento = dto.complemento() == null ? "" : dto.complemento().strip();
        Validacao.exigir(complemento.length() <= 200, "complemento deve ter até 200 caracteres");
        // I/O externo fora da seção crítica; o cliente é consultado novamente antes de salvar.
        EnderecoDTO cep = consultaCep.consultarCEP(dto.cep());
        synchronized (transacao) {
            Cliente cliente = encontrar(id);
            long enderecoId = cliente.enderecos().stream().mapToLong(Endereco::id).max().orElse(0) + 1;
            Endereco endereco = new Endereco(enderecoId, cep.getCep(), cep.getLogradouro(), numero,
                    complemento, cep.getBairro(), cep.getLocalidade(), cep.getUf());
            List<Endereco> lista = new ArrayList<>(cliente.enderecos());
            lista.add(endereco);
            clientes.salvar(new Cliente(id, cliente.nome(), cliente.email(), cliente.senhaHash(), lista));
            return EnderecoClienteDTO.Resposta.de(endereco);
        }
    }
    private Cliente encontrar(long id) {
        return clientes.buscar(id).orElseThrow(() -> ApiException.naoEncontrado("Cliente"));
    }
    private String email(String valor) {
        String email = Validacao.texto(valor, "email", 254).toLowerCase(Locale.ROOT);
        Validacao.exigir(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "email inválido");
        return email;
    }
    private void emailDisponivel(String email, Long id) {
        if (clientes.listar().stream().anyMatch(c -> c.email().equals(email) && (id == null || c.id() != id))) {
            throw ApiException.conflito("Email já cadastrado");
        }
    }
}
