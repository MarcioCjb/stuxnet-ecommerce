# StuxNet — e-commerce de hardware e periféricos

API REST acadêmica para uma loja de componentes de computador e periféricos. Permite gerenciar produtos e clientes, completar endereços pelo ViaCEP, calcular frete e criar ou cancelar pedidos com controle de estoque.

A aplicação usa **Java 17, sem frameworks de aplicação**: o servidor é o `HttpServer` do próprio JDK e a composição das dependências é manual. Gson, já utilizado no projeto, faz a conversão JSON. JUnit é utilizado somente nos testes. A persistência utiliza **PostgreSQL via JDBC**, mantendo os contratos DAO, os modelos e as rotas existentes. Não há ORM ou framework adicional. Produtos, clientes, endereços, pedidos e itens permanecem gravados após encerrar a aplicação.

## Executar

Pré-requisitos: JDK 17 ou superior, Maven 3.9 ou superior, PostgreSQL 17 ou superior e Postman Desktop para a demonstração. Java e Maven devem estar no `PATH`; confira com `java -version` e `mvn -version`. O primeiro build precisa de internet para baixar as dependências. As rotas de consulta de CEP, cadastro de endereço e cotação de frete precisam de acesso ao ViaCEP.

No terminal, a partir da pasta que contém este README:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/iniciar-postgres.ps1
mvn clean verify
java -jar target/stuxnet-ecommerce-1.0.0.jar
```

Execute os comandos na pasta `stuxnet-ecommerce`, onde estão `pom.xml` e `database.properties`. O arquivo de configuração é lido a partir da pasta atual do terminal.

O servidor atende em `http://localhost:8080/api`. Verifique `GET http://localhost:8080/api/health`; a resposta esperada é `200` e `{"aplicacao":"StuxNet","status":"UP"}`. Encerre com `Ctrl+C`.

Antes de recompilar no Windows, encerre a instância em execução: o processo Java mantém o JAR bloqueado e impede `mvn clean` de removê-lo.

Para alterar a porta no PowerShell:

```powershell
$env:PORT = "8081"
java -jar target/stuxnet-ecommerce-1.0.0.jar
```

O servidor escuta em `127.0.0.1`. Um banco novo começa vazio, sem cliente ou produto previamente cadastrado. **Os dados persistem ao reiniciar a API e o PostgreSQL.** Não é necessário deixar o Java aberto para preservar os cadastros.

## PostgreSQL local e configuração

A instalação deste computador utiliza o **instalador oficial do PostgreSQL 18 para Windows**, fora da pasta do usuário:

| Componente | Localização |
| --- | --- |
| Programa e ferramentas | `C:\Program Files\PostgreSQL\18` |
| Dados do banco | `C:\ProgramData\PostgreSQL\18\data` |
| Serviço do Windows | `postgresql-x64-18` (início automático) |
| Conexão da aplicação | `127.0.0.1:5432`, banco `stuxnet`, usuário `stuxnet` |
| Banco de testes | `stuxnet_test` |

Os bancos existentes e suas credenciais foram preservados na migração. A cópia anterior foi transferida para `C:\ProgramData\StuxNet-BackupPostgreSQL\instalacao-anterior` como backup; a pasta antiga em AppData foi desativada e transferida integralmente para fora do perfil do usuário. O diretório `.local` do projeto contém apenas arquivos auxiliares privados, como credenciais e backup; ele não é a instalação do PostgreSQL.

As credenciais aleatórias da aplicação estão em `database.properties`, ignorado pelo Git. As credenciais administrativas estão em `.local/postgres-admin.properties`, também ignorado. Não publique esses arquivos. A aplicação não utiliza o superusuário PostgreSQL.

O serviço inicia automaticamente com o Windows. Para consultar ou controlar seu estado:

```powershell
Get-Service postgresql-x64-18

# Os comandos de início/parada exigem PowerShell como administrador
# quando é necessário mudar o estado do serviço.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/iniciar-postgres.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/parar-postgres.ps1
```

