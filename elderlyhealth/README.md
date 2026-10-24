# ElderlyHealth CPS — protótipo back-end (Sprint 1)

Controllers REST das **4 features básicas** do projeto MSP 26/27, em Java 17 + Spring Boot 3 (Maven).
Cada controller segue o BPMN da feature: **toda a lógica está no CPS** (services) e os outros
participantes (utilizador, idoso, família, enfermeiro, sensores) só enviam e recebem informação.

## Correr

```bash
mvn spring-boot:run          # abrir http://localhost:8080 no browser
mvn test                     # 34 testes unitários dos serviços
```

O **front-end** está em `src/main/resources/static/` e é servido pelo próprio Spring Boot:
não é preciso Node nem outro servidor. A letra (Atkinson Hyperlegible) está incluída no projeto.

### Contas de demonstração

| Conta | E-mail | Palavra-passe |
|---|---|---|
| Família (filho da Maria Silva) | `joao.silva@mail.pt` | `familia2026` |
| Enfermagem | `enfermeira@elderlyhealth.pt` | `enfermagem2026` |
| Administração | `admin@elderlyhealth.pt` | `admin2026` |

São criados dois idosos (Maria Silva e Manuel Ferreira) com uma semana de medições de tensão.
Desligar com `elderlyhealth.dados-demo=false`. Os dados ficam em memória; **os documentos ficam no disco**.

## Login e permissões (main.bpmn)

`Aceder à aplicação -> Já tem conta? -> Não: criar conta + inscrever idoso -> Realizar autenticação -> Menu principal`

| Endpoint | O que faz |
|---|---|
| `POST /api/auth/registar` | cria conta (familiar ou o próprio idoso) e já devolve a sessão |
| `POST /api/auth/login` | devolve `{ token, utilizador }` |
| `POST /api/auth/logout` / `GET /api/auth/eu` | terminar sessão / quem sou |

- Todos os pedidos a `/api/**` levam `Authorization: Bearer {token}` (sessões de 8 h).
- Palavras-passe guardadas com **PBKDF2-SHA256 + salt** (nunca em texto); 5 tentativas erradas bloqueiam 5 min.
- **Família / idoso** só vê os idosos que inscreveu; **enfermagem / administração** vê todos.
  Só a equipa regista o relatório de atendimento de uma queda.
- Sensores e tensiómetros podem enviar `POST /api/quedas` e `POST /api/tensao-arterial/medicoes`
  com o cabeçalho `X-Device-Key: dispositivo-demo-123` (sem login).

## Pasta de documentos de cada idoso

```
storage/idosos/idoso-{NIF}/historico-clinico/
                           exames/
                           receitas/
                           relatorios/      <- relatórios de quedas gerados automaticamente
                           outros/
```

A pasta é criada quando o idoso é registado e usa o NIF, para continuar associada ao mesmo idoso
depois de reiniciar a aplicação. Configurável em `elderlyhealth.storage.dir`. Aceita PDF, imagens,
Word, Excel, ODT, CSV e texto até 10 MB; nomes são limpos e há proteção contra *path traversal*.

| Endpoint | |
|---|---|
| `GET /api/idosos/{id}/documentos[?categoria=EXAMES]` | listar |
| `POST /api/idosos/{id}/documentos` (multipart: `ficheiro`, `categoria`) | carregar |
| `GET /api/idosos/{id}/documentos/{categoria}/{nome}[?inline=true]` | descarregar / ver |
| `DELETE /api/idosos/{id}/documentos/{categoria}/{nome}` | apagar |

## Estrutura

```
controller/   RegistoContaController, ContactoFamiliarController, DetecaoQuedaController,
              TensaoArterialController, AuthController, DocumentoController
              (+ IdosoController, NotificacaoController de apoio)
service/      lógica de cada BPMN + AuthService + DocumentoService + TimeoutScheduler (timers)
model/        entidades, estados (enums) e records de dados
repository/   repositórios em memória (trocáveis por Spring Data JPA)
dto/          corpos dos pedidos e respostas
exception/    erros -> HTTP 400 / 401 / 403 / 404 / 409 / 410
config/       AuthInterceptor (sessões), WebConfig, dados de demonstração
static/       front-end (index.html, css/, js/, fonts/)
```

As mensagens que o CPS envia aos participantes (os *message flows* do BPMN) ficam em
`GET /api/notificacoes?destinatario=UTILIZADOR|IDOSO|FAMILIA|ENFERMEIRO`.

Os timers dos BPMN estão em `application.properties` (`PT20M`, `PT1M`) e são verificados a cada 5 s.

---

Nos exemplos `curl` abaixo, acrescentem `-H "Authorization: Bearer $TOKEN"` (token devolvido pelo login).

## 1. Registar conta do idoso — `/api/registos`

| BPMN | Endpoint |
|---|---|
| Pedido de registo → *Requisitar informação do idoso* | `POST /api/registos` |
| *Receber dados* → *Validar dados* (NIF válido e não repetido) | `PUT /api/registos/{id}/dados` |
| Ramo paralelo: *Receber preferências* | `PUT /api/registos/{id}/preferencias` |
| Ramo paralelo: *Receber contacto de emergência* → validar | `PUT /api/registos/{id}/contacto-emergencia` |
| Timer 20 min → *Tempo limite excedido* | automático (estado `EXPIRADO`, HTTP 410) |

