"""Recria o PPTX e o roteiro. Dependências opcionais: python-pptx e python-docx."""
from pathlib import Path
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from docx import Document
from docx.shared import Inches as DInches, Pt as DPt, RGBColor as DColor

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs"
slides = [
    (1, "Uma compra completa, do catálogo ao pedido", "STUXNET", [
        ("01 / CATÁLOGO", "Hardware e periféricos com preço e estoque."),
        ("02 / CLIENTE", "Cadastro e endereço completado pelo ViaCEP."),
        ("03 / COMPRA", "Frete, pedido, cancelamento e histórico.")],
     "Bom dia. Somos a equipe da StuxNet, uma API REST de e-commerce de hardware e periféricos. Nosso projeto cobre o caminho do cadastro de um produto até a criação e o cancelamento de um pedido. A aplicação controla estoque, calcula frete e completa endereços pelo ViaCEP. Nesta versão, os dados são gravados no PostgreSQL e continuam disponíveis depois de reiniciar a aplicação. Eu apresentarei o contexto e as funcionalidades; o segundo integrante explicará a arquitetura e os padrões; e o terceiro mostrará o banco, os testes e a demonstração final.",
     "Apresentar a equipe. Preencher os nomes no roteiro antes do ensaio."),
    (1, "Java simples, com persistência real", "TECNOLOGIAS", [
        ("JAVA 17+", "HttpServer do JDK e composição manual das dependências."),
        ("JSON + TESTES", "Gson nos contratos HTTP; JUnit 5 na validação automatizada."),
        ("POSTGRESQL + JDBC", "Driver pgJDBC e SQL explícito, sem ORM.")],
     "A aplicação utiliza Java 17 ou superior e o servidor HTTP que já pertence ao JDK. A composição das dependências é manual: não utilizamos um framework de aplicação. O Gson converte os contratos para JSON, enquanto o JUnit executa os testes. A persistência usa o driver JDBC oficial do PostgreSQL. Escolhemos manter SQL explícito e as interfaces DAO já existentes para adicionar o banco com poucas mudanças na estrutura. O projeto continua sendo empacotado pelo Maven em um único JAR executável. O PostgreSQL é um processo separado e precisa estar ativo para iniciar a API.",
     "Mostrar pom.xml e o comando java -jar target/stuxnet-ecommerce-1.0.0.jar."),
    (1, "As regras acompanham o fluxo de compra", "FUNCIONALIDADES", [
        ("PRODUTOS E CLIENTES", "CRUD, validação dos campos e email único."),
        ("ENDEREÇO E FRETE", "CEP validado, endereço associado ao cliente e cotação regional."),
        ("PEDIDOS E ESTOQUE", "Totais calculados no servidor; cancelamento devolve as unidades.")],
     "No catálogo, podemos cadastrar, consultar, atualizar e excluir produtos. Cada produto informa categoria, preço e quantidade disponível. O cadastro de clientes exige um email único. Para cadastrar um endereço, enviamos CEP e número; a API consulta o ViaCEP para completar os demais campos. Na compra, o consumidor informa os produtos e as quantidades, enquanto o servidor consulta os preços e calcula os totais com BigDecimal. O cancelamento preserva o histórico e restaura o estoque. Clientes e produtos com histórico de pedidos não podem ser excluídos. Essas validações já existiam e foram mantidas ao acrescentar o PostgreSQL.",
     "Apontar os contratos no README; ainda não é necessário criar um pedido."),
    (1, "Primeiro contato com a API", "DEMONSTRAÇÃO / PARTE 1", [
        ("GET /api/health", "200 confirma que o servidor HTTP está disponível."),
        ("POST /api/produtos", "201 devolve o ID do cadastro e o header Location."),
        ("GET /api/produtos/{id}", "Consultar o produto que será usado na demonstração final.")],
     "Vamos começar conferindo a disponibilidade em GET /api/health. Em seguida, cadastramos um produto no Postman e guardamos o ID que a API devolve. O status 201 indica que o cadastro foi criado; o header Location aponta para sua consulta. Enviamos então um GET usando esse mesmo ID. O produto já foi gravado no PostgreSQL, mas a confirmação visual dessa persistência ficará para o terceiro integrante, que vai reiniciar a aplicação e consultar novamente o mesmo registro. Agora passo a palavra para a explicação da arquitetura e dos padrões usados para organizar esse fluxo.",
     "Usar a coleção Postman, pastas 01 e 02, ou cadastrar um SSD de R$ 199,90 com estoque 5. Anotar o ID; não excluir esse produto antes do slide 11."),
    (2, "Uma requisição percorre camadas pequenas", "ARQUITETURA", [
        ("ENTRADA HTTP", "FrontController → Router → Command"),
        ("REGRAS E CONTRATOS", "Service valida; DTO define entrada e saída; Model representa o domínio."),
        ("PERSISTÊNCIA", "DAO → JDBC → PostgreSQL")],
     "Toda requisição entra pelo Front Controller. Ele aplica headers comuns, cria o identificador da requisição e centraliza o tratamento de erros. O Router combina o método HTTP e o caminho para selecionar um Command. O comando transforma a entrada em um DTO e chama o serviço responsável. O serviço executa as regras de negócio e depende apenas de interfaces DAO. A implementação JDBC é quem conhece o SQL e o PostgreSQL. Essa divisão permite trocar a persistência sem alterar os contratos HTTP. Na configuração, construímos os objetos explicitamente e compartilhamos o mesmo coordenador de transações entre os serviços.",
     "Mostrar ConfiguracaoAplicacao.java e acompanhar CadastrarProdutoCommand → ProdutoService → ProdutoJdbcDAO."),
    (2, "Os padrões têm responsabilidades concretas", "PADRÕES DE PROJETO", [
        ("COMMAND", "Uma classe para cada ação HTTP; 19 operações registradas."),
        ("STRATEGY", "Políticas regionais intercambiáveis de frete."),
        ("DECORATOR", "Modalidade expressa envolve a política regional e ajusta valor e prazo.")],
     "Os três padrões GoF aparecem em pontos concretos. Command encapsula cada operação da API, como cadastrar um produto ou criar um pedido, em uma classe com o mesmo contrato de execução. Strategy representa as políticas regionais de frete: o serviço seleciona uma implementação com base na UF. Decorator acrescenta a modalidade expressa sem duplicar todas as estratégias regionais. Além deles, usamos Front Controller para a entrada HTTP, DTO para os contratos e DAO para isolar a persistência. As interfaces de DAO foram preservadas; somente a configuração e a implementação do armazenamento mudaram para o PostgreSQL.",
     "Abrir Command.java, FreteStrategy.java e FreteExpressoDecorator.java."),
    (2, "Frete regional com uma extensão expressa", "STRATEGY + DECORATOR", [
        ("NORMAL / SUDESTE", "R$ 34,90 · 6 dias úteis"),
        ("EXPRESSO / SUDESTE", "R$ 49,90 · 4 dias úteis"),
        ("REGRA EXPRESSA", "+ R$ 15,00; menos 2 dias, com mínimo de 1 dia útil.")],
     "Vamos usar o Sudeste como exemplo. Na modalidade normal, a regra didática cobra trinta e quatro reais e noventa centavos, com seis dias úteis. A modalidade expressa envolve essa mesma estratégia, acrescenta quinze reais e reduz o prazo em dois dias. O resultado é quarenta e nove reais e noventa centavos, com quatro dias úteis. Há quatro estratégias: Bahia, demais estados do Nordeste, Sudeste e outras regiões. O Decorator pode envolver qualquer uma delas. No pedido, usamos a UF do endereço que já está cadastrado, portanto não precisamos fazer uma nova chamada ao ViaCEP para calcular o frete daquela compra.",
     "Opcional: GET /api/frete?cep=01001000&modalidade=NORMAL e depois EXPRESSO. A cotação por CEP depende da internet."),
    (2, "O contrato público protege os dados internos", "DTO + VALIDAÇÃO", [
        ("ENTRADA DO CLIENTE", "nome + email + senha"),
        ("RESPOSTA DO CLIENTE", "id + nome + email"),
        ("ERROS E HISTÓRICO", "400 para entrada inválida; 409 para conflitos; pedido guarda cópias da compra.")],
     "O DTO define exatamente o que entra e sai da API. No cadastro de cliente, recebemos nome, email e senha. Na resposta, devolvemos apenas id, nome e email. A senha não é gravada em texto puro: utilizamos derivação PBKDF2 com salt aleatório. O hash também não aparece nas respostas. Os erros seguem um formato comum, com status, código, mensagem e identificador da requisição. Um email duplicado ou estoque insuficiente retorna 409; uma entrada inválida retorna 400. É importante delimitar o escopo: proteger o hash e os DTOs não significa que exista login. Esta versão continua sem autenticação. O terceiro integrante agora mostrará como esses dados são persistidos.",
     "Comparar model/Cliente.java com dto/ClienteDTO.java. Não exibir credenciais do banco durante a apresentação."),
    (3, "Cinco tabelas preservam os dados do negócio", "POSTGRESQL", [
        ("CADASTRO", "clientes 1:N enderecos; produtos mantém preço e estoque."),
        ("COMPRA", "clientes 1:N pedidos; pedidos 1:N pedido_itens; itens referenciam produtos."),
        ("INTEGRIDADE", "PKs, FKs, email único e checks; endereço da compra copiado em JSONB.")],
     "A persistência utiliza cinco tabelas. Clientes e endereços representam o cadastro; produtos guarda o catálogo e o estoque. Pedidos se relaciona com clientes, e pedido_itens relaciona cada compra aos produtos. O banco valida chaves estrangeiras, unicidade de email e restrições sobre os valores. O pedido também mantém uma cópia do endereço em JSONB e os itens guardam nome e preço da compra. Assim, uma atualização no cadastro ou no catálogo não altera o histórico. O script schema.sql cria os objetos ausentes durante a inicialização sem apagar registros. As credenciais ficam em configuração local ignorada pelo Git; o código não contém uma senha fixa.",
     "Mostrar src/main/resources/db/schema.sql e os nomes das tabelas. Se abrir um cliente SQL, usar as credenciais locais sem projetar a senha."),
    (3, "Pedido e estoque são uma única transação", "COMMIT / ROLLBACK", [
        ("1 / VALIDAR", "Cliente, endereço, itens, disponibilidade e totais."),
        ("2 / GRAVAR", "Atualizar estoque e salvar pedido/itens na mesma conexão JDBC."),
        ("3 / CONFIRMAR", "Sucesso → commit. Qualquer falha → rollback de todas as alterações.")],
     "Persistir não é apenas executar INSERT. Ao criar um pedido, precisamos atualizar o estoque e gravar a compra como uma única operação. O coordenador Transacao compartilha uma conexão JDBC entre os DAOs envolvidos. Se tudo funcionar, executa commit. Se ocorrer uma exceção, executa rollback e o estoque anterior é preservado. O cancelamento usa o mesmo princípio e devolver unidades duas vezes continua sendo impedido pela regra existente. Um bloqueio transacional do PostgreSQL coordena as operações mesmo quando duas instâncias da API usam o banco. É uma solução simples para este projeto acadêmico, que prioriza a consistência e serializa as operações da aplicação.",
     "Mostrar BancoDeDados.transacionar e PedidoService.criar. Explicar que o bloqueio serializa a API e limita a concorrência."),
    (3, "Reiniciar a API preserva o cadastro", "DEMONSTRAÇÃO / PARTE 2", [
        ("ANTES", "Consultar o ID criado no slide 4 e observar seus dados."),
        ("REINICIAR", "Encerrar o Java com Ctrl+C e executar o mesmo JAR."),
        ("DEPOIS", "Consultar o mesmo ID: dados preservados no PostgreSQL.")],
     "Agora vamos comprovar a diferença em relação ao armazenamento em memória. Consultamos o produto criado no início e observamos seu ID. Encerramos o processo Java com Ctrl+C, mantendo o banco ativo, e iniciamos novamente o mesmo JAR na pasta do projeto. Ao repetir a consulta com o mesmo ID, recebemos o produto já cadastrado. Nenhum cadastro foi refeito. Os dados estão no PostgreSQL, separados do processo da aplicação. Se houver tempo, podemos criar um pedido, verificar a baixa de estoque e cancelar para demonstrar a devolução. Para uma demonstração completa, a coleção do Postman organiza esse fluxo nas pastas de 01 a 07.",
     "Executar java -jar target/stuxnet-ecommerce-1.0.0.jar a partir de stuxnet-ecommerce. Consultar GET /api/produtos/{id} com o ID anotado no slide 4; não supor que o ID seja 1."),
    (3, "Persistência validada, estrutura preservada", "TESTES E ENCERRAMENTO", [
        ("21 TESTES / POSTGRESQL", "HTTP, regras de negócio, histórico, reinício e rollback após falhas SQL."),
        ("BANCO DE TESTES ISOLADO", "stuxnet_test e um schema temporário por teste; ViaCEP simulado localmente."),
        ("ESCOPO ACADÊMICO", "API local; sem login, pagamentos reais ou integração com transportadora.")],
     "Executamos os vinte e um testes com PostgreSQL real, sem falhas. A suíte inclui as operações HTTP, o controle de estoque, os cálculos, a preservação do histórico e os erros de entrada. Os testes novos comprovam a persistência depois de reiniciar o servidor e o rollback diante de falhas SQL. Cada teste usa um schema exclusivo no banco stuxnet_test. O ViaCEP é simulado localmente nos testes, evitando dependência da internet. O README explica como iniciar o banco, configurar as credenciais e executar a aplicação. A entrega mantém a estrutura Java e os contratos da API, adicionando a persistência PostgreSQL. O escopo continua acadêmico, sem login ou pagamentos reais. Obrigado; estamos disponíveis para as perguntas.",
     "Mostrar a execução de scripts/testar-postgres.ps1: 21 testes, 0 falhas, 0 erros, 0 ignorados. Se não houver tempo, mostrar o relatório salvo do ensaio. Não anunciar resultados Newman que não foram executados nesta entrega.")
]