Em outro computador Windows, instale para a máquina com o script abaixo. O instalador solicitará a confirmação administrativa do Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/instalar-postgres.ps1
# Para uma porta diferente, acrescente -Porta 5433.
```

O script utiliza `winget` para executar o [instalador oficial EDB indicado pelo PostgreSQL](https://www.postgresql.org/download/windows/), especificando Program Files, ProgramData e o serviço do Windows. Em uma instalação nova, cria o usuário e os dois bancos e gera `database.properties`. Caso detecte o cluster antigo deste projeto, faz um backup e restaura seus bancos na instalação da máquina. A origem só deve ser removida depois da validação dos dados. O PostgreSQL também fica registrado em Aplicativos instalados do Windows e inclui as ferramentas de linha de comando e o pgAdmin. Não é necessário modificar o PATH.

Para utilizar outro PostgreSQL, crie o usuário e os bancos como administrador (substitua a senha):

```sql
CREATE ROLE stuxnet LOGIN PASSWORD 'SUA_SENHA';
CREATE DATABASE stuxnet OWNER stuxnet;
CREATE DATABASE stuxnet_test OWNER stuxnet;
```

Copie `database.properties.example` para `database.properties` e ajuste URL, usuário e senha. Alternativamente, configure as variáveis abaixo no terminal que inicia o Java; elas têm prioridade sobre o arquivo:

```powershell
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/stuxnet"
$env:DB_USER = "stuxnet"
$env:DB_PASSWORD = "SUA_SENHA"
java -jar target/stuxnet-ecommerce-1.0.0.jar
```

O [driver oficial pgJDBC](https://jdbc.postgresql.org/download/) é incluído no JAR pelo Maven. Na inicialização, [schema.sql](src/main/resources/db/schema.sql) cria automaticamente as tabelas ausentes, as chaves, as sequências e os índices. A inicialização repetida não apaga registros; futuras mudanças no formato de tabelas existentes exigirão migrações SQL explícitas. Se a conexão falhar, a API não inicia; não há troca automática para memória.

### Modelo persistido e transações

| Tabela | Conteúdo e relacionamento |
| --- | --- |
| `produtos` | Catálogo, categoria, preço e estoque; ID gerado por sequência. |
| `clientes` | Nome, email único normalizado e hash da senha; ID por sequência. |
| `enderecos` | Endereços do cliente; chave composta `(cliente_id, id)` e exclusão em cascata com o cliente. |
| `pedidos` | Cliente, valores, modalidade, prazo, status e cópia do endereço em JSONB. |
| `pedido_itens` | Pedido e produto por chaves estrangeiras; nome e preço copiados no momento da compra. |

Os serviços utilizam `Transacao`, que compartilha uma conexão JDBC entre os DAOs durante cada operação. `commit` confirma todas as alterações; uma exceção provoca `rollback`. Criar e cancelar pedidos inclui a alteração de estoque na mesma transação. Um bloqueio transacional consultivo do PostgreSQL serializa as operações da API, inclusive entre instâncias conectadas ao mesmo banco. É uma escolha simples para o escopo acadêmico; limita a concorrência e não coordena alterações SQL feitas fora da aplicação.

Os DAOs em `dao/memory` são mantidos para testes independentes do banco. A execução normal utiliza exclusivamente `dao/jdbc`. Endereços continuam pertencendo ao cliente; o JSONB no pedido preserva o endereço histórico da compra.

## Testar pelo Postman

1. Inicie o servidor e importe [docs/stuxnet.postman_collection.json](docs/stuxnet.postman_collection.json).
2. Na coleção, confira a variável `baseUrl`, cujo padrão é `http://localhost:8080/api`.
3. Abra o **Collection Runner**, selecione as pastas **01 a 07** e execute na ordem apresentada, com uma iteração.
4. Confira os resultados dos scripts de teste. A pasta **08 — ViaCEP direto** é opcional e preserva a comparação com o fornecedor da integração original.

A coleção contém **67 requisições: 64 para a StuxNet e 3 diretas ao ViaCEP**. Há cobertura de todas as combinações de método e rota implementadas, com scripts para status, JSON, proteção dos dados do cliente, frete, totais e estoque. IDs retornados são gravados automaticamente nas variáveis da coleção; os emails usam valores aleatórios. Execute as pastas anteriores antes de enviar isoladamente uma requisição que utiliza essas variáveis. Uma nova execução completa cria seus próprios recursos, inclusive se o servidor continuar ligado.

