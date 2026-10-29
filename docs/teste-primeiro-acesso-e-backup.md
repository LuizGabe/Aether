# Resultado dos testes de primeiro acesso e backup

Data: 29/10/2026.

## Isolamento

- Perfil principal: `%USERPROFILE%\Aether\data\aether.db`, porta 38472. A aplicação aberta e o cofre principal foram preservados durante o teste.
- Primeiro acesso: `.tools\first-run-lab\zero`, porta 38482, dados do WebView e cofre próprios.
- Destino da restauração: `.tools\first-run-lab\restaurado`, porta 38483, dados do WebView e cofre próprios.
- Origem da exportação: cópia SQLite obtida com backup online da base principal, aberta em um processo transitório na porta 38484, sem credenciais herdadas. Nenhuma consulta bancária externa foi realizada.

## Execução

1. Os dois perfis novos iniciaram com `firstRun=true`, zero contas/transações e `pluggyCredentialsConfigured=false`.
2. A janela nativa do primeiro acesso mostrou a tela de boas-vindas, botões de configurar conexões/restaurar e a faixa de identificação do teste.
3. Um backup local (tamanho omitido) foi extraído pela mesma rota HTTP de exportação do aplicativo. Integridade SQLite: `ok`.
4. O arquivo foi importado pela rota multipart de restauração do destino isolado.
5. O destino foi reexportado e todas as tabelas de dados foram comparadas com a origem, por quantidade de linhas e SHA-256 do conteúdo ordenado.
6. Um arquivo inválido foi recusado com HTTP 400, preservando o histórico do destino.
7. A remoção foi executada exclusivamente na porta 38483, após checar a pasta do perfil. O aplicativo retornou a `firstRun=true`, sem credenciais configuradas. A cópia automática de segurança foi confirmada.
8. O backup reexportado foi restaurado novamente no destino. Todas as tabelas voltaram a corresponder à origem.
9. As assinaturas de todas as tabelas do perfil principal foram comparadas antes/depois: conteúdo idêntico.

## Conteúdo restaurado

- transações de uma cópia local (quantidade omitida);
- produtos bancários da cópia local (quantidade omitida);
- faturas da cópia local (quantidade omitida);
- preferências de nomes/imagens da cópia local (quantidade omitida);
- demais tabelas de categorias, regras, investimentos, snapshots e referências locais conferidas integralmente.

Credenciais não foram lidas, exibidas nem gravadas neste documento. Os testes não conectaram nem revogaram bancos. Os arquivos SQLite de laboratório contêm o histórico financeiro e devem permanecer privados.

## Validação automatizada

- 37 testes de backend passaram, incluindo cinco novos testes de exportação/remoção/restauração, confirmação obrigatória, arquivo danificado, esquema inválido, rollback e falha de gravação do backup automático.
- 18 testes de frontend passaram.
- TypeScript e `cargo check` passaram.
- O laboratório exige os parâmetros de pasta/porta; o marcador `aether-isolated-only` impede que a cópia de teste aberta sem parâmetros use silenciosamente o perfil principal.

## Arquivos

- Tutorial: `docs/tutorial-primeiro-acesso.md`.
- Backup extraído: `.tools/first-run-lab/backup-exportado.sqlite`.
- Evidência das comparações: `.tools/first-run-lab/resultado-testes.json`.
- Script da integração: `.tools/first-run-lab/test-restore.py` (destino fixado e verificado na porta 38483).
- Atalhos dos testes: `dist/Laboratorio-Aether/Abrir-primeiro-acesso.cmd` e `Abrir-restaurado.cmd`.

A versão final foi instalada também em `dist/Aether` após o usuário fechar a janela principal. Os executáveis/JARs principal e de laboratório têm SHA-256 idênticos. O perfil principal continuou íntegro e com todas as tabelas idênticas após a instalação. O marcador de perfil da interface foi tornado neutro, removendo as iniciais pessoais fixas.
