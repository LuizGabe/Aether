# Estudo do aprendizado — integração com Pluggy

## Objetivo
Validar, em modo de teste, se as credenciais `CLIENT_ID` e `CLIENT_SECRET` permitiam acessar dados reais da conta no Pluggy e confirmar especialmente a presença de `bills` e transações de cartão.

## O que foi implementado
O script em [main.py](main.py) passou a:
- carregar variáveis do arquivo `.env`
- autenticar no Pluggy com `CLIENT_ID` e `CLIENT_SECRET`
- buscar a API key via `POST /auth`
- consultar `GET /connectors`
- consultar `GET /items/{id}` quando há `ITEM_ID`
- consultar `GET /accounts?itemId=...`
- consultar `GET /accounts/{id}`
- consultar `GET /bills?accountId=...`
- consultar `GET /v2/transactions?accountId=...`

## Variáveis usadas no teste
Foram usadas estas variáveis do `.env`:
- `CLIENT_ID`
- `CLIENT_SECRET`
- `ACCOUNT_ID`
- `ACCOUNT_ID_2`
- `ACCOUNT_ID_3`
- `ITEM_ID`
- `ITEM_ID_2`

## Aprendizados principais
### 1. Existem dois tipos bem diferentes de conta
- `BANK`: conta bancária normal, com transações de débito e crédito comuns.
- `CREDIT`: conta de cartão, com `bills` e metadata específica de cartão.

### 2. `bills` vêm da conta de cartão
A consulta de `bills` retornou dados para a conta do tipo `CREDIT`, confirmando que o Pluggy está trazendo faturas do cartão.

### 3. As transações de cartão carregam metadata própria
Nas transações do cartão apareceu `creditCardMetadata`, incluindo informações como:
- `billId`
- `cardNumber`
- `payeeMCC`
- `installmentNumber`
- `totalInstallments`
- `totalAmount`

Isso confirma que as transações de cartão de crédito estão sendo recuperadas corretamente.

### 4. A conta bancária não traz `bills`
Na conta do tipo `BANK`, foram vistas transações normais, mas sem `creditCardMetadata` e sem faturas associadas.

### 5. A conta bancária trouxe transações de débito e crédito
Na conta bancária principal, houve:
- transações do tipo `DEBIT`
- transações do tipo `CREDIT`
- movimentos PIX
- pagamento de fatura do cartão

### 6. Alguns IDs do `.env` não estavam válidos no ambiente atual
- `ITEM_ID_2` retornou `item not found`
- `ACCOUNT_ID_2` retornou `Account not found`

## Conclusão prática
A integração está funcionando e o Pluggy está retornando:
- conta bancária com transações normais
- conta de cartão com `bills`
- transações de cartão com metadata específica

Ou seja, o teste confirmou que os dados de cartão de crédito realmente vêm pela API.

## Resumo do que vale guardar
- use `ACCOUNT_ID` de conta `CREDIT` para buscar `bills`
- use `ACCOUNT_ID` de conta `BANK` para buscar transações bancárias normais
- use `creditCardMetadata` para identificar transações de cartão
- `bills` são o ponto principal para faturas de cartão

## Observação de segurança
Não é recomendável deixar `CLIENT_SECRET` exposto em arquivos versionados ou compartilhados.