BG = '0B1220'; PANEL = '152236'; WHITE = 'F4F7FB'; MUTED = 'B3C2D4'
COLORS = {1:'43DEC5', 2:'6BAAFF', 3:'FFB567'}
prs = Presentation(); prs.slide_width = Inches(13.333); prs.slide_height = Inches(7.5)

def box(slide, x, y, w, h, color, rounded=False):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE if rounded else MSO_SHAPE.RECTANGLE, Inches(x), Inches(y), Inches(w), Inches(h))
    shape.fill.solid(); shape.fill.fore_color.rgb = RGBColor.from_string(color); shape.line.fill.background()
    if rounded: shape.adjustments[0] = 0.10
    return shape

def text(slide, value, x, y, w, h, size=24, color=WHITE, bold=False):
    shape = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = shape.text_frame; tf.word_wrap = True
    tf.margin_left = tf.margin_right = Inches(0.01); tf.margin_top = tf.margin_bottom = Inches(0.01)
    for i, line in enumerate(value.split('\n')):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph(); p.text = line
        p.font.name = 'Aptos'; p.font.size = Pt(size); p.font.bold = bold; p.font.color.rgb = RGBColor.from_string(color)
        p.space_after = Pt(8)
    return shape

for idx, (speaker, title, label, cards, notes, action) in enumerate(slides, 1):
    slide = prs.slides.add_slide(prs.slide_layouts[6]); slide.background.fill.solid(); slide.background.fill.fore_color.rgb = RGBColor.from_string(BG)
    accent = COLORS[speaker]
    box(slide, 0, 0, 0.15, 7.5, accent)
    text(slide, 'STUXNET / E-COMMERCE', .6, .35, 8, .4, 13, MUTED, True)
    text(slide, f'INTEGRANTE {speaker}  /  {idx:02d}', 10.2, .35, 2.6, .4, 13, accent, True)
    text(slide, label, .65, 1.03, 12, .4, 14, accent, True)
    text(slide, title, .65, 1.55, 12, 1.2, 36 if len(title)<48 else 32, WHITE, True)
    if idx == 1:
        text(slide, 'API REST  ·  JAVA 17+  ·  POSTGRESQL', .65, 2.7, 12, .5, 20, MUTED)
    for j, (heading, body) in enumerate(cards):
        x = .65 + j * 4.08
        box(slide, x, 3.30, 3.88, 3.0, PANEL, True)
        box(slide, x+.22, 3.56, .45, .05, accent)
        text(slide, heading, x+.22, 3.78, 3.43, .6, 16, accent, True)
        text(slide, body, x+.22, 4.50, 3.43, 1.65, 20, WHITE)
    text(slide, 'CONTEXTO E FLUXO' if speaker==1 else 'ARQUITETURA E PADRÕES' if speaker==2 else 'BANCO, DEMONSTRAÇÃO E TESTES', .65, 6.85, 9, .35, 12, MUTED)
    box(slide, 10.8, 6.96, 1.9, .035, PANEL)
    box(slide, 10.8, 6.96, 1.9*idx/12, .035, accent)
    slide.notes_slide.notes_text_frame.text = f'INTEGRANTE {speaker} — SLIDE {idx}\nTempo sugerido: 50–75 segundos (demonstrações podem exigir mais).\n\nFALA SUGERIDA\n{notes}\n\nAÇÃO / APOIO\n{action}'
