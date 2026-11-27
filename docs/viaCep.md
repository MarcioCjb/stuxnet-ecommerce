# API externa: ViaCEP

A StuxNet usa a [ViaCEP](https://viacep.com.br/) para consultar um endereço a partir do CEP. É uma API pública e gratuita, sem necessidade de chave ou cadastro.

A consulta externa é feita neste formato:

```http
GET https://viacep.com.br/ws/{cep}/json/
```

O `{cep}` é enviado com os oito números, sem hífen. Exemplo:

```text
https://viacep.com.br/ws/01001000/json/
```

## Como a API StuxNet consulta o CEP

No lugar de o cliente acessar o ViaCEP diretamente, a aplicação disponibiliza sua própria rota:

```http
GET http://localhost:8080/api/cep/{cep}
```

Exemplo:

```http
GET http://localhost:8080/api/cep/01001-000
```

A collection do Postman verifica que um CEP válido retorna `200` e que a resposta contém os campos `localidade` e `uf`. Para o CEP `01001-000`, os valores esperados são `São Paulo` e `SP`.

## O que o ViaCEP devolve

Uma consulta válida retorna um JSON com os dados do endereço. Por exemplo:

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

A collection da StuxNet confere especificamente `localidade` e `uf` na consulta de CEP. No cadastro de endereço, ela também verifica se a resposta da aplicação contém a cidade `São Paulo`.

## Diferença entre as respostas do ViaCEP e da StuxNet

É importante separar o comportamento do serviço externo do comportamento da API da loja.

**1. CEP válido:** o ViaCEP responde `200` com os dados do endereço. A rota `/api/cep/{cep}` da StuxNet também deve responder `200`.

**2. CEP inexistente:** o ViaCEP não responde `404`. Ele retorna `200` com um JSON contendo o campo `erro` como texto:

```json
{
  "erro": "true"
}
```

A StuxNet trata esse retorno como CEP não encontrado e responde `404`. Esse comportamento é verificado na collection com o CEP `99999999`.

**3. CEP malformado:** na consulta direta, o ViaCEP responde `400` para um CEP como `123`. A collection também verifica `400` ao consultar `/api/cep/123` na StuxNet.

## Onde isso é usado

- **Consulta e cadastro de endereço:** a aplicação disponibiliza `POST /api/clientes/{clienteId}/enderecos`. O corpo da requisição recebe o CEP, o número e, opcionalmente, o complemento. Exemplo:

  ```json
  {
    "cep": "01001-000",
    "numero": "100",
    "complemento": "Sala 2"
  }
  ```

  A collection verifica o retorno `201` e a cidade `São Paulo`. Os endereços cadastrados podem ser consultados por `GET /api/clientes/{clienteId}/enderecos`. O cadastro para um cliente inexistente é testado e deve retornar `404`.

- **Cálculo do frete:** a rota `GET /api/frete` recebe o CEP como parâmetro. A modalidade expressa pode ser solicitada com `modalidade=EXPRESSO`. A collection verifica os seguintes valores:

  | Região testada | Frete normal | Prazo normal | Frete expresso | Prazo expresso |
  |---|---:|---:|---:|---:|
  | Bahia | R$ 22,90 | 2 dias úteis | R$ 37,90 | 1 dia útil |
  | Demais estados do Nordeste | R$ 22,90 | 4 dias úteis | R$ 37,90 | 2 dias úteis |
  | Sudeste | R$ 34,90 | 6 dias úteis | R$ 49,90 | 4 dias úteis |
  | Demais regiões | R$ 49,90 | 9 dias úteis | — | — |

  A collection também verifica que CEP ausente, CEP inválido e modalidade inválida retornam `400` na cotação do frete.

## Testes no Postman

Os cenários relacionados ao CEP e aos endereços estão na pasta **`04 - CEP e endereços`** da collection:

- CEP válido: consulta `01001-000`, espera `200` e confere cidade e UF.
- CEP inexistente: consulta `99999999` e espera `404` na API StuxNet.
- CEP malformado: consulta `123` e espera `400`.
- Cadastro de endereço: envia CEP, número e complemento, espera `201` e confere a cidade.
- Listagem de endereços: espera `200`.
- Cliente inexistente: tenta cadastrar um endereço para um ID que não existe e espera `404`.

A pasta **`08 - ViaCEP direto (opcional)`** consulta o fornecedor sem passar pela API StuxNet. Ela contém os três casos — CEP válido, inexistente e malformado — e pode ser desmarcada no Collection Runner. Esses testes diretos ajudam a conferir as respostas originais do ViaCEP.

As consultas de CEP válido e inexistente dependem de conexão com a internet. Para executar o fluxo completo da collection, rode as pastas **01 a 07** em ordem no Collection Runner. Os IDs necessários são salvos automaticamente pela própria collection.
