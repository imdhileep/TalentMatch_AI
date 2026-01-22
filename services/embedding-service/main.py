from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI(title="Embedding Service")
model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")


class EmbedRequest(BaseModel):
    id: str
    text: str


class EmbedResponse(BaseModel):
    id: str
    vector: list[float]


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest) -> EmbedResponse:
    vector = model.encode(request.text).tolist()
    return EmbedResponse(id=request.id, vector=vector)
