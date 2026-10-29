# Componentes de terceiros

A licença de uso pessoal do Aether se aplica às suas partes originais. Os componentes abaixo conservam seus próprios direitos, termos e avisos. As restrições do Aether não substituem permissões obrigatórias dessas licenças.

| Componente | Licença / avisos | Código e informações |
|---|---|---|
| Eclipse Temurin / OpenJDK 21, runtime Windows | GPLv2 com Classpath Exception; avisos em `resources/runtime/legal`, `NOTICE` e arquivos do runtime | https://github.com/adoptium/temurin21-binaries ; https://github.com/adoptium/jdk21u |
| Microsoft Edge WebView2 | Termos de distribuição Microsoft incluídos no instalador oficial | https://developer.microsoft.com/microsoft-edge/webview2/ |
| Tauri, Rust e dependências do shell | MIT e/ou Apache-2.0, conforme cada componente e seus manifests | https://github.com/tauri-apps/tauri ; `apps/desktop/src-tauri/Cargo.lock` |
| React | MIT | https://github.com/facebook/react |
| Lucide | ISC | https://github.com/lucide-icons/lucide |
| Spring Boot / Spring Framework / Jackson / Tomcat / Flyway e dependências | Licenças individuais; textos mantidos nos JARs distribuídos | `apps/api/pom.xml`, manifests e `META-INF` em `resources/aether-api.jar` |
| Hibernate ORM 6.6.22.Final | LGPL-2.1-or-later; biblioteca não modificada, licença no JAR | https://github.com/hibernate/hibernate-orm/tree/6.6.22 |
| SQLite JDBC / SQLite | Apache-2.0 e avisos BSD/public-domain dos componentes incluídos | https://github.com/xerial/sqlite-jdbc |
| Manrope | SIL Open Font License 1.1 | `apps/desktop/src/assets/Manrope-OFL.txt`; cópia distribuída em `licenses/Manrope-OFL.txt` |
| Logotipos de instituições financeiras | Marcas de seus respectivos titulares; usadas para identificação | `apps/desktop/src/assets/banks/README.md` |

Não foram modificados os componentes de terceiros acima. Licenças e avisos internos dos JARs e do runtime Java são preservados no pacote. Avisos das dependências JavaScript/Rust e o inventário de versões são incluídos em `third-party-licenses/` no diretório de recursos do instalador. Para obter suas versões de código-fonte, consulte os repositórios correspondentes e as versões dos manifests/lockfiles.

Quando a LGPL for aplicável, você mantém os direitos que ela exige para substituir ou modificar a biblioteca e analisar essas alterações. O JAR do backend é um arquivo ZIP: as bibliotecas ficam em `BOOT-INF/lib` e podem ser extraídas e substituídas; preserve as entradas e utilize uma versão compatível. As restrições da licença do Aether não anulam esses direitos.

As ilustrações do README são demonstrativas, com dados fictícios. Nenhum histórico, captura de conta real, credencial ou banco de dados de usuário faz parte do instalador.
