<div align="center">

![Aether — seu dinheiro, em perspectiva](docs/images/aether-hero.svg)

[![Windows x64](https://img.shields.io/badge/Windows-x64-214f3e?style=flat-square)](https://github.com/LuizGabe/Aether-finance-tracker/releases/latest)
[![Versão 0.2.0](https://img.shields.io/badge/vers%C3%A3o-0.2.0-78945f?style=flat-square)](https://github.com/LuizGabe/Aether-finance-tracker/releases)
[![Uso pessoal](https://img.shields.io/badge/licen%C3%A7a-uso_pessoal-d4dfb8?style=flat-square)](LICENSE)

**Contas, cartões e investimentos. Uma visão clara das suas finanças, no seu computador.**

[Baixar o aplicativo](https://github.com/LuizGabe/Aether-finance-tracker/releases/latest) · [Primeiros passos](docs/tutorial-primeiro-acesso.md) · [Como compilar](#desenvolvimento) · [Licença](LICENSE)

</div>

## Clareza para o dia a dia

O Aether reúne seu histórico financeiro em um aplicativo desktop para Windows. Contas correntes, crédito e investimentos têm seu próprio lugar, para que saldos, limites e gastos façam sentido na mesma análise.

Os dados importados ficam em um SQLite local. Você pode consultar o histórico offline, revisar a identificação das transações e organizar categorias, regras e recorrências sem depender de uma aba aberta no navegador.

![Visão geral ilustrativa do Aether, com dados fictícios](docs/images/aether-overview.svg)

<sub>Ilustração da interface. Todos os valores são fictícios; nenhum dado bancário real foi utilizado nas imagens deste README.</sub>

## O que você encontra

| Área | Para que serve |
|---|---|
| **Visão geral** | Acompanhar saldos, receitas, gastos e instituições em um só lugar. |
| **Contas** | Entender o dinheiro disponível e consultar o fluxo de caixa. |
| **Cartões** | Separar limite utilizado, faturas, compras e conta de pagamento. |
| **Investimentos** | Consultar posições e movimentos conforme disponibilizados pela instituição. |
| **Transações** | Buscar, filtrar, exportar e revisar identificação, categoria e tipo de movimento. |
| **Momento do dia** | Visualizar horários disponíveis com marcadores de sol e lua. |
| **Recorrências** | Reconhecer padrões e adicionar nomes e imagens que façam sentido para você. |
| **Configurações** | Gerenciar conexões, produtos, categorias, regras, backups e preferências. |

Os resultados dependem da cobertura e atualização da instituição. Campos ausentes são identificados; o aplicativo não inventa estabelecimentos, horários ou posições financeiras.

## Baixar e começar

1. Acesse a [última release](https://github.com/LuizGabe/Aether-finance-tracker/releases/latest).
2. Baixe **`Aether-v0.2.0-windows-x64-setup.exe`** e execute o instalador.
3. Abra o Aether pelo menu Iniciar. Se desejar, fixe o ícone na barra de tarefas.
4. Escolha configurar suas conexões ou restaurar um backup existente.

**O instalador inclui Java 21 e o instalador offline do Microsoft WebView2.** Não é necessário instalar Java, Node.js, Maven ou Rust para usar o aplicativo.

A configuração dos bancos exige internet, uma aplicação Pluggy e a autorização do titular no fluxo oficial do banco/Meu Pluggy. As credenciais são informadas no próprio aplicativo e guardadas no cofre do Windows. Elas não vêm no download e não são incluídas nos backups.

> Os arquivos automáticos **Source code (zip/tar.gz)** do GitHub são o código-fonte. Para instalar, escolha o arquivo **`-setup.exe`**. O acesso ao download segue as permissões deste repositório, inclusive quando ele estiver privado.

O instalador desta versão não possui certificado de assinatura de código. Veja as notas da release e confira o `SHA256SUMS.txt` antes de instalar.

[Leia o tutorial completo de configuração →](docs/tutorial-primeiro-acesso.md)

## Seu histórico, protegido e portátil

![Fluxo ilustrado de backup criptografado entre instalações](docs/images/aether-backup.svg)

Em **Configurações → Dados e backup**, você pode exportar o histórico em um arquivo **`.aetherbackup` protegido por senha com AES-256-GCM**. A senha é utilizada pelo aplicativo local e não é salva ou enviada à Pluggy.

A restauração valida a integridade do arquivo antes de substituir os dados. Uma cópia automática, também criptografada, é criada antes de restaurar ou voltar ao primeiro acesso.

- Guarde a senha: o Aether não possui recuperação nem chave mestra.
- Backups SQLite antigos continuam importáveis, mas não são criptografados retroativamente.
- O arquivo leva histórico e preferências; credenciais e sessões de autenticação permanecem fora dele.
- A proteção dos backups é separada do banco SQLite ativo, que permanece local.

[Conheça o formato e seus limites →](docs/formato-backup-criptografado.md)

## Privacidade e armazenamento

| Informação | Onde fica |
|---|---|
| Histórico e preferências | `%USERPROFILE%\Aether\data\aether.db` |
| Credenciais Pluggy no desktop | Cofre de credenciais do Windows |
| Cópias automáticas | Subpasta `backups` do perfil, em formato criptografado |
| Dados de desenvolvimento isolado | Pasta e porta explicitamente escolhidas para cada perfil |

O instalador distribui somente o programa, o runtime e os avisos legais. Não leva dados de usuários, contas, credenciais, capturas ou backups do desenvolvimento. Atualizar a instalação preserva a pasta de dados do usuário.

## Desenvolvimento

O projeto é um workspace com **React + TypeScript + Tauri 2** no desktop e **Java 21 + Spring Boot + SQLite** no serviço local.

```text
apps/
  api/                 Serviço local e migrações
  desktop/             Interface React e shell Tauri
scripts/               Build, empacotamento e perfis isolados
docs/                  Guias, decisões e notas de versão
```

Para desenvolver no Windows, instale **Node.js 24**, **pnpm 10**, **JDK 21**, **Maven 3.9+**, **Rust estável MSVC** e os [pré-requisitos do Tauri](https://v2.tauri.app/start/prerequisites/).

```powershell
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

A primeira compilação pode precisar de internet para baixar dependências. As credenciais continuam sendo configuradas em tempo de execução; não crie arquivos com segredos para acompanhar o código.

| Comando | Resultado |
|---|---|
| `pnpm dev` | Desktop em desenvolvimento. |
| `pnpm dev:api` | Serviço local pelo Maven. |
| `pnpm build` | Backend e frontend compilados. |
| `pnpm build:app` | Executável local em `dist/Aether`, usando o Java do computador. |
| `pnpm build:release` | Instalador completo, Java incluído, em `dist/releases/0.2.0`. |

O build de release obtém um runtime Eclipse Temurin 21 oficial, verifica seu checksum e preserva suas licenças. Reúne também os avisos das dependências, gera o instalador NSIS e cria os checksums dos arquivos distribuídos.

### Perfis isolados

O desktop aceita um par explícito de argumentos para manter dados, backend, WebView e cofre separados:

```powershell
Aether.exe --data-dir "C:\AetherSandbox\perfil" --api-port 38482
```

As duas opções devem ser usadas juntas. A porta de teste não pode ser a porta principal, `38472`, e a pasta deve ser diferente da pasta normal de dados. A janela apresenta uma faixa de identificação do perfil isolado.

## Releases

O workflow [Build](.github/workflows/development.yml) compila API e interface em pushes e pull requests. O workflow [Release](.github/workflows/deploy.yml) monta o instalador no Windows e publica uma **release estável normal**, com instalador, checksums, manifesto do build, guia rápido e licença.

Para uma próxima versão:

1. Sincronize a versão em `package.json`, `apps/desktop/package.json`, `apps/api/pom.xml`, `Cargo.toml` e `tauri.conf.json`.
2. Atualize as notas em `docs/releases/X.Y.Z.md` e faça um commit semântico.
3. Crie e envie uma tag estável `vX.Y.Z`. O fluxo também pode ser iniciado manualmente pelo GitHub Actions.

```powershell
git tag v0.2.0
git push origin dev
git push origin v0.2.0
```

A publicação só acontece após o build e a conferência dos checksums. Os workflows desta entrega validam compilação e empacotamento; não executam uma bateria de testes nem instalam o aplicativo automaticamente. Atualizações automáticas dentro do app ainda não estão disponíveis.

[Detalhes do processo de publicação →](docs/releases/README.md)

## Licença e propriedade intelectual

**O uso pessoal, privado e não comercial está liberado. Uso empresarial, profissional ou em nome de uma organização exige autorização específica por escrito.**

O código disponibilizado não transfere a propriedade intelectual do Aether e não autoriza sua revenda, sublicenciamento ou exploração comercial. Esta é uma licença personalizada de uso pessoal; não é uma licença de código aberto aprovada pela OSI.

Consulte a [licença completa](LICENSE). Bibliotecas, runtimes, fontes e marcas de terceiros mantêm seus próprios termos, descritos nos [avisos de terceiros](THIRD_PARTY_NOTICES.md).

## Parcerias e próximos capítulos

O Aether está aberto a conversas sobre parcerias estratégicas, continuidade do produto e novas formas de ampliar seu alcance. Se você enxerga um próximo capítulo para esta tecnologia, os canais do [mantenedor](https://github.com/LuizGabe) estão abertos para uma conversa.
