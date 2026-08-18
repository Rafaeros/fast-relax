# Notificações do FastRelax

Como o aviso sai de uma regra de negócio e chega no aparelho do colaborador.

## Desenho

```
CollaboratorSessionService / SessionReminderJob
        │  publica SessionLifecycleEvent
        ▼
SessionNotificationListener        (@TransactionalEventListener, AFTER_COMMIT)
        │
        ▼
NotificationService  ──── grava em `notifications`  (histórico, sempre)
        │
        ▼
PushDispatcher       (@Async, pool fastrelax-push)
        │
   ┌────┴─────┐
   ▼          ▼
FcmPushProvider   WebPushProvider
 (ANDROID/IOS)      (WEB)
   │                  │
   ▼                  ▼
  FCM            serviço de push do navegador
   │                  │
   ▼                  ▼
 App Capacitor     /sw.js
```

Duas decisões sustentam o resto:

**As regras de sessão não conhecem notificação.** Elas publicam
`SessionLifecycleEvent` e seguem. Acrescentar e-mail ou WhatsApp amanhã é mexer
no listener, não no código que agenda massagem.

**Grava antes de entregar.** Push é best-effort: celular desligado, permissão
revogada ou navegador fechado fazem o aviso sumir sem rastro. A tabela
`notifications` é a fonte da verdade, e a central em `/colaborador/notificacoes`
mostra tudo mesmo para quem nunca ativou push.

## Eventos que geram aviso

| Evento | Tipo | Texto |
| --- | --- | --- |
| Agendou | `SESSION_SCHEDULED` | Sua massagem está marcada para 18/08 às 12:05. |
| Véspera, 10h | `SESSION_REMINDER` | Sua massagem é amanhã às 12:05. |
| 1 h antes | `SESSION_REMINDER` | Sua massagem é em 1 hora, às 12:05. |
| 5 min antes | `SESSION_REMINDER` | Sua massagem é em 5 minutos, às 12:05. |
| Iniciou | `SESSION_STARTED` | Bom descanso! A cadeira desliga sozinha às 12:10. |
| Terminou | `SESSION_FINISHED` | Sua massagem das 12:05 terminou. |
| Não iniciou a tempo | `SESSION_EXPIRED` | Não foi iniciada e o horário foi liberado. |
| Cancelou | `SESSION_CANCELLED` | Sua massagem de 18/08 às 12:05 foi cancelada. |

Os lembretes são os únicos que chegam com o app fechado — os demais reagem a
algo que a pessoa acabou de fazer. São eles que justificam o push existir.

## Cadências dos lembretes

```properties
app.notifications.reminder-offsets=60,5
app.notifications.reminder-interval-ms=60000
app.notifications.daily-digest-cron=0 0 10 * * *
```

**Rolantes** (60 e 5 min): janela com teto, `início <= agora + offset`. Cada
faixa dispara no seu momento. Acrescentar uma é acrescentar um número na lista.

O tick é de **1 minuto**, não 10: com intervalo maior, o lembrete de 5 minutos
poderia sair depois do horário da massagem — atrasado, seria pior que nenhum. A
consulta é um índice, então o custo é irrelevante.

**Véspera**: por calendário, não por offset. Tratar "1 dia antes" como 1440
minutos rolantes quebraria — quem agenda hoje para amanhã, com menos de 24h de
antecedência, casaria com a janela no instante do agendamento e receberia "sua
massagem é amanhã" junto da confirmação.

**Na inicialização** (`ApplicationReadyEvent`): expira as sessões atrasadas e
roda as duas varreduras. Como a máquina é ligada todo dia, subir é justamente
quando mais coisa está pendente. O resumo da véspera só entra se o horário do
cron já passou hoje — a checagem pergunta ao próprio cron, então mudar a
expressão não exige ajustar código.

### Por que uma tabela em vez de uma coluna

`session_reminders(session_id, kind)`, com índice único. Um carimbo só na sessão
bastaria para um lembrete e falharia para três: enviar o de 1 hora marcaria a
sessão como lembrada e engoliria o de 5 minutos.

