# QuestionsGenerators — How CalculationGenerator Works

## Overview

`CalculationGenerator` takes a **template string** (e.g. `"X + X * X"`) from the database,
fills it with random numbers, evaluates it respecting standard math operator precedence,
and returns a `Question` entity with the expression, correct answer, step-by-step solution,
and (optionally) multiple-choice options.

---

## Step-by-step flow

### 1. Get a template from the DB

A template is a string where each operand is the placeholder `X` and operators are spaced out:

```
"X + X"        → simple addition
"X * X - X"    → multiply then subtract
"X + X * X"    → mixed, precedence matters
```

Templates are stored in the `question_template` table, linked to a `SubSubject` and a difficulty level.
If no template matches the requested difficulty, the generator falls back to the hardest available one.

---

### 2. Replace every X with a random number (Step 1 in code)

The template is split by spaces into a token list: `["X", "+", "X", "*", "X"]`

Each `X` is replaced with a random integer. The range depends on the context:
- If the `X` is **next to a `*` operator** → small number (2 to `difficulty * 2`) to keep results reasonable
- Otherwise → normal range (`subSubjectLevel + difficulty` to `subSubjectLevel + difficulty * 2`)

After this step, the token list might look like: `["5", "+", "7", "*", "3"]`

The expression string is saved **here**, before any evaluation — this is the exact string the student sees:
```
expression = "5 + 7 * 3"
```

---

### 3. Evaluate `*` and `/` first (Step 2 in code)

The token list is scanned left-to-right. When a `*` or `/` is found:

1. Take the numbers to its left and right
2. For `/`: check if `left % right == 0`. If not, replace `right` with a random divisor of `left` so the result is always a whole number
3. Compute the result
4. Add a solution step: `"7 * 3 = 21"`
5. Collapse the three tokens (`left`, `op`, `right`) into the single result

Repeat until no `*` or `/` remain. After this pass: `["5", "+", "21"]`

---

### 4. Evaluate `+` and `-` (Step 3 in code)

Same scan, but now only addition and subtraction remain:

1. Take left and right numbers
2. Compute the result
3. Add a solution step: `"5 + 21 = 26"`
4. Collapse to a single token

After this pass: `["26"]` → `sum = 26`

---

### 5. Build multiple-choice options (if requested)

Four options are generated using a `LinkedHashSet` to prevent duplicates:

- The **correct answer is always included first**
- Three distractors are picked by shifting the correct answer by a random amount within ±15% spread
- The final list is shuffled so the correct answer isn't always in the same position

---

### 6. Return the Question

A `Question` entity is built with:

| Field           | Value                                      |
|-----------------|--------------------------------------------|
| `expression`    | The filled template string (`"5 + 7 * 3"`) |
| `correctAnswer` | The evaluated result (`"26"`)              |
| `solution`      | Steps in order: `["7 * 3 = 21", "5 + 21 = 26"]` |
| `options`       | 4 shuffled choices including the answer, or `null` |
| `difficultyLevel` | As requested                             |
| `language`      | As requested                               |

---

## Division — why it always gives a whole number

Division is fixed **at evaluation time** (Step 2), not during X replacement.
This matters because a template like `X * X / X` means the dividend is the *result of the multiplication*,
not a plain filled X. By the time the evaluator reaches `/`, it already knows the real left-hand value
and can pick a proper divisor.

```
Template:  X * X / X
Filled:    6 * 4 / X
Step 2:    6 * 4 = 24  →  ["24", "/", "X"]
           24 / X: picks a divisor of 24 (e.g. 6)  →  24 / 6 = 4
```

---

## Adding new templates

Templates live in the `question_template` table. Each row has:
- `sub_subject_id` — which operation (add, sub, mult, div, mixed)
- `expression` — the template string with `X` placeholders
- `difficulty_level` — integer (1 = easy, 10 = hard)

`DataInitializer` seeds a few defaults on startup when `SEED_DATA=true`.
You can add more rows directly in the DB or via a future admin endpoint.
