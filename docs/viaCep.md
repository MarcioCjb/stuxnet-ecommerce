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

1. Tira tudo que não for número (o hífen, por exemplo) e confere se sobraram 8 dígitos. Se não, lança erro **sem nem chamar a ViaCEP**.
2. Faz o GET com o `HttpClient` do próprio Java, com limite de 3 segundos.
3. Se a ViaCEP respondeu 400 ou qualquer status diferente de 200, lança erro.
4. Se deu 200, usa o **Gson** para transformar o JSON num objeto `EnderecoDTO`.
5. Se o campo `erro` veio como `"true"`, o CEP não existe e o service lança erro.

Todos os erros saem como `IllegalArgumentException`, cada um com uma mensagem dizendo o que aconteceu.

## Onde isso é usado

- **Consultar CEP e cadastrar endereço do cliente:** o `ViaCepService` completa rua, bairro, cidade e UF a partir do CEP e do número.
- **Frete:** o `FreteService` pega a UF que o `ViaCepService` devolveu e escolhe o valor e o prazo (por região, valores simulados, sem transportadora de verdade).

## Testes

Rodando `mvn test` são 8 testes JUnit, todos passando: 3 do `ViaCepServiceTest` (CEP válido, inexistente e com menos de 8 dígitos) e 5 do `FreteServiceTest` (as 4 faixas de frete e o CEP inexistente). Eles batem na ViaCEP de verdade, então precisam de internet.