---
name: reviewer-generation-quality
description: Enforce high-precision, hallucination-free reviewer and quiz generation across definition, enumeration, multiple-choice, true/false, fill-in-the-blanks, matching, and sequencing formats with strict source grounding.
---

# Reviewer Generation Quality

## 1. Core Principles

The reviewer engine exists to convert imported study materials into rigorous, dependable questions for active recall and spaced repetition.

Strict constraints:
- Precision over volume: 10 rock-solid, unambiguous questions are vastly superior to 50 noisy, trivial, or hallucinated questions.
- Pure source grounding: every question, answer key, and explanation must be directly traceable to an identified source paragraph or sentence.
- Zero invented facts: if a document does not explicitly provide sufficient context to make an answer unambiguous, discard that candidate item.
- Anti-trivia: test core concepts, definitions, operational mechanisms, principles, and taxonomies; avoid arbitrary numbers or formatting artifacts.

---

## 2. Question Types and Algorithmic Construction

### 1. Definition and Identification
- Extraction Pattern: Detect subject-predicate structures (`X is defined as Y`, `X refers to Y`, `X is a Y that Z`, or markdown bolding `**Term**: Definition`).
- Flashcard & Identification Generation:
  - Prompt: "What term describes: [Definition with the term replaced]?"
  - Key: Exact canonical term with allowable case-insensitive aliases or abbreviations.
- Validation: The definition must uniquely identify the target term without overlapping with neighboring terms in the same section.

### 2. Enumeration
- Extraction Pattern: Detect numbered or bulleted lists following an introductory colon (e.g., `The four stages of mitosis are: 1. Prophase, 2. Metaphase...`).
- Question Formats:
  - Complete recall: "List the [N] components of [Category]."
  - Partial recall / spot check: "Which of the following is one of the [N] stages of [Category]?"
- Validation: Ensure all list elements belong strictly to the enumerated category.

### 3. Multiple Choice Questions (MCQ)
- Structure:
  - Stem: A direct, unambiguous question or incomplete sentence.
  - Correct Answer: Verifiably true according to the source text.
  - Distractors: Exactly 3 plausible distractors (total 4 choices).
- Distractor Construction Rules:
  - Same Category: Distractors must be drawn from parallel entities in the same document (e.g., if the answer is "Mitochondria", distractors should be "Ribosome", "Endoplasmic Reticulum", "Golgi Apparatus").
  - Similar Syntactic Length: Prevent the correct answer from being noticeably longer or more detailed than the distractors.
  - Ban Clichés: Never generate "All of the above", "None of the above", or humorous/nonsensical options.
  - Uniqueness: Ensure distractors are mutually exclusive and genuinely incorrect for the specific stem.

### 4. True / False
- Structure: Clear, declarative statement that is definitively true or false.
- Rules for False Statements:
  - Modify a single critical attribute, condition, or term from a true sentence (e.g., swapping an antonym, changing a direction, or swapping two related entities).
  - Never introduce double negatives or convoluted grammatical inversions.
  - Always generate a concise explanation specifying why the false statement is incorrect and what the true fact is.

### 5. Fill-in-the-Blanks (Cloze)
- Sentence Selection: Select sentences where removing a single word or compound phrase leaves unambiguous grammatical and contextual clues.
- Target Selection: Blank out key semantic nouns, verbs, or domain terms.
- Negative Rules: Never blank out function words (`the`, `and`, `of`, `when`), numbers without context, or ambiguous common words.
- Single Answer Guarantee: Provide acceptable canonical variants or synonyms if multiple forms exist (e.g., singular vs plural, abbreviations).

### 6. Matching Pairs
- Structure: A group of 4 to 6 terms paired with their corresponding definitions, symbols, or functions.
- Constraint: Never present more than 6 pairs in a single question to avoid cognitive overload on mobile screens.
- Randomization: Scramble the targets deterministically so no pair matches by default index.

### 7. Sequencing / Chronology
- Extraction Pattern: Processes with explicit step indicators (`First`, `Next`, `Then`, `Finally`) or historical/scientific stages.
- Structure: Present 3 to 6 unordered steps; require the learner to arrange them in proper logical or chronological order.

---

## 3. Grounding and Provenance Tracking

Every generated question data model must store its source lineage:

```kotlin
data class GeneratedQuestion(
    val id: String,
    val documentId: Long,
    val sourceChunkIndex: Int,
    val pageNumber: Int?,
    val slideNumber: Int?,
    val sectionTitle: String?,
    val sourceExcerpt: String,
    val type: QuestionType,
    val stem: String,
    val correctAnswer: String,
    val options: List<String>?,
    val explanation: String,
    val difficultyScore: Float
)
```

Rules:
- `sourceExcerpt`: Must preserve the exact sentence(s) from which the question was derived.
- `explanation`: Must reference the source excerpt to give instant feedback during active recall sessions.

---

## 4. Deduplication and Similarity Filtering

To avoid repetitive questions:
1. Lexical Similarity: Compare question stems using Jaccard token overlap or normalized Levenshtein distance.
2. Threshold: If similarity between two question stems exceeds 0.75, discard the lower-quality candidate.
3. Answer Overlap: Never generate two identical question stems with identical correct answers in the same generated deck.
4. Concept Distribution: Cap question density at a maximum of 3 questions per source paragraph to ensure balanced topic coverage across the document.

---

## 5. Reviewer Quality Verification Checklist

Before emitting a generated question deck, run this verification pass:
- [ ] Is every fact directly supported by the source excerpt?
- [ ] Is the question stem unambiguous and free of grammatical errors?
- [ ] For multiple choice: are all 3 distractors plausible yet undeniably false?
- [ ] For fill-in-the-blanks: is the missing blank a critical domain keyword?
- [ ] Does the explanation clearly justify why the correct answer is right?
- [ ] Are duplicate and near-duplicate stems eliminated?
