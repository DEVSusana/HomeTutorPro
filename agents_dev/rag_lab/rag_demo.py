#!/usr/bin/env python3
"""
Isolated RAG & Embeddings Learning Lab (zero impact on HomeTutorPro Android app).

Demonstrates the full RAG pipeline over sample tutoring notes:
1. Chunking with overlap and metadata (student, subject, date).
2. Vectorization (local TF-IDF / char-ngram fallback offline, or Gemini embeddings with --live).
3. Hybrid retrieval (cosine vector similarity + lexical keyword overlap) with metadata pre-filtering.
4. Grounded prompt construction with abstention when no chunk clears the similarity threshold.
"""

import argparse
import math
import os
import re
import sys
from collections import Counter
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

SAMPLE_NOTES = [
    {
        "id": "note-1",
        "student": "Darío",
        "subject": "Matemáticas",
        "date": "2026-11-10",
        "text": (
            "Trabajamos ecuaciones de segundo grado. Le costó despejar la x en la fórmula cuadrática "
            "cuando el discriminante era negativo. Repasar ejercicios 4 y 5 la próxima sesión."
        ),
    },
    {
        "id": "note-2",
        "student": "Ana",
        "subject": "Física",
        "date": "2026-11-12",
        "text": (
            "Vimos cinemática y caída libre. Domina bien el despeje de velocidad final, "
            "pero se confunde con los signos de la aceleración cuando el objeto sube."
        ),
    },
    {
        "id": "note-3",
        "student": "Darío",
        "subject": "Matemáticas",
        "date": "2026-11-18",
        "text": (
            "Ya resuelve fracciones algebraicas con soltura. Queda pendiente revisar sistemas "
            "de dos ecuaciones lineales antes del examen del viernes."
        ),
    },
]


@dataclass
class Chunk:
    """Represents a single indexed text chunk with metadata for pre-filtering."""

    chunk_id: str
    student: str
    subject: str
    date: str
    text: str


def chunk_text(text: str, max_words: int = 28, overlap_words: int = 6) -> List[str]:
    """Splits text into overlapping word chunks so context is not cut mid-idea."""
    words = text.split()
    if len(words) <= max_words:
        return [text]

    chunks = []
    step = max(1, max_words - overlap_words)
    for start in range(0, len(words), step):
        piece = " ".join(words[start : start + max_words])
        chunks.append(piece)
        if start + max_words >= len(words):
            break
    return chunks


def build_chunks(notes: List[Dict[str, str]]) -> List[Chunk]:
    """Converts raw notes into metadata-tagged chunks."""
    all_chunks: List[Chunk] = []
    for note in notes:
        for idx, piece in enumerate(chunk_text(note["text"])):
            all_chunks.append(
                Chunk(
                    chunk_id=f"{note['id']}#c{idx}",
                    student=note["student"],
                    subject=note["subject"],
                    date=note["date"],
                    text=piece,
                )
            )
    return all_chunks


def _tokenize(text: str) -> List[str]:
    return re.findall(r"[a-záéíóúüñ0-9]+", text.lower())


def _char_ngrams(text: str, n: int = 3) -> Counter:
    cleaned = f" {text.lower()} "
    return Counter(cleaned[i : i + n] for i in range(max(0, len(cleaned) - n + 1)))


def cosine_similarity(vec_a: Dict[str, float], vec_b: Dict[str, float]) -> float:
    """Computes cosine similarity between two sparse vectors."""
    if not vec_a or not vec_b:
        return 0.0
    common = set(vec_a.keys()) & set(vec_b.keys())
    dot = sum(vec_a[k] * vec_b[k] for k in common)
    norm_a = math.sqrt(sum(v * v for v in vec_a.values()))
    norm_b = math.sqrt(sum(v * v for v in vec_b.values()))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return dot / (norm_a * norm_b)


def dense_cosine_similarity(vec_a: List[float], vec_b: List[float]) -> float:
    """Computes cosine similarity between two dense float embedding vectors."""
    if not vec_a or not vec_b or len(vec_a) != len(vec_b):
        return 0.0
    dot = sum(a * b for a, b in zip(vec_a, vec_b))
    norm_a = math.sqrt(sum(a * a for a in vec_a))
    norm_b = math.sqrt(sum(b * b for b in vec_b))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return dot / (norm_a * norm_b)


