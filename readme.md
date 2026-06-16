# CS2 Radar - Pro Match Aggregator 🎯

O **CS2 Radar** é um sistema de alta performance construído em **Java** e implantado na **AWS** que atua como um agregador de partidas profissionais e cotações (odds) de Counter-Strike 2.

A aplicação coleta dados em tempo real da API PandaScore, gerencia filas de mensagens de forma assíncrona, salva as informações em um banco NoSQL de alta escalabilidade e expõe uma REST API pública otimizada com cache em memória.

---

## 🏗️ Arquitetura do Sistema

O projeto é dividido em quatro módulos Maven sob uma arquitetura serverless e conteinerizada:

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
                Processor (ECS Fargate Spring Boot)
                    /                  \
                   /                    \
                  ▼                      ▼
        Amazon DynamoDB             Amazon ElastiCache Redis
        (cs2-matches)               (Odds Cache - TTL 2m)
                  \                      /
                   \                    /
                    ▼                  ▼
                    API Lambda (Java)
                             ▲
                             │
                    Amazon API Gateway (HTTP)
                             ▲
                             │ (GET /matches/...)
                          Clientes (Bruno/curl)
```

### Detalhes dos Módulos:
1. **`fetcher` (AWS Lambda)**: Executa periodicamente a cada 5 minutos via AWS EventBridge. Consulta a API da PandaScore por próximas partidas de CS2 filtrando apenas por torneios relevantes (**Tier S e Tier A**), empacotando e enviando as mensagens para a fila do Amazon SQS.
2. **`processor` (Spring Boot no ECS Fargate)**: Um contêiner contínuo que monitora a fila do SQS de forma assíncrona. Ele faz o processamento dos dados recebidos, salva os registros no **Amazon DynamoDB** e cria um cache rápido das odds de aposta no **Amazon Redis**.
3. **`api` (AWS Lambda + API Gateway)**: Uma API REST serverless de baixa latência (otimizada com AWS SnapStart) que responde às requisições do usuário. Ela utiliza uma estratégia de **Cache-First** no Redis para as cotações de aposta e faz fallback para o DynamoDB se necessário.
4. **`infra` (AWS CDK em Java)**: Código de infraestrutura (IaC) que provisiona de forma automatizada toda a infraestrutura da AWS na sua conta (VPC, NAT Gateway, Instâncias do Redis, Tabelas DynamoDB, Fila SQS, Clusters ECS, Lambdas e Rotas da API).

---

## ⚡ Rotas da API

*   **`GET /matches/upcoming`** - Lista as próximas partidas profissionais agendadas.
*   **`GET /matches/{id}`** - Retorna detalhes e odds de uma partida específica (busca em cache primeiro).
*   **`GET /tournaments`** - Lista os IDs de todos os torneios ativos cadastrados.
*   **`GET /teams/{id}/matches`** - Lista todas as partidas agendadas de um time específico pelo ID.

---

## 💻 Desenvolvimento Local

O projeto conta com ferramentas para emular o ambiente completo da AWS no seu computador usando contêineres Docker.

### Pré-requisitos:
* Docker e Docker Compose instalados.
* Maven e Java 21 instalados.

### 1. Iniciar os Serviços Locais:
Suba o LocalStack (que simula o SQS e DynamoDB) e o Redis local rodando:
```bash
docker compose up -d
```

### 2. Criar Tabelas e Filas Locais:
Configure os recursos locais rodando o script de inicialização apropriado para seu terminal:
* **PowerShell (Windows):**
  ```powershell
  ./init-local.ps1
  ```
* **Bash (Linux/Mac):**
  ```bash
  chmod +x init-local.sh
  ./init-local.sh
  ```

### 3. Rodar o Servidor de API Localmente:
Inicie o servidor HTTP local na porta `8080` (que emula o API Gateway):
```bash
mvn exec:java -pl api '-Dexec.mainClass=com.cs2agg.api.LocalApiServer'
```

### 4. Semear Dados de Partidas Localmente:
Você pode disparar o processo do Fetcher manualmente para preencher o banco de dados local com partidas reais da PandaScore:
```bash
mvn exec:java -pl fetcher '-Dexec.mainClass=com.cs2agg.fetcher.LocalFetcherRunner'
```

---

## 🚀 Implantação na AWS (Produção)

Para subir o projeto para a nuvem da AWS usando o CDK:

### 1. Salvar Token da API no SSM:
Salve a sua chave da API da PandaScore de forma segura no Parameter Store da AWS (mude o valor para o seu token real):
```bash
aws ssm put-parameter --name "/cs2agg/pandascore-key" --type "String" --value "SEU_TOKEN_PANDASCORE" --overwrite
```

### 2. Compilar os Pacotes:
Gere os arquivos JAR sombreados (sombra) necessários para o deploy das Lambdas e do ECS:
```bash
mvn clean package -DskipTests
```

### 3. Deploy da Infraestrutura:
Entre na pasta `infra` e execute o deploy do CDK:
```bash
cd infra
cdk deploy --all
```

Ao final da execução, o console do CDK imprimirá o endpoint público gerado pela AWS para você consumir a sua API de qualquer lugar!
