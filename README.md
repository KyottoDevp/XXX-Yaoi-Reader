# XXX Yaoi Reader

Aplicativo Android nativo, em Kotlin + Jetpack Compose, para navegar no catálogo XXX Yaoi, consultar detalhes, abrir capítulos e ler imagens em rolagem vertical. O projeto inclui um workflow de GitHub Actions que compila um APK de debug instalável.

> **Estado do projeto: alpha inicial.** A interface, a camada de rede HTTP, modelos locais, favoritos, histórico, downloads e pipeline de compilação estão implementados. A compatibilidade real com o leitor de capítulos do site ainda precisa ser validada em um aparelho com acesso ao domínio. Não há promessa de que a primeira compilação conseguirá carregar capítulos protegidos ou gerados exclusivamente por JavaScript.

## O que existe nesta versão

- Interface Android com tema escuro, navegação inferior e layout adaptável.
- Catálogo e busca por nome de obra.
- Tela de detalhes com sinopse, autor, artista, gênero, situação e capítulos.
- Leitor de imagens nativo com rolagem vertical.
- Favoritos e histórico persistidos localmente.
- Download de capítulos para o diretório privado externo do aplicativo.
- Campo para informar manualmente um cabeçalho Cookie de uma sessão que o próprio usuário tenha obtido legitimamente.
- Tratamento explícito para erros HTTP, Cloudflare, limite de requisições e páginas que não aparecem no HTML.
- GitHub Actions com testes unitários e geração de APK.

## Sem WebView

O projeto não declara nem utiliza `android.webkit.WebView`. A comunicação com o site usa OkHttp; o HTML é interpretado com Jsoup; as imagens são exibidas com Coil; e a interface é composta em Jetpack Compose.

Isso é uma escolha intencional, mas traz uma limitação importante: o site pode executar scripts no navegador, criar imagens `blob:`, capturar cabeçalhos `X-*` ou exigir Cloudflare Turnstile. Uma implementação baseada somente em HTTP não consegue reproduzir automaticamente todas essas ações do navegador. Quando o site exigir uma verificação interativa, o app reportará a limitação em vez de tentar contorná-la.

## Como gerar o APK pelo GitHub Actions

1. Crie um repositório no GitHub e envie todo o conteúdo deste ZIP para a raiz do repositório.
2. Na aba **Actions**, habilite workflows caso o GitHub solicite.
3. Execute **Android APK Build** usando **Run workflow**, ou faça push para `main`/`master`.
4. Aguarde a conclusão do job **Compile debug APK**.
5. Na execução, baixe o artifact **`xxxyaoi-reader-debug-apk`**. Dentro dele estará `app-debug.apk`.
6. Instale o APK no Android. Esta compilação debug usa a chave de depuração automática do Gradle.

O workflow usa JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.1 com Kotlin integrado do AGP e Android API 37. O plugin Compose 2.4.21 seleciona a versão compatível do compilador Kotlin. As versões estão fixadas para que a compilação não mude silenciosamente por atualização dinâmica.

## Build local

Abra a pasta do projeto no Android Studio com suporte ao Android Gradle Plugin 9.3.1 e Gradle 9.5.0. Ou, em um ambiente com Android SDK configurado:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK esperado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Pacotes usados

- Kotlin 2.4.21
- Jetpack Compose BOM 2026.11.01
- OkHttp 4.12.0
- Jsoup 1.21.2
- Coil 2.7.0
- Gson 2.13.2

## Privacidade e segurança

Favoritos, histórico, downloads e cookie informado nas configurações ficam no armazenamento privado do aplicativo. Não existe conta própria nem backend intermediário neste projeto. O cookie é enviado ao domínio configurado para acesso ao site; não informe cookies de contas que não sejam suas. Não compartilhe APKs modificados com credenciais incluídas.

## Créditos e relação com o site

Este é um projeto independente, não oficial e não afiliado ao XXX Yaoi, Mihon, Tachiyomi, Komikku ou Keiyoushi. O domínio e os formatos de dados foram identificados a partir do APK de extensão fornecido pelo usuário e da documentação/código público da extensão XXX Yaoi. O projeto não inclui conteúdo editorial do site.