prs.save(OUT/'apresentacao-stuxnet.pptx')

doc = Document(); sec=doc.sections[0]; sec.top_margin=DInches(.7); sec.bottom_margin=DInches(.65)
normal=doc.styles['Normal']; normal.font.name='Calibri';normal.font.size=DPt(11);normal.paragraph_format.space_after=DPt(7)
for sty in ['Title','Heading 1','Heading 2']:
    doc.styles[sty].font.color.rgb=DColor.from_string('153D61')
doc.add_heading('STUXNET', 0); doc.add_paragraph('Apresentação do projeto para três integrantes', 'Subtitle')
doc.add_paragraph('API REST de hardware e periféricos · Java 17+ · PostgreSQL')
doc.add_heading('Divisão da apresentação', 1)
t=doc.add_table(rows=1,cols=3); t.style='Light Shading Accent 1'
for c,tx in zip(t.rows[0].cells,['Integrante','Slides e tema','Tempo']):c.text=tx
for person,span,theme in [(1,'1–4','Contexto e funcionalidades'),(2,'5–8','Arquitetura e padrões'),(3,'9–12','Banco, demonstração e testes')]:
    c=t.add_row().cells;c[0].text=f'{person} — Nome: __________';c[1].text=f'{span}: {theme}';c[2].text='4–5 min'
