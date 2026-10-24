# ElderlyHealth CPS — protótipo back-end (Sprint 1)

Controllers REST das **4 features básicas** do projeto MSP 26/27, em Java 17 + Spring Boot 3 (Maven).
Cada controller segue o BPMN da feature: **toda a lógica está no CPS** (services) e os outros
participantes (utilizador, idoso, família, enfermeiro, sensores) só enviam e recebem informação.

## Correr

```bash
mvn spring-boot:run          # arranca em http://localhost:8080
mvn test                     # 23 testes unitários dos serviços
```

Ao arrancar é criado um idoso de demonstração (**id 1**, Maria Silva, quarto Q204) para poderem testar
contactos, quedas e tensão sem registar primeiro (desligar com `elderlyhealth.dados-demo=false`).
Os dados ficam em memória (perdem-se ao reiniciar).

## Estrutura

```
controller/   RegistoContaController, ContactoFamiliarController, DetecaoQuedaController,
              TensaoArterialController (+ IdosoController, NotificacaoController de apoio)
service/      lógica de cada BPMN + TimeoutScheduler (timers) + ValidadorDados (NIF, telefone)
model/        entidades, estados (enums) e records de dados
repository/   repositórios em memória (trocáveis por Spring Data JPA)
dto/          corpos dos pedidos
exception/    erros -> HTTP 400 / 404 / 409 / 410
```

As mensagens que o CPS envia aos participantes (os *message flows* do BPMN) ficam em
`GET /api/notificacoes?destinatario=UTILIZADOR|IDOSO|FAMILIA|ENFERMEIRO`.

Os timers dos BPMN estão em `application.properties` (`PT20M`, `PT1M`) e são verificados a cada 5 s.

---

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
| 404 | idoso / pedido / alerta inexistente |
| 409 | passo fora de ordem no processo (ex.: preferências antes dos dados) |
| 410 | o timer do BPMN expirou |