A marca é gravada **antes** do aviso, na mesma transação — se a notificação
falhar, a marca volta atrás e a faixa continua pendente. E se duas execuções se
cruzarem, a segunda esbarra no índice único em vez de mandar push repetido.

## Web (navegador) — já configurado

VAPID gerado e gravado no `.env` da API:

```properties
WEBPUSH_PUBLIC_KEY=...
WEBPUSH_PRIVATE_KEY=...
WEBPUSH_SUBJECT=mailto:ti@fastrelax.local
```

Gerar outro par (invalida todas as inscrições existentes):

```bash
npx web-push generate-vapid-keys
```

O colaborador ativa em **Perfil → Notificações**. O fluxo é: pede permissão →
registra `/sw.js` → `pushManager.subscribe` → `POST /notifications/devices` com
`platform: "WEB"` e a inscrição inteira.

### A restrição que importa: HTTPS

Service Worker e Push API só existem em **contexto seguro**. Na prática:

| Endereço | Push web |
| --- | --- |
| `http://localhost` | funciona (localhost é exceção da spec) |
| `http://10.48.0.189` | **não funciona** — o navegador nem expõe a API |
| `https://fastrelax.empresa.local` | funciona |

Ou seja: hoje, na rede interna por IP em HTTP, o botão de ativar aparece
explicando que o recurso exige HTTPS. Para liberar de verdade, é preciso servir
o Next por HTTPS — proxy reverso (Caddy, nginx) com certificado interno, ou um
certificado próprio distribuído nas máquinas.

Nada disso afeta o app Android: FCM é nativo, não passa por essa regra.

## Android / iOS (Capacitor) — falta o Firebase

O backend já aceita o token; o que falta é do lado do app e do Firebase.

1. Criar o projeto no [Firebase Console](https://console.firebase.google.com) e
   adicionar um app Android com o mesmo `applicationId` do Capacitor.
2. Baixar `google-services.json` para `android/app/`.
3. Em *Configurações do projeto → Contas de serviço*, gerar a chave privada
   (JSON) e apontar a API para ela:

   ```properties
   FCM_CREDENTIALS_FILE=C:/caminho/fastrelax-firebase-adminsdk.json
   ```

   Enquanto isso estiver vazio o `FcmPushProvider` fica inerte — a notificação
   continua sendo gravada e aparece na central do app, só não vira push.

4. No app:

   ```bash
   npm install @capacitor/push-notifications
   npx cap sync android
   ```

5. Registrar o token depois do login, chamando a action que já existe:

   ```ts
   import { PushNotifications } from "@capacitor/push-notifications";
   import { registerFcmTokenAction } from "@/features/notifications/actions/notification.actions";

   await PushNotifications.requestPermissions();
   await PushNotifications.register();

   PushNotifications.addListener("registration", (token) => {
     void registerFcmTokenAction(token.value, "ANDROID");
   });
   ```

O `AndroidManifest.xml` precisa da permissão `POST_NOTIFICATIONS` (Android 13+),
que o plugin do Capacitor já declara.

## Rotas

| Método | Rota | Para quê |
| --- | --- | --- |
| `GET` | `/notifications` | Lista paginada do colaborador logado |
| `GET` | `/notifications/unread-count` | Contador do sininho |
| `PATCH` | `/notifications/{id}/read` | Marca uma como lida |
| `PATCH` | `/notifications/read-all` | Marca todas |
| `POST` | `/notifications/devices` | Registra token (FCM) ou inscrição (Web) |
| `GET` | `/notifications/devices` | Aparelhos ativos do colaborador |
| `GET` | `/notifications/devices/vapid-public-key` | Chave pública para inscrever |
| `DELETE` | `/notifications/devices?token=` | Desativa um aparelho |
| `DELETE` | `/notifications/devices/subscription?endpoint=` | Desativa uma inscrição web |

## Higiene dos destinos

`device_tokens` não acumula lixo sozinho: quando o provedor responde que o
destino morreu — `UNREGISTERED` no FCM, HTTP 404/410 no Web Push — o
`PushDispatcher` marca `active = false` na hora. Sem isso, cada notificação
futura gastaria uma chamada de rede para receber o mesmo erro.

Falha temporária (rede, serviço fora do ar) **não** desativa nada.