A coleção cobre os fluxos de CRUD, endereço e compra; também valida CEP inválido/inexistente, email duplicado, falta de estoque, tentativa de exclusão com histórico, cancelamento repetido, JSON inválido, campos inválidos, rota desconhecida, método incompatível e tipo de conteúdo incorreto.

Opcionalmente, com Node.js e Newman disponíveis, execute a mesma coleção pelo terminal:

```powershell
npx newman run docs/stuxnet.postman_collection.json
```

Esse comando também executa a pasta opcional do fornecedor. Os testes automatizados Java não precisam de Newman nem de acesso ao ViaCEP: utilizam um servidor HTTP local que simula exclusivamente o fornecedor.

## Rotas e contratos

Todos os caminhos abaixo são relativos a `/api`. Requisições com corpo devem enviar `Content-Type: application/json`. Respostas com corpo usam JSON em UTF-8. Uma exclusão bem-sucedida retorna `204` sem corpo.

| Método | Rota | Resultado de sucesso |
| --- | --- | --- |
| GET | `/health` | 200 — disponibilidade |
| GET | `/produtos` | 200 — lista de produtos |
| GET | `/produtos/{id}` | 200 — produto |
| POST | `/produtos` | 201 — produto criado; header `Location` |
| PUT | `/produtos/{id}` | 200 — produto atualizado |
| DELETE | `/produtos/{id}` | 204 — produto excluído |
| GET | `/clientes` | 200 — lista de clientes sem senha/hash |
| GET | `/clientes/{id}` | 200 — cliente sem senha/hash |
| POST | `/clientes` | 201 — cliente criado; header `Location` |
| PUT | `/clientes/{id}` | 200 — nome e email atualizados |
| DELETE | `/clientes/{id}` | 204 — cliente e seus endereços excluídos |
| GET | `/clientes/{id}/enderecos` | 200 — endereços do cliente |
| POST | `/clientes/{id}/enderecos` | 201 — endereço completado pelo ViaCEP |
| GET | `/cep/{cep}` | 200 — dados do CEP |
| GET | `/frete?cep=01001000&modalidade=NORMAL` | 200 — cotação |
| GET | `/pedidos` | 200 — lista de pedidos |
| GET | `/pedidos/{id}` | 200 — pedido e seus totais |
| POST | `/pedidos` | 201 — pedido criado; header `Location` |
| PUT | `/pedidos/{id}/cancelamento` | 200 — pedido cancelado |

### Exemplos de corpos JSON

Cadastro e atualização de produto (`POST /produtos` e `PUT /produtos/{id}`):

```json
{
  "nome": "Memória DDR5 16 GB",
  "descricao": "Componente de hardware",
  "categoria": "HARDWARE",
  "preco": 499.90,
  "estoque": 10
}
```

`categoria` aceita `HARDWARE` ou `PERIFERICO`. Preço deve ser positivo, ter até duas casas decimais e ser no máximo `99999999.99`. Estoque deve ser inteiro entre zero e um milhão. A atualização substitui os campos do produto; `descricao` é opcional.

Cadastro de cliente (`POST /clientes`):

```json
{"nome":"Cliente StuxNet","email":"cliente@example.com","senha":"StuxNet123!"}
```

A senha deve ter de 8 a 128 caracteres. É armazenada somente como derivação PBKDF2-HMAC-SHA256 com salt aleatório. A resposta contém apenas `id`, `nome` e `email`. Emails são normalizados em minúsculas e devem ser únicos. Para `PUT /clientes/{id}`, envie `nome` e `email`; esse endpoint não altera a senha.

Cadastro de endereço (`POST /clientes/{id}/enderecos`):

```json
{"cep":"01001-000","numero":"100","complemento":"Sala 2"}
```

O cliente deve existir. CEP e número são obrigatórios; complemento é opcional. A API obtém rua, bairro, cidade e UF pelo ViaCEP. O ID do endereço pertence ao contexto daquele cliente e é usado no pedido.

Pedido (`POST /pedidos`), utilizando IDs efetivamente retornados pelos cadastros:

```json
{
  "clienteId": 1,
  "enderecoId": 1,
  "itens": [{"produtoId": 1, "quantidade": 2}],
  "modalidadeFrete": "NORMAL"
}
```

