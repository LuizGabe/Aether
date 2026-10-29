# Distribuição e Premium — proposta em discussão

## Decisões confirmadas

- Conta individual, sem limite de dispositivos.
- Dispositivos servem para controle de instalações/sessões e revogação de acesso, não para limitar a quantidade de computadores.
- Licenciamento e atualização automática ainda não estão implementados. Este documento registra a proposta antes de escolher serviços ou fornecedores.

## Entrega local para uso diário

`pnpm build:app` gera uma compilação release e publica uma cópia em `C:\Dev\Aether\dist\Aether\Aether.exe`, com o JAR do backend na subpasta `resources`. Este caminho permanece igual nas próximas compilações para manter válido o ícone fixado na barra de tarefas.

O aplicativo abre maximizado, com os controles normais do Windows. O executável de release e seu backend não devem abrir janelas de console. A execução atual utiliza o Java já instalado no computador; a pasta inteira deve permanecer junta. O SQLite fica em `%USERPROFILE%\Aether\data\aether.db`, com caminho absoluto independente do iniciador do programa. As credenciais permanecem no cofre do usuário, fora da pasta do aplicativo.

O script também aceita `-DebugBuild` para uma cópia de depuração quando necessário. Para desenvolvimento com recarga local, continua disponível `pnpm dev`. Publicar a cópia diária é uma ação explícita; editar o código não substitui automaticamente o aplicativo diário.

## Proposta de licenciamento

A conta terá plano e permissões premium validados por um serviço remoto. O cliente recebe um comprovante assinado, com validade limitada, para continuar funcionando durante uma interrupção de internet. O prazo offline ainda deve ser decidido; sete dias é uma proposta inicial, não uma regra aprovada.

Cada instalação pode ter um identificador aleatório e uma sessão própria guardada no cofre do sistema. Evitar coletar números de disco, MAC ou outros identificadores de hardware quando o objetivo é apenas controle. Reinstalações e restaurações precisam de uma regra clara para não produzir uma lista confusa de dispositivos.

O painel da conta permite ver as instalações, último acesso e encerrar sessões. Revogar uma sessão não apaga o SQLite nem o histórico do usuário. A revogação só é percebida offline quando expira o comprovante local — existe uma escolha entre prazo offline e rapidez da revogação.

O servidor decide as permissões premium; esconder botões no React não é suficiente. Recursos executados remotamente são protegidos no servidor. Validar também as operações locais no backend/Rust, sem tratar isso como proteção absoluta contra modificação de um cliente distribuído.

Licenciamento, cobrança e sincronização dos dados bancários são responsabilidades separadas. Uma conta com vários dispositivos não terá automaticamente seus dados sincronizados entre eles; isso exige um projeto próprio de sincronização/backup, criptografia e conflitos.

Antes de distribuir a integração Pluggy comercialmente, rever o modelo de credenciais: um segredo compartilhado da empresa não deve ser embutido no executável entregue aos clientes.

## Proposta de atualizações automáticas

Usar o plugin updater do Tauri com pacotes assinados. Separar canal estável, canal de testes e execução de desenvolvimento. O aplicativo diário procura atualizações publicadas, não alterações de código não lançadas.

Fluxo proposto: editar → testar localmente → gerar release → validar em testes → publicar pacote assinado e manifesto → verificar/baixar em segundo plano → instalar ao reiniciar. Evitar interromper uma revisão ou sincronização em andamento. A instalação no Windows exige fechar o aplicativo.

Antes de instalar, fechar corretamente o backend e criar um backup consistente do banco. A nova versão precisa validar saúde e migrações. Voltar somente o executável para uma versão antiga pode ser incompatível com um SQLite já migrado; reversão de dados requer desenho e testes próprios.

Ainda faltam escolher hospedagem dos artefatos/manifesto, política de publicação, gestão da chave de assinatura, certificado de assinatura do Windows e instalador com runtime Java. A assinatura dos pacotes do updater e a assinatura de código reconhecida pelo Windows são coisas distintas.

## Fontes técnicas

- https://tauri.app/plugin/updater/
- https://keygen.sh/docs/activating-machines/
- https://keygen.sh/docs/api/cryptography/

Keygen é uma referência para modelos de ativação e certificados offline; nenhum fornecedor foi escolhido ou contratado.
