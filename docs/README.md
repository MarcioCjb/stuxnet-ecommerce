Documentação — StuxNet

Este diretório reúne os diagramas de modelagem, a especificação da integração externa e a coleção de testes da API do e-commerce StuxNet.

Conteúdo

diagrama_de_classes.pdf — Mapeia as entidades de domínio (Cliente, Endereco, Produto, Pedido), os enums e as classes responsáveis pelo cálculo de frete (Strategy e Decorator).

diagrama de componentes.pdf — Apresenta a organização da aplicação em camadas (FrontController, Router, Command, Service, DTO e DAO) e a comunicação com serviços externos.

DER.pdf — Modelo lógico de dados representando as entidades, atributos e relacionamentos do sistema (armazenados em memória via ConcurrentHashMap).

viaCep.md — Documentação sobre o consumo da API pública do ViaCEP, validações de entrada e tratamento de falhas.

stuxnet.postman_collection.json — Coleção do Postman pronta para importação, contendo as requisições de teste para todos os endpoints da aplicação.
