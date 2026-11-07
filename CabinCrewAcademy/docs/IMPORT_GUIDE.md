# Importing your questions

This guide explains how to get your study material into Cabin Crew Academy.

There are three ways:

1. **JSON files.** Best for large banks and for backups.
2. **CSV files.** Easiest if your questions are in Excel or Google Sheets.
3. **Manual entry** in the app. Use it to fix or add individual questions.

You can import several files at once. They're validated together, so a duplicate ID across two files is reported too.

---

## How to import

1. Open **Questions → Import / Export**. You can also use **Settings → Import database**, or **Import questions** on the dashboard.
2. Tap **Choose files** and select one or more `.json` or `.csv` files. Hold a file to multi-select it.
3. Read the **validation report**. It shows:
   - **Valid questions:** questions that passed every check.
   - **New:** questions whose ID isn't in your database yet.
   - **Already in database:** questions whose ID already exists.
   - **New categories:** categories that will be created.
   - **Errors:** each problem, with the file name and the exact row or JSON path. These questions will **not** be imported.
   - **Warnings:** non-blocking issues, such as an unknown difficulty value, which falls back to *medium*.
4. Choose one of these:
   - **Import N new (skip M existing):** adds only the new questions. Existing questions are left untouched.
   - **Import and overwrite M existing:** asks for confirmation first, then replaces the text, options, answer, explanation, difficulty and category of the existing questions with the file's version. **Study progress for those questions is kept.**
   - **Cancel:** nothing is saved.

Nothing is written to the database until you press one of the import buttons.

---

## JSON format

```json
{
  "categories": [
    {
      "name": "Aviation Knowledge",
      "description": "Optional – shown on the Categories screen",
      "questions": [
        {
          "id": "Q001",
          "question": "Example question?",
          "options": ["Option A", "Option B", "Option C", "Option D"],
          "correctAnswer": 1,
          "explanation": "Explanation of the correct answer.",
          "difficulty": "medium"
        }
      ]
    },
    {
      "name": "First Aid",
      "questions": [ ... ]
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `categories[].name` | ✅ | Matched to existing categories without regard to case. A new category is created if no match is found. |
| `categories[].description` | – | Optional text. |
| `id` | recommended | A unique identifier such as `Q001` or `FA-012`. If you leave it out, the app generates a stable ID from the category and question text, so re-importing the same file is still detected as a duplicate. |
| `question` | ✅ | The question text. |
| `options` | ✅ | Between **2 and 10** non-empty answers. |
| `correctAnswer` | ✅ | The **zero-based index** of the correct option: `0` for the first, `1` for the second, `2` for the third, and so on. |
| `explanation` | – | Shown after you answer in study mode and in the exam correction report. |
| `difficulty` | – | `easy`, `medium` (the default) or `hard`. `1` / `2` / `3` also work. |

**Alternative flat layout.** You can also give a single `questions` array where each question carries its own `category`:

```json
{ "questions": [ { "category": "English", "id": "EN1", "question": "…", "options": ["…","…"], "correctAnswer": 0 } ] }
```

### Index reminder

```
"options": ["Paris", "London", "Madrid", "Rome"]
             0        1         2         3
"correctAnswer": 1   →   London
```

---

## CSV format

The first row must be a header. Column names aren't case sensitive, and the column order doesn't matter.

```csv
id,category,question,option1,option2,option3,option4,correctAnswer,explanation,difficulty
Q001,Aviation Knowledge,Example question?,Option A,Option B,Option C,Option D,1,Explanation of the correct answer.,medium
Q002,First Aid,Another question?,Yes,No,,,0,,easy
```

- **Required columns:** `category`, `question`, `correctAnswer` and at least `option1` and `option2`.
- **Optional columns:** `id`, `explanation`, `difficulty`.
- **More options:** add `option5`, `option6` … up to `option10`. Leave unused option cells empty at the end of the row. The second example row above has only two options.
- **`optionA`, `optionB` …** are accepted as column names too.
- **`correctAnswer`** is the zero-based index (`0` = option1). A letter **`A`–`J`** also works (`A` = option1).
- **Separators:** a comma, semicolon (the Excel default in Portugal and much of Europe) or tab is detected automatically.
- **Quotes:** values containing the separator, quotes or line breaks must be wrapped in double quotes, and a quote inside a value is written twice (`""`). Excel and Google Sheets do this automatically.
- **Encoding:** UTF-8 is recommended. Files saved by older Excel versions (Windows-1252) are detected and read correctly, including accented characters.

### Creating a CSV in Excel or Google Sheets

1. Put the header row in row 1 and one question per row.
2. Excel: **File → Save As → CSV UTF-8 (Comma delimited)**. Google Sheets: **File → Download → CSV**.
3. Copy the file to the tablet and import it.

Ready-made templates: [`templates/questions_template.csv`](templates/questions_template.csv) (comma) and [`templates/questions_template_semicolon.csv`](templates/questions_template_semicolon.csv).

---

## Common validation errors

| Message | Fix |
|---|---|
| `Invalid JSON: …` | There's a syntax error, such as a missing comma, bracket or quote. Paste the file into a JSON validator to find it. |
| `"correctAnswer" is 4 but valid values are 0..3 (index starts at 0)` | You counted from 1. Subtract 1. |
| `needs at least 2 options` | Add more options, or check that the option columns are filled in. |
| `option 2 is empty but a later option is filled` | There's a gap between options. Move the options left. |
| `Duplicate id "Q010"` | Two questions share an ID. Give each question a unique ID. |
| `Missing column(s): "correctAnswer"` | Fix the CSV header spelling. |
| `Unknown difficulty "x" – using Medium` | This is only a warning. Use `easy`, `medium` or `hard`. |

---

## Manual creation and editing

- **Questions → New question.** Enter an ID (a free `M0001`-style ID is suggested), choose a category (or create one from the list), type the question, fill in 2–10 options, tick the radio button next to the correct answer, and optionally add an explanation and a difficulty. Then tap **Save**.
- **Edit:** tap any question in the question bank.
- **Delete:** open the question and tap the 🗑 icon.
