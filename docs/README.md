# Documentação — StuxNet

Este diretório reúne os diagramas de modelagem, a especificação da integração externa e a coleção de testes da API do e-commerce StuxNet.

## Conteúdo

- diagrama_de_classes.pdf — Mapeamento das entidades de domínio (Cliente, Endereco, Produto e Pedido), seus atributos e relacionamentos.
- diagrama_classe_frete.pdf — Estrutura dos padrões Strategy e Decorator aplicados ao cálculo de frete (FreteStrategy, estratégias regionais e FreteExpressoDecorator).
- diagrama_de_componentes.pdf — Apresenta a organização da aplicação em camadas (FrontController, Router, Command, Service, DTO e DAO) e a comunicação com serviços externos.
- diagrama_entidade-relacionamento.pdf — Modelo lógico de dados representando as entidades, atributos e relacionamentos do sistema (diagrama conceitual original; a implementação física atual está em `../src/main/resources/db/schema.sql`).
- viaCep.md — Documentação sobre o consumo da API pública do ViaCEP, validações de entrada e tratamento de falhas.
- stuxnet.postman_collection.json — Coleção do Postman pronta para importação, contendo as requisições de teste para todos os endpoints da aplicação.

- apresentacao-stuxnet.docx — Documento principal da apresentação em Word, dividido entre três integrantes.
- apresentacao-stuxnet.pdf — Versão PDF dos slides para leitura e projeção.
- apresentacao-stuxnet.pptx — Apresentação com 12 slides, dividida entre três integrantes, incluindo notas de fala e demonstração de persistência.
- roteiro-apresentacao-tres-integrantes.docx — Roteiro atualizado de fala por integrante, tempo sugerido e passo a passo da demonstração.

Os PDFs de modelagem são documentos anteriores à integração PostgreSQL. Para as tabelas, restrições e índices efetivamente utilizados, consulte [schema.sql](../src/main/resources/db/schema.sql) e o [README principal](../README.md).
