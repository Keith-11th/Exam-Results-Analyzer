"""Exam Results Analyzer - analysis helper.
Reads results (from the Java backend or from standard input) and prints a JSON analysis."""
import json
import sys
import urllib.request

API = "http://localhost:8080/api/results"


def load_results():
    if "--stdin" in sys.argv:
        return json.loads(sys.stdin.buffer.read())
    with urllib.request.urlopen(API, timeout=10) as response:
        return json.load(response)


def build_comment(r, best, weakest, class_avg, total):
    first = r["name"].split()[0]
    avg = r["average"]
    grade = r["grade"]
    scores = r["subjects"]

    openings = {
        "A": f"{first} is performing excellently",
        "B": f"{first} is performing very well",
        "C": f"{first} is performing satisfactorily",
        "D": f"{first} is performing fairly but has room to improve",
        "E": f"{first} is performing below the expected level",
        "F": f"{first} is struggling and needs urgent support",
    }
    parts = [
        f"{openings[grade]}, with an average of {avg:g}% (grade {grade}) "
        f"and position {r['position']} of {total}."
    ]

    if best != weakest:
        if scores[weakest] >= 70:
            weak_text = f"Relatively weaker in {weakest} ({scores[weakest]:g}), though still strong."
        else:
            weak_text = f"Needs most support in {weakest} ({scores[weakest]:g})."
        parts.append(f"Strongest subject: {best} ({scores[best]:g}). {weak_text}")

    diff = round(avg - class_avg, 1)
    if diff > 0:
        parts.append(f"This is {diff:g} points above the class average.")
    elif diff < 0:
        parts.append(f"This is {abs(diff):g} points below the class average.")
    else:
        parts.append("This is exactly the class average.")

    if avg < 50:
        parts.append("Recommend extra practice and a follow-up with the parent or guardian.")
    elif scores[weakest] < 50:
        parts.append(f"Focus revision on {weakest}.")
    else:
        parts.append("Keep up the good work.")

    return " ".join(parts)


def analyze(results):
    if not results:
        return {"summary": {"learners": 0}, "subjects": {}, "learners": []}

    by_subject = {}
    for r in results:
        for subject, score in r["subjects"].items():
            by_subject.setdefault(subject, []).append(score)

    subject_stats = {
        s: {
            "average": round(sum(v) / len(v), 1),
            "highest": max(v),
            "lowest": min(v),
        }
        for s, v in by_subject.items()
    }

    class_avg = round(sum(r["average"] for r in results) / len(results), 1)
    hardest = min(subject_stats, key=lambda s: subject_stats[s]["average"])
    easiest = max(subject_stats, key=lambda s: subject_stats[s]["average"])

    grade_counts = {}
    for r in results:
        grade_counts[r["grade"]] = grade_counts.get(r["grade"], 0) + 1

    learners = []
    for r in results:
        scores = r["subjects"]
        best = max(scores, key=scores.get)
        weakest = min(scores, key=scores.get)
        learners.append({
            "learnerId": r["learnerId"],
            "name": r["name"],
            "strongest": best,
            "weakest": weakest,
            "comment": build_comment(r, best, weakest, class_avg, len(results)),
        })

    return {
        "summary": {
            "learners": len(results),
            "classAverage": class_avg,
            "hardestSubject": hardest,
            "easiestSubject": easiest,
            "gradeCounts": grade_counts,
            "needsAttention": [r["name"] for r in results if r["average"] < 50],
        },
        "subjects": subject_stats,
        "learners": learners,
    }


if __name__ == "__main__":
    try:
        data = load_results()
    except Exception as exc:
        print(f"Could not load results: {exc}", file=sys.stderr)
        sys.exit(1)
    print(json.dumps(analyze(data)))