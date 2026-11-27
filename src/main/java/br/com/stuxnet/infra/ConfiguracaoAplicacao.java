package br.com.stuxnet.infra;

import br.com.stuxnet.command.SaudeCommand;
import br.com.stuxnet.command.cliente.*;
import br.com.stuxnet.command.entrega.*;
import br.com.stuxnet.command.pedido.*;
import br.com.stuxnet.command.produto.*;
import br.com.stuxnet.dao.ClienteDAO;
import br.com.stuxnet.dao.PedidoDAO;
import br.com.stuxnet.dao.ProdutoDAO;
import br.com.stuxnet.dao.jdbc.ClienteJdbcDAO;
import br.com.stuxnet.dao.jdbc.PedidoJdbcDAO;
import br.com.stuxnet.dao.jdbc.ProdutoJdbcDAO;
import br.com.stuxnet.infra.http.Router;
import br.com.stuxnet.service.*;

/** Composição manual das dependências e associação de método/rota ao Command correspondente. */
public final class ConfiguracaoAplicacao {
    private ConfiguracaoAplicacao() {}

    public static Router criarRouter(ConsultaCep cep) {
        BancoDeDados banco = BancoDeDados.configurado();
        banco.inicializar();
        return criarRouter(cep, new ProdutoJdbcDAO(banco), new ClienteJdbcDAO(banco),
                new PedidoJdbcDAO(banco), new Transacao(banco));
    }

    public static Router criarRouter(ConsultaCep cep, ProdutoDAO produtos, ClienteDAO clientes,
                                     PedidoDAO pedidos, Transacao transacao) {
        FreteService freteService = new FreteService(cep);
        ProdutoService produtoService = new ProdutoService(produtos, pedidos, transacao);
        ClienteService clienteService = new ClienteService(clientes, pedidos, cep, new SenhaService(), transacao);
        PedidoService pedidoService = new PedidoService(pedidos, produtos, clientes, freteService, transacao);

        Router router = new Router();
        router.registrar("GET", "/api/health", new SaudeCommand());

        router.registrar("GET", "/api/produtos", new ListarProdutosCommand(produtoService));
        router.registrar("GET", "/api/produtos/{id}", new BuscarProdutoCommand(produtoService));
        router.registrar("POST", "/api/produtos", new CadastrarProdutoCommand(produtoService));
        router.registrar("PUT", "/api/produtos/{id}", new AtualizarProdutoCommand(produtoService));
        router.registrar("DELETE", "/api/produtos/{id}", new ExcluirProdutoCommand(produtoService));

        router.registrar("GET", "/api/clientes", new ListarClientesCommand(clienteService));
        router.registrar("GET", "/api/clientes/{id}", new BuscarClienteCommand(clienteService));
        router.registrar("POST", "/api/clientes", new CadastrarClienteCommand(clienteService));
        router.registrar("PUT", "/api/clientes/{id}", new AtualizarClienteCommand(clienteService));
        router.registrar("DELETE", "/api/clientes/{id}", new ExcluirClienteCommand(clienteService));
        router.registrar("GET", "/api/clientes/{id}/enderecos", new ListarEnderecosCommand(clienteService));
        router.registrar("POST", "/api/clientes/{id}/enderecos", new CadastrarEnderecoCommand(clienteService));

        router.registrar("GET", "/api/pedidos", new ListarPedidosCommand(pedidoService));
        router.registrar("GET", "/api/pedidos/{id}", new BuscarPedidoCommand(pedidoService));
        router.registrar("POST", "/api/pedidos", new CriarPedidoCommand(pedidoService));
        router.registrar("PUT", "/api/pedidos/{id}/cancelamento", new CancelarPedidoCommand(pedidoService));

        router.registrar("GET", "/api/cep/{cep}", new ConsultarCepCommand(cep));
        router.registrar("GET", "/api/frete", new CalcularFreteCommand(freteService));
        return router;
    }
}