def _embed_with_gemini(texts: List[str]) -> List[List[float]]:
    """Optional live dense embeddings using google-genai if GEMINI_API_KEY is set."""
    from google import genai

    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise ValueError("GEMINI_API_KEY is not set.")
    client = genai.Client(api_key=api_key)
    vectors: List[List[float]] = []
    for text in texts:
        resp = client.models.embed_content(model="text-embedding-004", contents=text)
        vectors.append(list(resp.embeddings[0].values))
    return vectors


def hybrid_search(
    query: str,
    chunks: List[Chunk],
    student_filter: Optional[str] = None,
    top_k: int = 2,
    min_score: float = 0.15,
    use_live_embeddings: bool = False,
) -> List[Tuple[Chunk, float]]:
    """
    Retrieves top_k chunks using metadata pre-filtering + hybrid scoring
    (vector cosine similarity + exact token overlap).
    """
    candidates = [
        c for c in chunks if not student_filter or c.student.lower() == student_filter.lower()
    ]
    if not candidates:
        return []

    query_tokens = set(_tokenize(query))

    if use_live_embeddings:
        vectors = _embed_with_gemini([query] + [c.text for c in candidates])
        q_dense, doc_denses = vectors[0], vectors[1:]
        scored = []
        for chunk, d_vec in zip(candidates, doc_denses):
            sem = dense_cosine_similarity(q_dense, d_vec)
            doc_tokens = set(_tokenize(chunk.text))
            lex = len(query_tokens & doc_tokens) / max(1, len(query_tokens))
            score = 0.75 * sem + 0.25 * lex
            if score >= min_score:
                scored.append((chunk, round(score, 4)))
    else:
        q_vec = {k: float(v) for k, v in _char_ngrams(query).items()}
        scored = []
        for chunk in candidates:
            d_vec = {k: float(v) for k, v in _char_ngrams(chunk.text).items()}
            sem = cosine_similarity(q_vec, d_vec)
            doc_tokens = set(_tokenize(chunk.text))
            lex = len(query_tokens & doc_tokens) / max(1, len(query_tokens))
            score = 0.65 * sem + 0.35 * lex
            if score >= min_score:
                scored.append((chunk, round(score, 4)))

    scored.sort(key=lambda item: item[1], reverse=True)
    return scored[:top_k]


def build_grounded_prompt(query: str, matches: List[Tuple[Chunk, float]]) -> str:
    """Builds a grounded RAG prompt instructing the model to abstain if evidence is missing."""
    if not matches:
        return "NO_CONTEXT_FOUND: Inform the user there is not enough information in the notes."

    context_lines = [
        f"[{c.chunk_id} | {c.student} | {c.subject} | {c.date} | score={score}] {c.text}"
        for c, score in matches
    ]
    context_block = "\n".join(context_lines)
    return (
        "Answer ONLY using the retrieved notes below. Cite the note ID and date.\n"
        "If the answer is not in the notes, reply: 'No tengo datos suficientes en las notas.'\n\n"
        f"--- RETRIEVED NOTES ---\n{context_block}\n\n"
        f"Question: {query}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Isolated RAG & Embeddings Lab")
    parser.add_argument(
        "query",
        nargs="?",
        default="¿Qué problemas tuvo Darío con la fórmula cuadrática?",
        help="Question to test retrieval against sample notes.",
    )
    parser.add_argument("--student", default=None, help="Optional student metadata pre-filter.")
    parser.add_argument("--live", action="store_true", help="Use Gemini text-embedding-004 instead of offline mode.")
    args = parser.parse_args()

    chunks = build_chunks(SAMPLE_NOTES)
    matches = hybrid_search(
        query=args.query,
        chunks=chunks,
        student_filter=args.student,
        use_live_embeddings=args.live,
    )

    print(f"\n=== RAG Lab ({'LIVE Gemini Embeddings' if args.live else 'Offline Hybrid Mode'}) ===")
    print(f"Query:   {args.query}")
    print(f"Filter:  student={args.student or 'ALL'}")
    print(f"Chunks:  {len(chunks)} indexed\n")
    print(build_grounded_prompt(args.query, matches))
    return 0


if __name__ == "__main__":
    sys.exit(main())
