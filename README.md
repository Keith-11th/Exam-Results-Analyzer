# Exam Results Analyzer

A small full-stack web app that helps teachers turn raw exam marks into class rankings, grades and written learner comments. It was built to replace hours of manual marking summaries.

It combines three languages, each with a clear job:

| Part | Technology | What it does |
|------|-----------|--------------|
| `backend/` | Java, Spring Boot | Stores marks, calculates totals, averages, grades and class positions, and exposes a REST API |
| `frontend/` | React (Vite) | Mark entry form, ranked class table, learner reports and class insights |
| `analytics/` | Python | Analyses the class results and writes a personalised comment for each learner |

## Features

- Add or update a learner's mark for any subject
- Automatic total, average, grade (A to F) and class position
- Click a learner to open an individual report with subject bars
- Print a learner's report straight from the browser
- Class insights: class average, hardest and easiest subject, subject averages, grade spread
- Learners needing attention (average below 50%) are flagged
- Auto-written comment for each learner covering strongest subject, weakest subject, comparison to the class average, and a suggested next step

## Project structure

```
exam-results-analyzer/
├── backend/     Spring Boot REST API (Java)
├── frontend/    React app (Vite)
├── analytics/   Python analysis script (analyze.py)
└── README.md
```

## Requirements

- Java 17 or newer
- Node.js 20 or newer
- Python 3 (standard library only, nothing to install)

## How to run

You need two terminal windows, one for the backend and one for the frontend.

**1. Clone the project**

```
git clone https://github.com/Keith-11th/Exam-Results-Analyzer.git
cd Exam-Results-Analyzer
```

**2. Start the backend (terminal 1)**

```
cd backend
mvnw spring-boot:run
```

On Mac or Linux, use `./mvnw spring-boot:run`. Wait for the line `Started BackendApplication`. The API runs on `http://localhost:8080`.

Start the backend from inside the `backend` folder, because it finds the Python script at `../analytics/analyze.py`.

**3. Start the frontend (terminal 2)**

```
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

The app starts with 5 sample learners so there is something to see straight away.

## API endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/marks` | Add or update one mark |
| GET | `/api/results` | All learners with total, average, grade and position |
| GET | `/api/results/{id}` | One learner's report |
| GET | `/api/analysis` | Class insights and learner comments, produced by the Python script |

Example body for `POST /api/marks`:

```json
{
  "learnerId": "L006",
  "name": "Grace Atim",
  "className": "Grade 7",
  "subject": "Math",
  "score": 74
}
```

## How the three parts connect

1. The React app calls the Java API for results and analysis.
2. When `/api/analysis` is requested, Java runs `analytics/analyze.py`.
3. The Python script reads the current results from the Java API, works out the statistics and comments, and prints JSON.
4. Java returns that JSON to React, which displays it.

If `python` is not recognised on your machine, change the `PYTHON` constant in `ResultsController.java` to `py` (Windows) or `python3` (Mac/Linux).

## Grading scale

| Average | Grade |
|---------|-------|
| 80 and above | A |
| 70 to 79 | B |
| 60 to 69 | C |
| 50 to 59 | D |
| 40 to 49 | E |
| Below 40 | F |

The scale lives in `gradeFor` in `ResultsService.java` and can be changed to match any school's system.

## Known limitations

- Marks are stored in memory, so they reset when the backend restarts
- Positions are based on total marks across subjects
- No login, so it is meant for a single teacher on a local machine

## Ideas for the future

- Save marks to a file or database
- Upload marks from a CSV file
- Per-class filtering and multiple terms
- Export the class report as a PDF

## Author

Keith Douglas Kirabo, software engineering student at Victoria University, Kampala.