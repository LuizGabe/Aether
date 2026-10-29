# Arquivo de backup criptografado do Aether

Formato 1, implementado em 29/10/2026. Extensão `.aetherbackup`.

## Proteção e limites

A criptografia ocorre no aplicativo de origem e a descriptografia no aplicativo de destino. O arquivo copiado/armazenado contém apenas o cabeçalho público e o conteúdo autenticado cifrado. As instalações derivam a chave da senha escolhida pelo usuário; não há chave mestra, senha embutida, recuperação, servidor de chaves ou envio para Pluggy/nuvem.

O endpoint confiável é o aplicativo local completo (interface e backend). A senha é enviada em corpo de requisição à API de loopback e utilizada em memória, sem ser armazenada, colocada em URL ou registrada em log. Esta implementação protege o arquivo durante a transferência entre instalações; não pretende tornar o backend local incapaz de ler o histórico que ele já gerencia.

O banco ativo e arquivos legados SQLite não foram criptografados nem removidos retroativamente. O tráfego HTTP interno em loopback não foi convertido para TLS. SQLite temporário é utilizado apenas no dispositivo durante snapshot/validação/migração e removido ao terminar; não se promete apagamento físico seguro de disco/SSD.

## Construção

- AES-256-GCM com etiqueta de autenticação de 128 bits.
- PBKDF2-HMAC-SHA-256, 600.000 iterações e chave de 256 bits.
- Sal de 16 bytes e nonce de 12 bytes, novos a cada arquivo, gerados por SecureRandom.
- Cabeçalho completo autenticado como AAD. Não contém nomes, contas, valores ou senha.
- Senha de 12 a 1024 caracteres, com espaços e Unicode preservados exatamente. Não há política de caracteres especiais; uma frase longa exclusiva é adequada.
- Limite de conteúdo de 100 MiB; sobrecarga do envelope: 56 bytes.

| Offset | Tamanho | Campo |
|---:|---:|---|
| 0 | 8 | ASCII `AETHBK01` (assinatura e versão) |
| 8 | 4 | Número de iterações, inteiro big-endian (600000) |
| 12 | 16 | Sal PBKDF2 |
| 28 | 12 | Nonce GCM |
| 40 | variável | Conteúdo AES-GCM cifrado, seguido da etiqueta de 16 bytes |

O AAD são os primeiros 40 bytes. Os parâmetros de iteração são verificados antes de derivar a chave, impedindo que um cabeçalho malformado solicite trabalho arbitrário. O código utiliza as primitivas JCA/JCE do Java; não implementa AES, GCM ou PBKDF2 manualmente.

## Fluxos

1. Exportar exige uma senha. A API entrega somente o envelope cifrado, com Cache-Control: no-store e nome `.aetherbackup`.
2. Restaurar exige senha e a confirmação RESTAURAR. Primeiro a autenticação GCM precisa passar; depois o SQLite é validado/migrado em uma cópia temporária. Somente então a cópia automática protegida é criada e os dados são trocados em transação.
3. Arquivos SQLite antigos ainda podem ser importados. A senha informada protege a cópia automática do destino, mesmo que o arquivo de origem não tenha proteção.
4. Remover exige APAGAR e uma senha para a cópia automática cifrada. Se não for possível proteger/salvar a cópia, os dados não são apagados.
5. Senha incorreta, arquivo alterado e corrupção retornam um erro genérico de autenticação; o destino permanece intacto.
6. A rota legada GET /api/mvp/backup não exporta mais SQLite sem senha e responde HTTP 410. A rota de exportação atual é POST /api/data/export, com password apenas no corpo.

Backups automáticos usam a senha da operação. O programa não a guarda: perder a senha significa perder acesso a esse arquivo. Cópias antigas sem criptografia devem ser protegidas por reexportação; não foram apagadas automaticamente.

## Referências

- [OWASP — Cryptographic Storage](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html): modos autenticados e AES.
- [OWASP — Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html): referência para o custo PBKDF2-HMAC-SHA-256.
- [Oracle — GCMParameterSpec](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/crypto/spec/GCMParameterSpec.html): nonce e etiqueta GCM.

## Validação realizada

- 42 testes de backend e 18 testes de frontend passaram.
- Teste do JAR final em dois processos/portas isolados: exportação protegida, senha errada e adulteração recusadas sem perda, restauração do arquivo em outra instalação, compatibilidade com legado, remoção com cópia automática cifrada e nova restauração.
- Todas as tabelas restauradas conferidas por conteúdo; transações de uma cópia local (quantidade omitida) preservadas. As tabelas do perfil principal permaneceram idênticas às do início do teste.
- O arquivo protegido foi recusado por um leitor SQLite padrão; a rota legada de exportação sem senha retornou HTTP 410.
- Senha aleatória gerada apenas na memória do teste, sem exibição ou persistência. Os arquivos da execução isolada são fixtures de teste, não backups de recuperação destinados ao usuário.
- Resultado resumido: `.tools/crypto-backup-tests/resultado.json`.
