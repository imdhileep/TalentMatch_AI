# TalentMatch AI (Full Project)

TalentMatch AI is a multi-service system for resume ingestion, embedding generation, vector search, and job matching.

## Services

- **resume-service** (Spring Boot, Java 21) - `http://localhost:8081`
- **match-service** (Spring Boot, Java 21) - `http://localhost:8082`
- **indexer-worker** (Spring Boot worker)
- **embedding-service** (FastAPI, Python 3.11) - `http://localhost:8000`
- **postgres** - `localhost:5432`
- **qdrant** - `http://localhost:6333` (dashboard)
- **kafka + zookeeper** - internal `kafka:9092`, host `localhost:29092`
- **jaeger** - `http://localhost:16686`
- **otel-collector** - `http://localhost:4318`
- **prometheus** - `http://localhost:9090`

## Run

```bash
docker compose down -v
docker compose up --build
```

## Verification

### Health checks

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8000/health
```

### Upload a resume

```bash
curl -F "file=@C:\Users\dhile\Downloads\Dhileep_Kumar_Pagadala_Java_Full_Stack_Developer.pdf" http://localhost:8081/resumes
```

### Create a job

```bash
curl -X POST http://localhost:8082/jobs \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Java Full Stack Developer\",\"company\":\"Acme\",\"location\":\"USA\",\"description\":\"Spring Boot, Kafka, AWS, Microservices\"}"
```

### Match jobs for a resume

```bash
curl "http://localhost:8082/match/resume/<resumeId>?topK=5"
```

### Qdrant UI

Open: http://localhost:6333/dashboard

## Notes

- Resumes are extracted using Apache PDFBox and stored in Postgres.
- `indexer-worker` consumes Kafka topic `resumes.created`, creates embeddings, and upserts to Qdrant collection `resumes`.
- `match-service` upserts job vectors to Qdrant collection `jobs` and searches by resume vector similarity.