Estados: `AGUARDA_DADOS → AGUARDA_PREFERENCIAS_E_CONTACTO → REGISTADO | NEGADO | EXPIRADO`.
A conta só é criada quando os dois ramos paralelos chegaram.

```bash
curl -X POST localhost:8080/api/registos -H 'Content-Type: application/json' -d '{"canal":"RECEPCIONISTA"}'
curl -X PUT localhost:8080/api/registos/1/dados -H 'Content-Type: application/json' \
  -d '{"nome":"António Costa","nif":"123456789","dataNascimento":"1940-01-01","informacaoMedica":"Diabetes","quarto":"Q101"}'
curl -X PUT localhost:8080/api/registos/1/preferencias -H 'Content-Type: application/json' \
  -d '{"dieta":"Sem açúcar","alergias":["Lactose"],"observacoes":"Usa andarilho"}'
curl -X PUT localhost:8080/api/registos/1/contacto-emergencia -H 'Content-Type: application/json' \
  -d '{"nome":"Ana Costa","telefone":"961234567","parentesco":"Filha"}'
```

## 2. Adicionar contacto familiar — `/api/idosos/{idosoId}/contactos-familiares`

| BPMN | Endpoint |
|---|---|
| *Contacto familiar* recebido → *Validar* → *Requisitar validação do user* | `POST .../pedidos` (202) |
| Event-based gateway: *Receber confirmação* | `POST .../pedidos/{pedidoId}/confirmacao` |
| Event-based gateway: timer 1 min → *Falta de confirmação* | automático (`EXPIRADO`, HTTP 410) |
| *Guardar contacto familiar* | ao confirmar com `true` |
| Lista de contactos | `GET /api/idosos/{idosoId}/contactos-familiares` |

Contacto inválido (telefone inválido ou repetido) → HTTP 400.

```bash
curl -X POST localhost:8080/api/idosos/1/contactos-familiares/pedidos -H 'Content-Type: application/json' \
  -d '{"nome":"Pedro Silva","telefone":"936543210","email":"pedro@mail.pt","parentesco":"Neto"}'
curl -X POST localhost:8080/api/idosos/1/contactos-familiares/pedidos/1/confirmacao -H 'Content-Type: application/json' \
  -d '{"confirmado":true}'
```

## 3. Detetar queda — `/api/quedas`

| BPMN | Endpoint |
|---|---|
| *Queda detetada pelo sensor* → *Pedir confirmação ao idoso* | `POST /api/quedas` (chamado pelo sensor) |
| *Resposta do idoso* → *Ajuda necessária?* | `POST /api/quedas/{id}/resposta` |
| Timer 1 min sem resposta → alerta | automático (`escaladoPorTimeout: true`) |
| *Obter quarto* → *Alertar enfermeiro* | automático quando pede ajuda ou não responde |
| *Receber relatório de atendimento* → *Registar incidente* | `POST /api/quedas/{id}/relatorio` |
| Painel da enfermagem | `GET /api/quedas?estado=ENFERMEIRO_ALERTADO` |

Estados: `AGUARDA_RESPOSTA_IDOSO → CANCELADO | ENFERMEIRO_ALERTADO → INCIDENTE_REGISTADO`.

```bash
curl -X POST localhost:8080/api/quedas -H 'Content-Type: application/json' -d '{"idosoId":1,"sensorId":"wearable-7"}'
curl -X POST localhost:8080/api/quedas/1/resposta -H 'Content-Type: application/json' -d '{"precisaAjuda":true}'
curl -X POST localhost:8080/api/quedas/1/relatorio -H 'Content-Type: application/json' \
  -d '{"enfermeiro":"Enf. Rita","relatorio":"Sem ferimentos, levantada e observada"}'
```

## 4. Monitorizar tensão arterial — `/api/tensao-arterial`

| BPMN | Endpoint |
|---|---|
| Dispositivo envia medição → *Analisar* → *Estado da pressão?* | `POST /api/tensao-arterial/medicoes` |
| OK → *Notificar idoso da condição normal* | resposta `NORMAL` |
| Alerta não grave → *Gerar relatório de cuidados* → notificar idoso | `ALERTA_NAO_GRAVE` + `recomendacoes` |
| Alerta grave → *Notificar enfermeiro* | `ALERTA_GRAVE` |
| Histórico | `GET /api/tensao-arterial/idosos/{idosoId}` |

Limiares do protótipo (mmHg, em `TensaoArterialService`): **grave** se sistólica ≥ 180, diastólica ≥ 120
ou sistólica < 80; **não grave** se sistólica ≥ 140, diastólica ≥ 90, sistólica < 90 ou diastólica < 60.

```bash
curl -X POST localhost:8080/api/tensao-arterial/medicoes -H 'Content-Type: application/json' \
  -d '{"idosoId":1,"dispositivoId":"tensiometro-1","sistolica":185,"diastolica":110,"pulso":95}'
curl 'localhost:8080/api/notificacoes?destinatario=ENFERMEIRO'
```

## Códigos de erro

| HTTP | Quando |
|---|---|
| 400 | dados inválidos (campos em falta, telefone inválido, sistólica ≤ diastólica) |
| 401 | sem sessão, sessão expirada ou credenciais erradas |
| 403 | a conta não tem acesso a este idoso / ação só da equipa |
| 413 | ficheiro maior do que 10 MB |
| 404 | idoso / pedido / alerta inexistente |
| 409 | passo fora de ordem no processo (ex.: preferências antes dos dados) |
| 410 | o timer do BPMN expirou |