O endereço deve pertencer ao cliente e a quantidade deve ser um inteiro positivo. O pedido aceita de 1 a 100 produtos distintos. Preço e total enviados pelo consumidor não determinam a cobrança: a aplicação consulta os produtos e calcula os valores com `BigDecimal`. O pedido registra uma cópia dos nomes, preços e endereço no momento da compra.

### Frete e regras de negócio

| Destino | Normal | Prazo normal | Expresso | Prazo expresso |
| --- | --- | --- | --- | --- |
| Bahia | R$ 22,90 | 2 dias úteis | R$ 37,90 | 1 dia útil |
| Demais estados do Nordeste (SE, PE, AL, PB, RN, CE, PI, MA) | R$ 22,90 | 4 dias úteis | R$ 37,90 | 2 dias úteis |
| Sudeste (SP, RJ, MG, ES) | R$ 34,90 | 6 dias úteis | R$ 49,90 | 4 dias úteis |
| Demais UFs | R$ 49,90 | 9 dias úteis | R$ 64,90 | 7 dias úteis |

As tarifas são regras didáticas da StuxNet. `NORMAL` é o padrão quando a modalidade não é informada. `EXPRESSO` acrescenta R$ 15,00 e reduz o prazo em dois dias, com mínimo de um dia útil. No pedido, o frete usa a UF do endereço cadastrado; não depende de uma nova chamada ao fornecedor.

Criar um pedido valida todos os itens antes de gravar o pedido ou alterar o estoque. Os serviços compartilham uma transação JDBC para confirmar pedido e estoque juntos, com rollback em caso de falha. Falta de estoque gera `409`. Cancelar devolve o estoque e mantém o histórico; repetir o cancelamento não devolve as unidades novamente. Produtos e clientes com histórico de pedidos, mesmo cancelados, não podem ser excluídos (`409`).

### Erros e processamento transversal

O Front Controller atribui `X-Request-Id`, aplica headers comuns, encontra a rota e converte exceções em um contrato único, sem enviar stack traces ao consumidor:

```json
{
  "status": 404,
  "codigo": "NAO_ENCONTRADO",
  "mensagem": "Produto não encontrado",
  "caminho": "/api/produtos/999",
  "requestId": "identificador-da-requisicao"
}
```

| Status | Uso |
| --- | --- |
| 400 | JSON, campos, identificadores ou CEP malformados |
| 404 | Rota, recurso ou CEP inexistente |
| 405 | Método incompatível com uma rota existente; inclui `Allow` |
| 409 | Email duplicado, estoque insuficiente ou exclusão que afetaria o histórico |
| 413 | Corpo acima de 100 KiB |
| 415 | Corpo enviado sem `Content-Type: application/json` |
| 500 | Falha interna inesperada |
| 502 | Falha de conexão, status inesperado ou resposta inválida do ViaCEP |
| 504 | Tempo limite na chamada ao ViaCEP |

O ViaCEP pode responder `200` com `erro: true` para um CEP inexistente; a StuxNet transforma esse caso em `404`. CEP aceita oito dígitos com hífen opcional. Os exemplos e as rotas da integração original foram mantidos e conectados aos cadastros reais.

## Arquitetura e padrões de projeto

```text
HTTP → FrontController → Router → Command → Service → DAO → JDBC → PostgreSQL
                                    ↕         ↕
                                   DTO       Model
                                              ↕
                                       ViaCEP / Frete
```

