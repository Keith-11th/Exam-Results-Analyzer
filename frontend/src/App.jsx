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
          <button className="print" onClick={() => window.print()}>
            Print report
          </button>
        </section>
      )}
    </div>
  );
}