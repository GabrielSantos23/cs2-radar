# CS2 Radar 🎯

---

O projeto coleta automaticamente partidas dos torneios mais importantes do cenário — Majors, IEM, BLAST Premier e equivalentes tier A — a cada 5 minutos, armazena tudo de forma estruturada e expõe uma REST API limpa para consumo. As odds ficam em cache com atualização frequentes em tempo real.

Esse projeto é um exercício real de arquitetura: coleta assíncrona, processamento desacoplado, cache inteligente, infraestrutura como código, rodando na AWS sem servidor.

---

## Arquitetura

O sistema é dividido em quatro módulos Maven com responsabilidades bem separadas:

```
                  ┌──────────────────────┐
                  │   PandaScore API     │
                  └──────────┬───────────┘
                             │ (a cada 5 min)
                    Fetcher Lambda (Java)
                             │
                             ▼
                    Amazon SQS Queue
                             │
                             ▼
                Processor (ECS Fargate · Spring Boot)
                    /                  \
                   /                    \
                  ▼                      ▼
        Amazon DynamoDB             Amazon ElastiCache Redis
        (cs2-matches)               (Odds Cache · TTL 2min)
                  \                      /
                   \                    /
                    ▼                  ▼
                    API Lambda (Java · SnapStart)
                             ▲
                             │
                    Amazon API Gateway (HTTP)
                             ▲
                             │
                          Client
```

### Módulos

**`fetcher`** — Lambda Java dispara a cada 5 minutos via EventBridge. Consulta a PandaScore filtrando apenas torneios Tier S e Tier A, serializa as partidas como JSON e envia para a fila SQS em batches. Não usa Spring para manter o JAR leve e o cold start baixo.

**`processor`** — Container Spring Boot rodando continuamente no ECS Fargate. Consome mensagens do SQS via long polling gerenciado pelo Spring Cloud AWS, persiste as partidas no DynamoDB e atualiza o cache de odds no Redis. É o único módulo com Spring Boot — faz sentido aqui porque é um processo de longa duração.

**`api`** — Lambda Java com SnapStart que serve a REST API via API Gateway. Adota estratégia cache-first: busca odds no Redis e só vai ao DynamoDB em caso de miss. Também sem Spring, pela mesma razão do fetcher.

**`infra`** — CDK em Java que provisiona toda a infraestrutura: VPC, ElastiCache, DynamoDB, SQS, ECS Cluster, Lambdas e API Gateway. Infraestrutura e aplicação no mesmo repositório, na mesma linguagem.

---

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/matches/upcoming` | Próximas partidas agendadas de torneios tier S e A |
| `GET` | `/matches/{id}` | Detalhes e odds de uma partida específica |
| `GET` | `/tournaments` | Torneios ativos com partidas cadastradas |
| `GET` | `/teams/{id}/matches` | Próximas partidas de um time pelo ID |

---

## Stack

- **Java 21** — records, virtual threads no processor
- **Spring Boot 3.3** — apenas no processor (consumer SQS)
- **AWS Lambda** — fetcher e api (runtime java21 + SnapStart na api)
- **AWS EventBridge** — scheduler do fetcher (cron a cada 5 min)
- **AWS SQS** — desacopla a coleta do processamento
- **AWS ECS Fargate** — container do processor sem gerenciar servidor
- **AWS DynamoDB** — armazenamento principal (pay-per-request)
- **AWS ElastiCache Redis** — cache de odds com TTL de 2 minutos
- **AWS API Gateway HTTP** — entrada da REST API com CORS configurado
- **AWS CDK v2 em Java** — infraestrutura como código
- **PandaScore API** — fonte de dados de partidas e torneios
- **LocalStack** — emulação local de SQS e DynamoDB para dev

---

## Rodando localmente

### Pré-requisitos

- Java 21
- Maven
- Docker e Docker Compose

### 1. Subir os serviços locais

```bash
docker compose up -d
```

Isso sobe o LocalStack (emula SQS e DynamoDB) e um Redis local.

### 2. Criar os recursos locais

**PowerShell (Windows):**
```powershell
./init-local.ps1
```

**Bash (Linux/Mac):**
```bash
chmod +x init-local.sh && ./init-local.sh
```

### 3. Popular o banco com partidas reais

Dispara o fetcher manualmente contra a PandaScore (requer `PANDASCORE_API_KEY` no ambiente):

```bash
mvn exec:java -pl fetcher -Dexec.mainClass=com.cs2agg.fetcher.LocalFetcherRunner
```

### 4. Subir a API local

```bash
mvn exec:java -pl api -Dexec.mainClass=com.cs2agg.api.LocalApiServer
```

A API sobe na porta `8080`. Teste com curl ou Bruno:

```bash
curl http://localhost:8080/matches/upcoming
```

---

## Deploy na AWS

### 1. Salvar a API key no SSM

```bash
aws ssm put-parameter \
  --name "/cs2agg/pandascore-key" \
  --type "SecureString" \
  --value "SEU_TOKEN_PANDASCORE" \
  --overwrite
```

### 2. Build

```bash
mvn clean package -DskipTests
```

### 3. Deploy

```bash
cd infra && cdk deploy --all
```

O CDK imprime o endpoint público ao final. A partir daí a coleta começa automaticamente — o EventBridge dispara o fetcher a cada 5 minutos sem nenhuma intervenção.

---

## Estrutura do projeto

```
cs2-intel/
├── fetcher/       # Lambda de coleta (Java puro + OkHttp)
├── processor/     # Consumer SQS (Spring Boot + ECS Fargate)
├── api/           # Lambda REST (Java puro + SnapStart)
├── infra/         # Infraestrutura CDK em Java
└── docker-compose.yml
```

---