| Padrão | Implementação | Responsabilidade e evidência |
| --- | --- | --- |
| Front Controller | [FrontController.java](src/main/java/br/com/stuxnet/controller/FrontController.java), [Router.java](src/main/java/br/com/stuxnet/infra/http/Router.java) | Um único contexto HTTP `/` encaminha todas as requisições; tratamento transversal precede a seleção e a execução do comando. |
| DTO | [dto/](src/main/java/br/com/stuxnet/dto) | Contratos de entrada/saída separados das entidades. `ClienteDTO.Resposta` omite `senhaHash`; os comandos retornam DTOs para o Front Controller serializar. |
| DAO / Repository | [DAO.java](src/main/java/br/com/stuxnet/dao/DAO.java), [ProdutoDAO.java](src/main/java/br/com/stuxnet/dao/ProdutoDAO.java), [ProdutoJdbcDAO.java](src/main/java/br/com/stuxnet/dao/jdbc/ProdutoJdbcDAO.java) | Interfaces específicas de produto, cliente e pedido estendem um contrato comum. Serviços recebem essas interfaces; `dao/jdbc` isola o acesso ao PostgreSQL; `dao/memory` atende aos testes sem banco. Endereços pertencem ao agregado Cliente. |
| GoF Command | [Command.java](src/main/java/br/com/stuxnet/command/Command.java), [CadastrarProdutoCommand.java](src/main/java/br/com/stuxnet/command/produto/CadastrarProdutoCommand.java), [CriarPedidoCommand.java](src/main/java/br/com/stuxnet/command/pedido/CriarPedidoCommand.java) | Cada uma das 19 operações HTTP é encapsulada em uma classe. O roteador executa o mesmo contrato, e cada comando adapta a entrada para um serviço. |
| GoF Strategy | [FreteStrategy.java](src/main/java/br/com/stuxnet/frete/FreteStrategy.java), `FreteBahia`, `FreteNordeste`, `FreteSudeste`, `FreteOutrasRegioes` | Políticas intercambiáveis fornecem o preço e o prazo regional; `FreteService` seleciona a política pela UF. |
| GoF Decorator | [FreteExpressoDecorator.java](src/main/java/br/com/stuxnet/frete/FreteExpressoDecorator.java) | Envolve qualquer `FreteStrategy`, preserva o contrato e acrescenta preço/prazo da modalidade expressa sem alterar as políticas regionais. |

A inicialização do servidor ocorre em [StuxNetApplication.java](src/main/java/br/com/stuxnet/StuxNetApplication.java). A composição das dependências e o registro dos comandos ficam em [ConfiguracaoAplicacao.java](src/main/java/br/com/stuxnet/infra/ConfiguracaoAplicacao.java). Instâncias são construídas explicitamente. `BancoDeDados` configura o JDBC e inicializa o schema; `Transacao` envolve as operações dos serviços com commit/rollback. As interfaces DAO e os contratos HTTP foram preservados.

```text
src/main/java/br/com/stuxnet/
├── StuxNetApplication.java  # Inicialização do servidor embarcado
├── controller/
│   └── FrontController.java # Entrada HTTP e tratamento transversal
├── command/
│   ├── Command.java        # Contrato para executar uma ação
│   ├── SaudeCommand.java
│   ├── produto/            # Cadastrar, listar, buscar, atualizar e excluir
│   ├── cliente/            # CRUD de clientes e comandos de endereço
│   ├── pedido/             # Criar, listar, buscar e cancelar
│   └── entrega/            # Consultar CEP e calcular frete
├── dao/
│   ├── DAO.java            # Contrato comum de persistência
│   ├── ProdutoDAO.java
│   ├── ClienteDAO.java
│   ├── PedidoDAO.java
│   ├── jdbc/               # Implementações JDBC para PostgreSQL
│   └── memory/             # Implementações em memória para testes
├── infra/
│   ├── ConfiguracaoAplicacao.java # Composição e associação de rotas a comandos
│   ├── BancoDeDados.java   # Conexões JDBC, schema e transações
│   ├── Transacao.java      # Coordenação das operações dos serviços
│   └── http/               # Router, Requisicao e RespostaHttp
├── model/                  # Produto, Cliente, Endereco e Pedido
├── service/                # Regras de negócio, ViaCEP e senhas
├── dto/                    # Contratos externos
├── frete/                  # Strategy e Decorator
└── exception/              # Erros de negócio com status apropriado
```

### Adaptação da estrutura da Aula 15

A organização se inspira no material **“Aula prática projetos Web — Soluções arquiteturais”**, de Prof. Eder Santos, especialmente os slides 2–4 (arquitetura/pacotes), 11–14 (Command) e 18–19 (Front Controller). Os exemplos da biblioteca foram adaptados às operações da StuxNet.