doc.add_paragraph('Duração total sugerida: 12–15 minutos. As falas são apoio ao ensaio e também estão nas notas dos slides. Os nomes devem ser preenchidos pela equipe.')
doc.add_heading('Preparação conjunta',1)
for tx in [
'Na pasta stuxnet-ecommerce, executar scripts/iniciar-postgres.ps1, mvn clean verify e java -jar target/stuxnet-ecommerce-1.0.0.jar. Manter o terminal da API acessível.',
'Importar docs/stuxnet.postman_collection.json e conferir baseUrl = http://localhost:8080/api. Ensaiar as pastas 01 a 07 em ordem; a pasta 08 contém chamadas opcionais ao fornecedor.',
'Executar scripts/testar-postgres.ps1 antes da apresentação e guardar o resumo: 21 testes, 0 falhas, 0 erros e 0 ignorados.',
'O banco preserva os dados ao reiniciar. Não esperar listas vazias nem IDs começando em 1. Usar os IDs realmente retornados pela API e emails novos.',
'Combinar quem controla o computador. Para mostrar persistência, criar um produto no slide 4 e consultar o mesmo ID após reiniciar a API no slide 11.',
'Não projetar database.properties nem .local/postgres-admin.properties. As credenciais são locais e não devem ser compartilhadas.',
'Para as consultas externas ao vivo, conferir a internet e o ViaCEP. Se o fornecedor falhar, mostrar os testes locais; pedidos com endereço existente usam a UF salva.'
]:doc.add_paragraph(tx,style='List Number')
for speaker in [1,2,3]:
    doc.add_page_break();doc.add_heading(f'Integrante {speaker}',0);doc.add_paragraph('Nome: ____________________________________  |  Tempo: 4–5 minutos')
    for idx,(who,title,label,cards,notes,action) in enumerate(slides,1):
        if who!=speaker:continue
        doc.add_heading(f'Slide {idx} — {title}',1)
        doc.add_paragraph(notes)
        p=doc.add_paragraph();p.add_run('Ação de demonstração: ').bold=True;p.add_run(action)
