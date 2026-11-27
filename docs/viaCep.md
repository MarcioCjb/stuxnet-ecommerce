# API externa: ViaCEP

A gente usa a ViaCEP, uma API pública e gratuita que devolve o endereço a partir de um CEP. Ela é chamada pelo backend, dentro do `ViaCepService`, e não precisa de chave nem de cadastro.

## Como chamar

```
GET https://viacep.com.br/ws/{cep}/json/
```

O `{cep}` vai só com os 8 números, sem hífen. Exemplo: `https://viacep.com.br/ws/01001000/json/`.

## O que ela devolve

Testei no Postman (a collection está em `docs/stuxnet.postman_collection.json`) e tem 3 casos:

**1. CEP válido:** volta 200 com o endereço. A gente só usa `cep`, `logradouro`, `complemento`, `bairro`, `localidade` (a cidade) e `uf`, o resto do JSON a gente ignora.

```json
{
    "cep": "01001-000",
    "logradouro": "Praça da Sé",
    "complemento": "lado ímpar",
    "bairro": "Sé",
    "localidade": "São Paulo",
    "uf": "SP"
}
```

**2. CEP que não existe:** não dá erro 404, volta **200** com um JSON esquisito. Repara que o `true` vem entre aspas, como texto:

```json
{
    "erro": "true"
}
```

**3. CEP mal formado (tipo `123`):** volta **400** com uma página HTML, não com JSON.

## Como o ViaCepService trata cada caso

O código está em `src/main/java/br/com/stuxnet/service/ViaCepService.java`.

1. Confere se o CEP contém oito dígitos, com hífen opcional. Letras e outros caracteres são rejeitados **sem nem chamar a ViaCEP**. Depois remove apenas o hífen para montar a URL.
2. Faz o GET com o `HttpClient` do próprio Java, com limite de 3 segundos.
3. Se o fornecedor respondeu um status diferente de 200 ou enviou dados inválidos, a StuxNet retorna **502**. Se a chamada excedeu o tempo limite, retorna **504**.
4. Se deu 200, usa o **Gson** para transformar o JSON num objeto `EnderecoDTO`.
5. Se o campo `erro` veio como `true` ou `"true"`, o CEP não existe e a API retorna **404**.

Erros de formato retornam **400**. O `FrontController` converte as exceções em JSON com `status`, `codigo`, `mensagem`, `caminho` e `requestId`, mantendo o mesmo contrato dos outros endpoints.

## Onde isso é usado

- **Consultar CEP e cadastrar endereço do cliente:** o `ViaCepService` completa rua, bairro, cidade e UF a partir do CEP e do número.
- **Frete:** o `FreteService` pega a UF que o `ViaCepService` devolveu e escolhe o valor e o prazo (por região, valores simulados, sem transportadora de verdade).

As quatro faixas de frete normal são: Bahia, R$ 22,90/2 dias; demais estados do Nordeste, R$ 22,90/4 dias; Sudeste, R$ 34,90/6 dias; demais UFs, R$ 49,90/9 dias. Essas regras são implementadas com Strategy. A modalidade expressa utiliza Decorator, acrescenta R$ 15,00 e reduz dois dias, com mínimo de um dia útil. Pedidos com endereço já cadastrado usam a UF salva, sem consultar o fornecedor novamente.

## Testes

Os cinco cenários de `FreteServiceTest` cobrem as quatro faixas de frete e o CEP inexistente. Foram integrados à suíte da API com um servidor HTTP local que simula o ViaCEP, evitando depender da internet durante `mvn test`. `ViaCepServiceTest` verifica o contrato do fornecedor, e `ApiIntegrationTest` exercita os endpoints, os fretes normal/expresso, os pedidos e o estoque.

Para testar o fornecedor real, use as consultas da pasta opcional `08 - ViaCEP direto` na coleção do Postman. As outras pastas validam a API StuxNet, incluindo as cotações de frete para o Nordeste.
