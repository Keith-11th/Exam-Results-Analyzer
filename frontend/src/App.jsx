import { useEffect, useState } from "react";
import "./App.css";

const API = "http://localhost:8080/api";

const emptyForm = {
  learnerId: "",
  name: "",
  className: "Grade 7",
  subject: "",
  score: "",
};

export default function App() {
  const [results, setResults] = useState([]);
  const [analysis, setAnalysis] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");

  async function loadResults() {
    try {
      const res = await fetch(`${API}/results`);
      setResults(await res.json());
      setError("");
    } catch {
      setError("Cannot reach the backend. Is it running on port 8080?");
      return;
    }
    try {
      const res = await fetch(`${API}/analysis`);
      setAnalysis(res.ok ? await res.json() : null);
    } catch {
      setAnalysis(null);
    }
  }

  useEffect(() => {
    loadResults();
  }, []);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const score = Number(form.score);
    if (score < 0 || score > 100 || Number.isNaN(score)) {
      setError("Score must be a number between 0 and 100.");
      return;
    }
    try {
      await fetch(`${API}/marks`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...form, score }),
      });
      setForm({ ...emptyForm, className: form.className });
      loadResults();
    } catch {
      setError("Could not save the mark.");
    }
  }

  const selected = results.find((r) => r.learnerId === selectedId);
  const comment = analysis?.learners.find(
    (l) => l.learnerId === selectedId
  )?.comment;
  const summary = analysis?.summary;

  return (
    <div className="app">
      <header>
        <h1>Exam Results Analyzer</h1>
        <p>Enter marks, see class rankings and individual reports.</p>
      </header>

      {error && <div className="error">{error}</div>}

      <section className="card">
        <h2>Add or update a mark</h2>
        <form onSubmit={handleSubmit} className="form">
          <input name="learnerId" placeholder="Learner ID (e.g. L006)"
                 value={form.learnerId} onChange={handleChange} required />
          <input name="name" placeholder="Learner name"
                 value={form.name} onChange={handleChange} required />
          <input name="className" placeholder="Class"
                 value={form.className} onChange={handleChange} required />
          <input name="subject" placeholder="Subject"
                 value={form.subject} onChange={handleChange} required />
          <input name="score" type="number" placeholder="Score /100"
                 value={form.score} onChange={handleChange} required />
          <button type="submit">Save mark</button>
        </form>
      </section>

      {summary && summary.learners > 0 && (
        <section className="card">
          <h2>Class insights</h2>
          <div className="stats">
            <div className="stat">
              <span>Class average</span>
              <strong>{summary.classAverage}</strong>
            </div>
            <div className="stat">
              <span>Hardest subject</span>
              <strong>{summary.hardestSubject}</strong>
            </div>
            <div className="stat">
              <span>Easiest subject</span>
              <strong>{summary.easiestSubject}</strong>
            </div>
          </div>

          <h3>Subject averages</h3>
          {Object.entries(analysis.subjects).map(([subject, s]) => (
            <div key={subject} className="bar-row">
              <span className="bar-label">{subject}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${s.average}%` }} />
              </div>
              <span className="bar-score">{s.average}</span>
            </div>
          ))}

          <h3>Grade spread</h3>
          <div className="grade-spread">
            {Object.entries(summary.gradeCounts).sort().map(([g, n]) => (
              <span key={g} className={`grade grade-${g}`}>{g}: {n}</span>
            ))}
          </div>

          <h3>Needs attention</h3>
          {summary.needsAttention.length === 0 ? (
            <p className="hint">No learner is below 50% average.</p>
          ) : (
            <ul className="attention">
              {summary.needsAttention.map((n) => <li key={n}>{n}</li>)}
            </ul>
          )}
        </section>
      )}

      <section className="card">
        <h2>Class ranking</h2>
        <table>
          <thead>
            <tr>
              <th>Pos</th><th>Name</th><th>Class</th>
              <th>Total</th><th>Average</th><th>Grade</th>
            </tr>
          </thead>
          <tbody>
            {results.map((r) => (
              <tr key={r.learnerId}
                  className={r.learnerId === selectedId ? "active" : ""}
                  onClick={() => setSelectedId(r.learnerId)}>
                <td>{r.position}</td>
                <td>{r.name}</td>
                <td>{r.className}</td>
                <td>{r.total}</td>
                <td>{r.average}</td>
                <td><span className={`grade grade-${r.grade}`}>{r.grade}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="hint">Click a row to open that learner's report.</p>
      </section>

      {selected && (
        <section className="card report">
          <h2>Report: {selected.name}</h2>
          <p>
            Position <strong>{selected.position}</strong> of {results.length}
            {" · "}Average <strong>{selected.average}</strong>
            {" · "}Grade <strong>{selected.grade}</strong>
          </p>
          {Object.entries(selected.subjects).map(([subject, score]) => (
            <div key={subject} className="bar-row">
              <span className="bar-label">{subject}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${score}%` }} />
              </div>
              <span className="bar-score">{score}</span>
            </div>
          ))}
          {comment && <p className="comment">{comment}</p>}
          <button className="print" onClick={() => window.print()}>
            Print report
          </button>
        </section>
      )}
    </div>
  );
}