| Elemento do slide | Adaptação na StuxNet |
| --- | --- |
| `Main` inicializa o servidor | `StuxNetApplication` inicializa o `HttpServer` do JDK. |
| `controller/FrontControllerServlet` | `controller/FrontController` recebe as requisições; `infra/http/Router` seleciona o comando por método e caminho. |
| `command/CadastrarLivroCommand` | `command/produto/CadastrarProdutoCommand` e demais ações de clientes, pedidos e entrega. |
| `dao/LivroDAO` e sua implementação | `ProdutoDAO`, `ClienteDAO` e `PedidoDAO`, com implementações PostgreSQL em `dao/jdbc` e versões em memória para testes. |
| `infra` reúne infraestrutura | `ConfiguracaoAplicacao` compõe as dependências; `infra/http` concentra o suporte ao protocolo. |
| `model/Livro` | Entidades do e-commerce em `model`: produto, cliente, endereço e pedido. |

A StuxNet acrescenta `service` e `dto` ao fluxo do slide: regras de estoque, preços e clientes ficam nos serviços, enquanto o JSON utiliza contratos próprios. Mantém o servidor do JDK, a integração ViaCEP e os padrões de frete, com persistência PostgreSQL via JDBC. Assim, aplica a organização da aula ao domínio do e-commerce com três padrões GoF: Command, Strategy e Decorator.

Exemplo: `POST /api/produtos` passa por `FrontController` → `Router` → `CadastrarProdutoCommand` → `ProdutoService` → `ProdutoDAO`. O comando lê `ProdutoDTO.Requisicao`; o serviço valida e persiste o produto; a resposta utiliza `ProdutoDTO.Resposta`, com status `201` e header `Location`.

## Testes e apresentação

```powershell
mvn test
```

Os testes exercitam HTTP real em portas locais livres: CRUD, DTOs sem dados sensíveis, normalização de email, todos os fretes, totais, baixa e restauração de estoque, falha de pedido sem alteração parcial, disputa pela última unidade, erros de entrada e falhas do fornecedor. A derivação de senha também é verificada. O fornecedor é simulado apenas em `src/test`, mantendo a suíte independente da disponibilidade da internet. Por padrão, a suíte básica executa 18 testes sem exigir banco, usando DAOs em memória nos testes HTTP; os três testes específicos de PostgreSQL são ignorados.

Para executar **os 21 testes com PostgreSQL real**, incluindo os testes HTTP com DAOs JDBC, reinicialização do servidor e rollback após falhas SQL:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-postgres.ps1
```

O script lê a configuração local e utiliza exclusivamente `stuxnet_test`. Cada teste cria e remove seu próprio schema; os registros da aplicação não são apagados. Para um ambiente diferente, defina `TEST_DB_URL`, `TEST_DB_USER` e `TEST_DB_PASSWORD` apontando para um banco de testes e execute `mvn test`.

O documento principal da apresentação está em **[docs/apresentacao-stuxnet.docx](docs/apresentacao-stuxnet.docx)**, em Word, com as falas divididas entre três integrantes. Os [12 slides em PowerPoint](docs/apresentacao-stuxnet.pptx) e a [versão PDF dos slides](docs/apresentacao-stuxnet.pdf) ficam como materiais complementares. A divisão é: integrante 1 nos slides 1–4 (contexto e funcionalidades), integrante 2 nos slides 5–8 (arquitetura e padrões) e integrante 3 nos slides 9–12 (PostgreSQL, testes e demonstração), com aproximadamente quatro a cinco minutos por pessoa. O roteiro detalhado está em [docs/roteiro-apresentacao-tres-integrantes.docx](docs/roteiro-apresentacao-tres-integrantes.docx).

Consulte [docs/viaCep.md](docs/viaCep.md) para os detalhes da integração com o ViaCEP e das quatro faixas regionais de frete. Para demonstrar a API, siga as pastas da coleção na ordem apresentada neste README.

Esta entrega tem escopo acadêmico: API local e aberta, sem login/autorização, pagamento real ou integração com transportadora. A senha é protegida no armazenamento e nos DTOs, mas não existe autenticação implementada. Um uso em produção exigiria desenvolver esses recursos.

Para recriar os arquivos editáveis da apresentação, instale as dependências opcionais `python-pptx` e `python-docx` e execute `python scripts/gerar-apresentacao.py`. Essas bibliotecas não são necessárias para executar a API.
