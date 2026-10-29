# Aether MVP 0.2

## Uso diário

1. Abra o desktop. O histórico importado permanece disponível sem internet.
2. Use o mês no cabeçalho para analisar receitas e despesas. Saldos de contas e posições de investimentos são os últimos valores informados, não saldos históricos daquele mês.
3. Em Transações, revise os registros sinalizados. Classifique PIX próprios, pagamentos, aplicações e estornos conforme o contexto real.
4. Em Cartões, selecione um cartão e use “Ver compras” para consultar os lançamentos associados a uma fatura importada.
5. Em Configurações, personalize nomes, vincule a conta de pagamento do cartão, crie categorias/regras e exporte um backup.

## Critérios financeiros

- Disponível soma contas bancárias em BRL. Limite de cartão não é dinheiro disponível.
- Gastos são despesas menos estornos; pagamentos de faturas, transferências próprias e aplicações/resgates são excluídos. Lançamentos pendentes, futuros ou ignorados não entram nos totais realizados.
- Fluxo de caixa considera entradas e saídas das contas bancárias, incluindo transferências, faturas e investimentos. Não equivale ao consumo.
- O patrimônio estimado usa saldos bancários + investimentos líquidos informados − saldo dos cartões. Não representa uma declaração patrimonial completa.
- Valores de moedas diferentes não são convertidos ou somados no consolidado BRL.
- A área de investimentos distingue preço unitário, quantidade, valor bruto e saldo líquido. Um dado ausente aparece como não informado.
- O histórico de posições/patrimônio começa nas sincronizações desta versão. Não se cria rentabilidade histórica a partir de um saldo isolado.
- Recorrências são sugestões por descrição, meses e valores semelhantes; parcelas futuras são estimativas baseadas nos metadados recebidos.

## Integração e segurança

O Aether importa dados somente para consulta e organização local. Não executa pagamentos, transferências ou ordens de investimento. As credenciais Pluggy permanecem no cofre do Windows; não são incluídas no backup criptografado `.aetherbackup`.

“Sincronizar” importa o estado disponibilizado pela Pluggy. A atualização dos bancos, autenticações e consentimentos dependem do Meu Pluggy e de cada instituição. A última importação e a data do último lançamento são mostradas separadamente.

As classificações são revisáveis. Categorias ambíguas do provedor não são tratadas como certeza. Regras locais podem ser excluídas e edições manuais têm prioridade. A conexão antiga ausente permanece somente nas opções avançadas para diagnóstico; o histórico foi preservado.

## Entrega e validação

Executável de uso diário: `dist/Aether/Aether.exe`, gerado por `pnpm build:app`. A janela abre maximizada. Esta entrega usa o Java já instalado no computador. Um instalador com runtime Java próprio é uma etapa futura de distribuição.

Verificações automatizadas cobrem classificação financeira, cálculos separados de caixa/consumo, valor total versus unitário dos investimentos, paginação acima de 500 lançamentos, filtros de fatura, prioridade das edições, lote atômico, contas de pagamento e backup consistente. A interface foi conferida com os dados reais; alterações manuais de teste são feitas em uma cópia isolada do SQLite.

## Próximas evoluções

- Orçamentos e metas por categoria após revisar a classificação inicial.
- Rentabilidade histórica e aportes líquidos quando houver série suficiente e cobertura dos movimentos.
- Notificações de vencimento e desvios, com preferências explícitas.
- Instalador de distribuição e atualização automática. A restauração guiada já está disponível em Configurações → Dados e backup.

## Identificação e horário dos lançamentos

A identificação usa, por prioridade, correção manual, estabelecimento (`merchant`), contraparte do pagamento conforme entrada/saída, descrição original, descrição do lançamento e informações adicionais disponíveis. Descrições genéricas não são convertidas em nomes de lojas por categoria, data ou valor. Movimentos financeiros como pagamentos de fatura e aplicações não exigem um estabelecimento comercial.

A fila “Sem identificação completa” é independente da revisão de categoria e distingue nome ausente de documento disponível sem nome. A revisão permite preencher um estabelecimento/contraparte para aquele lançamento e desfazer a substituição. A descrição original permanece disponível; a sincronização preserva a correção local.

Horários são apresentados no fuso `America/Sao_Paulo`. O horário explícito da compra tem prioridade sobre o timestamp do lançamento. Uma data sem horário, timestamp inválido ou meia-noite UTC ou local usada como marcador de data aparece como “Horário não informado”; nenhum horário é inferido. O horário do lançamento pode ser diferente do momento da compra, por isso a origem é indicada nos detalhes.

O componente de horário usa quatro cenários ilustrados em SVG: amanhecer (05–08h), dia (08–16h), entardecer (16–19h) e noite (19–05h). O astro percorre um arco conforme os minutos dentro do período solar ou noturno, com céu transparente no topo e cor na base, nuvens e estrelas. É uma representação visual, não um cálculo de nascer/pôr do sol. A data de lançamento do banco permanece nos cálculos e filtros; se o horário convertido cair em outra data local, ambas as datas são indicadas nos detalhes.


## Pasta de dados

O banco principal é `C:\Users\<usuário>\Aether\data\aether.db`. O desktop fornece esse caminho absoluto ao backend para que a abertura pelo Explorer/barra de tarefas e pelo ambiente de desenvolvimento use a mesma base. A pasta fica fora de AppData porque processos iniciados por um host MSIX, como o Codex, podem ter AppData redirecionada para uma área isolada. O log de inicialização fica em `backend.log` na mesma pasta. As credenciais continuam no cofre do Windows.

## Nomes e imagens

Configurações → Nomes e imagens permite apelidos e fotos para instituições e padrões de recorrência. Preferências ficam na tabela `visual_preferences`, incluída no backup criptografado `.aetherbackup`, separadas dos dados originais e preservadas por sincronizações. Recorrências usam uma identidade estável baseada no padrão normalizado e no produto bancário. Aliases não reclassificam transações nem confirmam assinaturas. Fotos PNG/JPG/WebP são reduzidas a 256 pixels, convertidas em PNG e armazenadas localmente. C6 e Inter têm logos empacotados para uso offline; fontes em `apps/desktop/src/assets/banks/README.md`.

## Proteção dos backups

Exportações e cópias automáticas novas exigem uma senha e usam o formato `.aetherbackup` com AES-256-GCM. Arquivos antigos SQLite podem ser importados, mas não são convertidos automaticamente. A senha é processada apenas no aplicativo local e não é armazenada. Perder a senha impede a recuperação pelo Aether. O banco ativo continua SQLite local separado da proteção dos arquivos de backup. Fluxo completo: `docs/tutorial-primeiro-acesso.md`; formato técnico: `docs/formato-backup-criptografado.md`.
