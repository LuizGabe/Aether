# Análise da inicialização — 28/10/2026

## Medições locais

Java 26.0.2.1, JAR de uso diário, cópia SQLite com lançamentos de uma cópia local (quantidade omitida), portas 38640–38643. Duas execuções por configuração, sequenciais e sem limpar o cache do Windows. Os tempos abaixo vão da criação do processo à resposta completa de `/api/mvp/workspace`; não incluem WebView, renderização ou a espera de repetição da interface.

| Configuração | Execução 1 | Execução 2 |
|---|---:|---:|
| Atual | 7.796 s | 5.109 s |
| Somente JVM TieredStopAtLevel=1 | 7.591 s | 4.277 s |
| Sem autoconfiguração Hibernate/JPA | 4.245 s | 3.278 s |
| Sem Hibernate/JPA + JVM nível 1 | 2.818 s | 2.832 s |

## Diagnóstico

- Nove inicializações registradas no log de uso diário: mediana de 4,264 s no Spring Boot, mínimo de 3,697 s e máximo de 8,879 s. O custo completo do processo é um pouco maior.
- A interface repete a busca a cada 4.000 ms. Quando o serviço fica pronto entre tentativas, o usuário pode esperar quase quatro segundos adicionais.
- Quatro interfaces JpaRepository existem, mas não foram encontradas injeções/consumidores delas. WorkspaceService e PluggySyncService usam JdbcTemplate. Hibernate e a fábrica JPA ainda são inicializados em cada abertura.
- As leituras completas dos dados também têm custo: no ensaio variaram com JIT/cache. A combinação levou 2,818 s e 2,832 s até a primeira resposta. As transações de uma cópia local (quantidade omitida) foram retornadas em todos os casos.

## Próximas mudanças propostas

1. Verificar prontidão real por `/api/health`, com tentativas curtas sequenciais e cancelamento ao sair. Buscar o workspace uma vez que API e banco estejam prontos. Evitar requisições iniciais concorrentes e o intervalo fixo de quatro segundos.
2. Desativar a autoconfiguração JPA/Hibernate no desktop e manter transações JDBC. A suíte existente foi executada com essas exclusões: 32 testes passaram, incluindo sincronização, alterações manuais e rollback.
3. Avaliar o uso do parâmetro JVM `-XX:TieredStopAtLevel=1` apenas no lançamento desktop. Ganho de partida tem contrapartida potencial em processamento prolongado; o ensaio não representa sincronizações grandes.
4. Mostrar uma abertura com a carteira do Aether, movimento suave e indicador indeterminado. Encerrar assim que os dados estiverem disponíveis; respeitar `prefers-reduced-motion`. Distinguir iniciar serviço, abrir histórico e erro, usando eventos reais. Não impor duração mínima nem porcentagem inventada.

## Referências

- Spring Boot sobre inicialização lazy e custo adiado para a primeira requisição: https://docs.spring.io/spring-boot/reference/features/spring-application.html
- Spring sobre redução da partida e contrapartida do compilador JVM: https://spring.io/blog/2018/12/12/how-fast-is-spring/

Resultados brutos e script reprodutível: `.tools/startup-analysis/results.json` e `.tools/startup-analysis/benchmark.py`. A etapa inicial acima foi de diagnóstico. As mudanças aplicadas estão descritas abaixo.

## Implementação aplicada

- HibernateJpaAutoConfiguration e JpaRepositoriesAutoConfiguration excluídos na aplicação. Serviços e transações JDBC mantidos; 32 testes do backend passaram na configuração final.
- O launcher desktop passa `-XX:TieredStopAtLevel=1` à JVM do JAR.
- A abertura consulta `/api/health` a cada 250 ms entre tentativas, sempre sequencialmente, e exige serviço e banco em estado `ok` antes de buscar os dados. Timeout da sondagem de 800 ms, da leitura do histórico de até 15 segundos e da abertura inteira de 30 segundos.
- Mudança de mês, nova tentativa e desmontagem cancelam a abertura anterior. Eventos de visibilidade não disparam buscas extras durante a inicialização.
- Carteira com flutuação suave, halo e órbita, indicador indeterminado e etapas baseadas na prontidão real. Sem duração mínima; o histórico aparece assim que chega. A preferência do sistema por movimento reduzido desativa animações. Após erro, animação para e há uma nova tentativa explícita.
- Seis novos testes do fluxo inicial validam prontidão do banco, ausência de espera extra e concorrência, cancelamento, timeout, recuperação e nova tentativa. Total: 18 testes de frontend + 32 de backend.

Validação final do JAR empacotado após a compilação, com cópia dos dados: 2.872 s, 3.734 s até entregar o histórico (transações de uma cópia local (quantidade omitida)). Os arquivos instalados foram comparados por SHA-256 aos artefatos compilados.
