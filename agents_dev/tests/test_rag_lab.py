import unittest

from rag_lab.rag_demo import (
    SAMPLE_NOTES,
    build_chunks,
    build_grounded_prompt,
    chunk_text,
    dense_cosine_similarity,
    hybrid_search,
)


class TestRagLab(unittest.TestCase):
    def test_chunk_text_splits_with_overlap(self):
        text = " ".join(f"w{i}" for i in range(40))
        chunks = chunk_text(text, max_words=20, overlap_words=5)
        self.assertGreaterEqual(len(chunks), 2)
        self.assertIn("w15", chunks[1])

    def test_hybrid_search_respects_student_prefilter(self):
        chunks = build_chunks(SAMPLE_NOTES)
        matches = hybrid_search(
            query="problemas con fórmula cuadrática",
            chunks=chunks,
            student_filter="Ana",
        )
        for chunk, _ in matches:
            self.assertEqual(chunk.student, "Ana")

    def test_hybrid_search_finds_relevant_dario_note_and_abstains_on_noise(self):
        chunks = build_chunks(SAMPLE_NOTES)
        matches = hybrid_search(
            query="¿Qué problemas tuvo Darío con la fórmula cuadrática?",
            chunks=chunks,
            student_filter="Darío",
        )
        self.assertTrue(len(matches) >= 1)
        self.assertEqual(matches[0][0].chunk_id, "note-1#c0")

        empty = hybrid_search(
            query="receta de paella valenciana",
            chunks=chunks,
            min_score=0.5,
        )
        prompt = build_grounded_prompt("receta de paella valenciana", empty)
        self.assertIn("NO_CONTEXT_FOUND", prompt)

    def test_dense_cosine_similarity(self):
        self.assertAlmostEqual(dense_cosine_similarity([1.0, 0.0], [1.0, 0.0]), 1.0)
        self.assertAlmostEqual(dense_cosine_similarity([1.0, 0.0], [0.0, 1.0]), 0.0)


if __name__ == "__main__":
    unittest.main()
