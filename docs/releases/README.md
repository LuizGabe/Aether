# Publicação de releases

A distribuição oficial atual é um instalador **Windows x64** com runtime Eclipse Temurin 21 e WebView2 offline. O app inicia sem depender de um Java instalado no sistema; a configuração Pluggy permanece com o usuário.

## Build local

Execute `pnpm install --frozen-lockfile` e `pnpm build:release` no Windows com Node 24, JDK 21, Maven e Rust MSVC estável. O resultado vai para `dist/releases/<versão>`, com instalador, SHA256SUMS.txt, BUILD-MANIFEST.json, guia e termos legais.

O runtime é baixado da API oficial Adoptium, conferido por SHA-256 e armazenado somente em pastas ignoradas. A versão e o checksum efetivamente usados ficam no manifesto. Não são empacotados `.env`, dados, logs, backups ou arquivos da pasta `.tools`.

O launcher procura `resources/runtime/bin/java.exe` ao lado do JAR. A execução de desenvolvimento mantém o fallback para Java no PATH. O instalador leva o runtime completo e o redistribuível offline do WebView2; avisos legais são preservados.

## GitHub Actions

- **Build:** compila API e frontend nos pushes/PRs para `dev` e `main`, sem testes.
- **Release:** tag `vX.Y.Z` ou execução manual com uma tag estável. Valida as versões, compila no Windows e guarda os arquivos como artifact. Um segundo job verifica SHA-256 e publica a release normal (`draft=false`, `prerelease=false`, marcada como mais recente).
- `GITHUB_TOKEN` precisa de `contents: write` somente no job de publicação; não é necessário cadastrar Client ID/Secret Pluggy nem segredos de usuários no repositório.
- Reexecutar o fluxo de uma tag existente substitui os assets conhecidos depois de novo build. O processo publica apenas uma lista explícita de nomes, nunca uma pasta arbitrária de desenvolvimento.

## Checklist de versão

1. Atualize os números no workspace, desktop, API, Cargo e Tauri. Atualize o Cargo.lock ao alterar a versão do pacote Rust.
2. Adicione `docs/releases/X.Y.Z.md` e ajuste os links estáticos da versão no README.
3. Confirme que não existem dados privados elegíveis ao Git.
4. Faça o commit semântico e envie a branch.
5. Crie e envie `vX.Y.Z`, ou use Run workflow informando uma tag existente; o checkout usa o commit dessa tag.
6. Aguarde o job Release terminar. Distribua o `-setup.exe`; os arquivos Source code do GitHub não são instaladores.

O build garante compilação/empacotamento; esta entrega não inclui teste automatizado de instalação. O instalador ainda não tem certificado de assinatura de código. A visibilidade dos downloads continua sendo a visibilidade do repositório; o fluxo não torna um projeto privado público.

## Atualização e recuperação

O instalador substitui os arquivos do programa e mantém `%USERPROFILE%\Aether\data`. Feche instâncias antigas antes de atualizar. Se necessário, preserve o instalador anterior para reinstalação e faça um backup protegido pela interface antes de migrar para outra versão.

A licença personalizada rege o Aether; não restringe direitos obrigatórios das dependências. Os avisos coletados de JavaScript/Rust, o runtime Java e os avisos internos do JAR acompanham a distribuição.