doc.add_page_break();doc.add_heading('Apoio à demonstração',0)
doc.add_heading('Persistência sem depender do ViaCEP',1)
for tx in ['GET /api/health → 200.', 'POST /api/produtos com o JSON abaixo → 201. Anotar id e consultar GET /api/produtos/{id}.', 'Encerrar somente a API com Ctrl+C. O PostgreSQL continua ligado.', 'Na pasta stuxnet-ecommerce, executar novamente java -jar target/stuxnet-ecommerce-1.0.0.jar.', 'Consultar o mesmo ID → 200, com os dados preservados. Nenhum novo cadastro é necessário.']:
    doc.add_paragraph(tx,style='List Number')
doc.add_paragraph('{"nome":"SSD demonstracao", "descricao":"Teste de persistencia", "categoria":"HARDWARE", "preco":199.90, "estoque":5}')
doc.add_heading('Pedido opcional com ViaCEP disponível',1)
doc.add_paragraph('Cadastrar um cliente com email novo e um endereço em São Paulo/SP. Com o produto de R$ 199,90 e estoque 5, comprar 2 unidades: subtotal R$ 399,80 + frete normal R$ 34,90 = total R$ 434,70. O estoque deve passar para 3. Cancelar devolve o estoque para 5; repetir o cancelamento mantém 5. Utilize os IDs retornados, sem supor valores fixos.')
doc.add_heading('Perguntas frequentes',1)
for q,a in [
('É Spring ou outro framework?', 'Não. O servidor é HttpServer do JDK; Gson converte JSON e JDBC acessa PostgreSQL.'),
('Existe login?', 'Não. Há proteção do hash e dos DTOs, mas autenticação e autorização não foram implementadas.'),
('Por que manter dao/memory?', 'Para executar a suíte básica sem exigir banco. A execução normal usa os DAOs JDBC.'),
('O banco foi instalado como serviço?', 'Sim. PostgreSQL 18 está em C:/Program Files/PostgreSQL/18, com dados em C:/ProgramData/PostgreSQL/18/data e serviço postgresql-x64-18 com início automático no Windows. Os scripts de iniciar/parar controlam o serviço e exigem administrador quando é necessário alterar seu estado.'),
('O que acontece se gravar o pedido falhar?', 'A operação executa rollback e desfaz inclusive a alteração de estoque. Os testes provocam falhas SQL para validar isso.'),
('Todos os diagramas PDF foram atualizados?', 'Não. São documentos conceituais anteriores. A estrutura física atual e suas restrições estão em src/main/resources/db/schema.sql.'),
('Quanto pode executar ao mesmo tempo?', 'As operações da API são serializadas por um bloqueio transacional do PostgreSQL. Isso simplifica a consistência no escopo acadêmico e limita a concorrência.')]:
    p=doc.add_paragraph();p.add_run(q+' ').bold=True;p.add_run(a)
doc.save(OUT/'roteiro-apresentacao-tres-integrantes.docx')
# Documento principal solicitado, mantido exclusivamente na pasta docs.
doc.save(OUT/'apresentacao-stuxnet.docx')
print('Gerados: apresentacao-stuxnet.docx, roteiro e 12 slides; todos na pasta docs.')
