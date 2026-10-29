# Aether — tutorial de configuração do zero

Atualizado em 29/10/2026. Este documento não contém senhas, Client ID, Client Secret, tokens ou números de documentos. Os campos sensíveis devem ser informados diretamente no aplicativo; o desktop os obtém do cofre em tempo de execução.

## 1. Escolher o ambiente correto

**Instalação normal:** baixe o instalador Windows x64 na [última release](https://github.com/LuizGabe/Aether-finance-tracker/releases/latest), instale e abra pelo menu Iniciar. O Java e o instalador offline do WebView2 já estão incluídos. Os dados ficam em `%USERPROFILE%\Aether\data\aether.db`.

### Ambientes locais do mantenedor

Os caminhos abaixo são exemplos do desenvolvimento deste projeto; os laboratórios não acompanham o instalador.

**Seu aplicativo habitual:** `C:\Dev\Aether\dist\Aether\Aether.exe`. O atalho da barra de tarefas continua apontando para ele. Os dados ficam em `%USERPROFILE%\Aether\data\aether.db` e o cofre usa o serviço `Aether Pluggy`.

**Laboratório de primeiro acesso:** abra `dist\Laboratorio-Aether\Abrir-primeiro-acesso.cmd` dentro do projeto. Usa a pasta `.tools\first-run-lab\zero`, a porta 38482, seu próprio armazenamento do WebView e um serviço separado no cofre. A faixa amarela e o título “Teste isolado” identificam a janela.

**Laboratório de restauração:** abra `dist\Laboratorio-Aether\Abrir-restaurado.cmd`. Usa `.tools\first-run-lab\restaurado`, porta 38483 e outro cofre. Pode ficar aberto ao lado do primeiro laboratório e da instalação principal.

Os laboratórios persistem o que você fizer neles: reabrir o mesmo atalho não apaga o teste. Para repetir a primeira abertura, use o botão de remover dados dentro da janela com a faixa amarela. Não use esse botão no perfil principal para testar.

Os arquivos de dados e de backup do laboratório contêm dados financeiros se um backup real for restaurado. Não compartilhe essa pasta junto do executável público.

## 2. Preparar um computador novo

1. Abra a [última release](https://github.com/LuizGabe/Aether-finance-tracker/releases/latest) e baixe `Aether-v0.2.0-windows-x64-setup.exe`. Os arquivos “Source code” são destinados ao desenvolvimento.
2. Execute o instalador e escolha o idioma. O programa é instalado para o usuário atual, com Java 21 incluído e WebView2 preparado automaticamente.
3. Abra **Aether** no menu Iniciar. Depois de preparar o serviço local, uma instalação sem dados mostra “Seu espaço financeiro começa aqui”.
4. Escolha configurar conexões ou restaurar um backup. A restauração não exige internet nem credenciais.

Não há login próprio do Aether: a conexão bancária exige configurar uma aplicação Pluggy. A execução normal usa a porta local 38472 e aceita conexões apenas de `127.0.0.1`.

**Somente para builds locais portáteis:** a saída de `pnpm build:app` exige copiar `Aether.exe` junto de `resources\aether-api.jar`, além de ter Java 21+ no PATH e WebView2 no computador. Para distribuir a alguém, prefira o instalador completo gerado por `pnpm build:release`.

## 3. Configurar a integração Pluggy

1. Acesse o [Dashboard da Pluggy](https://dashboard.pluggy.ai/) com a conta responsável pela aplicação.
2. Em **Applications**, crie ou selecione a aplicação que será usada pelo Aether. Selecione os conectores em **Customization → Connectors**. O fluxo pessoal atual do Aether usa **MeuPluggy**.
3. Obtenha o **Client ID** e o **Client Secret** dessa mesma aplicação. O segredo é mostrado apenas quando é criado/gerado; mantenha-o no seu gerenciador de senhas. Para uma aplicação existente, use o segredo já guardado: gerar outro invalida o anterior e pode afetar outros dispositivos que o utilizam.
4. No Aether, entre em **Configurações → Conexões → Credenciais da integração**.
5. Digite os dois valores diretamente nos campos e clique em **Salvar credenciais**. Aguarde o estado **Configuradas**. Não cole esses valores neste tutorial, em logs, em arquivos `.cmd` ou em capturas de tela.
6. No desktop, os valores ficam no cofre do Windows do perfil selecionado. Em uma execução somente pelo navegador, ficam apenas na memória do backend da sessão. O backup de histórico não os transfere.

Referência oficial: [conectores, aplicações, Item ID e credenciais](https://docs.pluggy.ai/en/docs/guides/dashboard-connectors).

## 4. Conectar os bancos

1. Em **Configurações → Conexões**, preencha **Nome do banco para a próxima conexão**, por exemplo `C6 Bank`.
2. Clique em **Conectar instituição**. O widget abre o Meu Pluggy. Faça a autenticação na interface oficial e aceite os termos e o compartilhamento desejado.
3. Escolha o banco e conclua as validações do próprio banco, inclusive no celular quando solicitado. Não informe senhas bancárias nem códigos nos campos Client ID/Client Secret do Aether.
4. Ao retornar ao aplicativo, espere a importação. Confira a instituição, o saldo e a data do último lançamento.
5. Repita o processo para o próximo banco, alterando o nome, por exemplo `Inter`. Conecte um banco por vez para identificar corretamente cada conexão.
6. O botão **Sincronizar** importa o que a Pluggy já tem disponível. Para dados antigos ou autorização pendente, use **Gerenciar no Meu Pluggy**, atualize a conexão e conclua a validação bancária; depois importe novamente no Aether.

Se aparecer `ITEM_USER_ALREADY_EXISTS`, a conexão já existe na aplicação Pluggy. Use a recuperação do widget ou importe o Item ID existente em **Conexões → Opções avançadas**. Esse ID deve pertencer à mesma aplicação das credenciais. Não apague o histórico nem gere um novo segredo para contornar uma duplicidade.

## 5. Organizar o aplicativo

- **Contas e cartões:** o tipo de produto é importado do banco; uma conta e um cartão do mesmo banco são produtos separados. Ajuste os nomes em Configurações e vincule o cartão à conta usada para pagar a fatura. Limite de crédito não é somado ao saldo da conta corrente.
- **Nomes e imagens:** personalize as instituições e os padrões de recorrência com apelidos e fotos. Logos locais de C6 e Inter funcionam offline. Essas preferências são incluídas no backup.
- **Categorias:** crie categorias pessoais ou arquive as que não utiliza. Categorias do provedor ficam preservadas.
- **Regras locais:** escolha um trecho de descrição e a categoria ou o tipo de movimento. Edições manuais têm prioridade.
- **Transações:** revise os avisos de classificação e identificação. Uma descrição ausente do banco não é inventada. Os horários disponíveis são exibidos em Brasília; horários ausentes não são estimados.
- **Investimentos e análises:** confira as datas e os dados disponíveis. Posições incompletas são identificadas e os padrões de recorrência são sugestões, não assinaturas confirmadas.

## 6. Exportar um backup

1. Abra **Configurações → Dados e backup**.
2. Clique em **Exportar backup**, crie uma senha de pelo menos 12 caracteres e confirme-a. Salve o arquivo `.aetherbackup` em um lugar privado. Guarde a senha em um gerenciador de senhas; ela não será salva nem poderá ser recuperada pelo Aether.
3. O arquivo leva o histórico, conexões locais, contas/cartões, investimentos, faturas, categorias, regras, edições, nomes e fotos. O arquivo é criptografado com AES-256-GCM antes de ser entregue para salvar. O histórico só poderá ser restaurado com a senha correta. Backups antigos `.sqlite/.db` continuam sem criptografia: importe-os em um perfil isolado e exporte novamente no novo formato para protegê-los.
4. Credenciais do cofre e sessões de autenticação não são incluídas. O arquivo permite consultar o histórico offline; sincronizar novamente em outro perfil exige configurar credenciais da aplicação correspondente e manter o consentimento bancário válido.

## 7. Restaurar em outra instalação ou perfil

1. Abra a instalação de destino. Para este teste, use a janela de porta **38483**, nunca o perfil principal.
2. Na tela de boas-vindas, escolha **Restaurar um backup**, ou abra **Configurações → Dados e backup**.
3. Escolha o `.aetherbackup` exportado pelo Aether (até 100 MB de dados) e clique em **Restaurar backup**. Arquivos antigos `.sqlite/.db` também podem ser importados.
4. Confira se o diálogo identifica **AMBIENTE DE TESTE ISOLADO**. Informe e confirme a senha do arquivo; a mesma senha protegerá a cópia automática do destino. Se o arquivo antigo não tinha senha, crie uma agora para essa cópia automática. Digite `RESTAURAR` e confirme.
5. O Aether autentica e descriptografa o arquivo, depois valida a versão do banco, as tabelas e os vínculos antes de trocar os dados. Senha incorreta ou arquivo adulterado são recusados sem alterar o destino. Cria uma cópia automática do destino e aplica a troca em uma transação. Se houver falha na gravação, mantém os dados anteriores.
6. A tela é recarregada. Confira os totais e alguns lançamentos, além dos apelidos e imagens.
7. Configure as credenciais separadamente se quiser sincronizar. Não precisa reconectar bancos apenas para ler o histórico restaurado. O arquivo deve ser compatível com esta versão; backups de versão futura podem ser recusados.

Uma restauração **substitui**, não mescla históricos. Backups de estruturas antigas reconhecidas são migrados em uma cópia temporária; o arquivo escolhido permanece intacto.

## 8. Voltar à primeira abertura

1. Confira a janela e o perfil antes de continuar.
2. Abra **Configurações → Dados e backup → Remover todos os dados**.
3. Leia o resumo, crie e confirme uma senha para a cópia automática de segurança, digite `APAGAR` e confirme. Sem a senha você não conseguirá recuperar essa cópia.
4. O aplicativo cria uma cópia automática criptografada `.aetherbackup`, remove os registros locais, desativa a integração e remove as credenciais do cofre daquele perfil. Ao recarregar, mostra a tela de boas-vindas, como uma instalação nova.
5. Backups já exportados/automáticos são preservados, inclusive a cópia feita antes da remoção. A pasta dos backups aparece na mesma página. Se sua intenção for entregar o computador a outra pessoa, esses arquivos também precisam ser tratados separadamente.
6. A remoção local não revoga consentimentos no Meu Pluggy nem encerra sessões do navegador externo. Para revogar o acesso bancário, gerencie os compartilhamentos no Meu Pluggy.

## 9. Diagnóstico sem expor segredos

- **Não abre ou permanece preparando:** confira Java, a pasta `resources`, porta do perfil e o arquivo `backend.log` na pasta de dados daquele perfil.
- **Já existe uma janela do teste:** feche-a ou escolha outro par de pasta/porta. Um teste nunca deve reutilizar a porta 38472.
- **Sem contas após salvar credenciais:** salvar credenciais não importa dados. É necessário conectar/autorizar o banco ou importar uma conexão existente.
- **Backup recusado:** selecione o arquivo realmente exportado pelo Aether, não um PDF/CSV de extrato. Para arquivos `.aetherbackup`, confira a senha exata. Não renomeie outro tipo de arquivo para tentar importá-lo.
- **Cofre não pôde ser limpo:** a integração fica desativada no banco local e a tela informa a falha. Remova manualmente a entrada daquele perfil em **Gerenciador de Credenciais → Credenciais do Windows**, sem alterar entradas dos outros perfis.

## 10. Reproduzir os laboratórios pelo terminal

A partir de `C:\Dev\Aether`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-isolated.ps1 -Perfil PrimeiroAcesso
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-isolated.ps1 -Perfil Restaurado
```

O executável também aceita `--data-dir "C:\caminho\absoluto\dados-do-teste" --api-port 38482`. As duas opções devem ser usadas juntas. A pasta não pode ser a do perfil principal; a porta do teste deve ser diferente de 38472. O título, a faixa, o backend, os dados do WebView e o cofre pertencem ao perfil isolado.

Para preparar novamente a cópia do executável do laboratório após compilar, copie `dist\Aether\Aether.exe` e `dist\Aether\resources\aether-api.jar` para `dist\Laboratorio-Aether\app` mantendo `resources` como subpasta. Isso atualiza o programa e preserva os dados dos testes.

## 11. Proteção dos arquivos de backup

Os novos backups exportados e automáticos usam AES-256-GCM, com sal aleatório, nonce aleatório e derivação PBKDF2-HMAC-SHA-256 de 600.000 iterações. Uma senha diferente ou qualquer adulteração autenticada impede a restauração.

A senha é processada em memória pelo aplicativo local, incluindo seu backend em `127.0.0.1`; não é enviada à Pluggy, incluída no arquivo nem salva no cofre. O arquivo permanece criptografado durante armazenamento/cópia entre as instalações e é aberto apenas no aplicativo de destino com a senha. Não há recuperação ou chave mestra.

Essa proteção se aplica aos arquivos novos, não à base SQLite em uso, às cópias `.sqlite` antigas nem ao tráfego HTTP interno já existente entre a interface e o backend local. Durante validação/restauração o aplicativo usa um SQLite temporário no próprio computador e o remove ao terminar. Mais detalhes: `docs/formato-backup-criptografado.md`.
