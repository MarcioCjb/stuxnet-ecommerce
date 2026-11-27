-- Executado na inicialização. Cria apenas objetos ausentes; não apaga dados.
CREATE TABLE IF NOT EXISTS produtos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('HARDWARE', 'PERIFERICO')),
    preco NUMERIC(10,2) NOT NULL CHECK (preco > 0),
    estoque INTEGER NOT NULL CHECK (estoque BETWEEN 0 AND 1000000)
);
CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email)),
    senha_hash TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS enderecos (
    cliente_id BIGINT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    id BIGINT NOT NULL,
    cep TEXT NOT NULL, logradouro TEXT NOT NULL, numero VARCHAR(30) NOT NULL,
    complemento VARCHAR(200) NOT NULL, bairro TEXT NOT NULL, cidade TEXT NOT NULL, uf VARCHAR(2) NOT NULL,
    PRIMARY KEY (cliente_id, id)
);
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    -- Cópia do endereço da compra; mudanças no cadastro não alteram o histórico.
    endereco JSONB NOT NULL,
    subtotal NUMERIC(20,2) NOT NULL CHECK (subtotal >= 0),
    frete NUMERIC(20,2) NOT NULL CHECK (frete >= 0),
    total NUMERIC(20,2) NOT NULL CHECK (total = subtotal + frete),
    modalidade_frete VARCHAR(10) NOT NULL CHECK (modalidade_frete IN ('NORMAL', 'EXPRESSO')),
    prazo_dias_uteis INTEGER NOT NULL CHECK (prazo_dias_uteis >= 1),
    status VARCHAR(10) NOT NULL CHECK (status IN ('CRIADO', 'CANCELADO'))
);
CREATE TABLE IF NOT EXISTS pedido_itens (
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    nome VARCHAR(150) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(10,2) NOT NULL CHECK (preco_unitario > 0),
    total NUMERIC(20,2) NOT NULL CHECK (total = preco_unitario * quantidade),
    posicao INTEGER NOT NULL,
    PRIMARY KEY (pedido_id, produto_id)
);
CREATE INDEX IF NOT EXISTS idx_pedidos_cliente ON pedidos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedido_itens_produto ON pedido_itens(produto_id);
