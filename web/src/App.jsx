import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { findSegment, formatTimestamp, sectionDefinitions } from "./data/fixtures";
import { MyLectures } from "./components/MyLectures";

const API_BASE_URL = "/api";

function SourceReferences({ references, segments, onReferenceClick }) {
  if (!references?.length) return null;
  return (
    <div className="source-references" aria-label="Transcript sources">
      <span className="source-label">Sources</span>
      {references.map((reference) => {
        const segment = findSegment(segments, reference.segment_id);
        return (
          <button
            className="source-link"
            key={reference.segment_id}
            onClick={() => onReferenceClick(reference.segment_id)}
            type="button"
            title={
              segment
                ? `Open transcript at ${formatTimestamp(segment.start)}`
                : "Transcript segment unavailable"
            }
          >
            {segment ? formatTimestamp(segment.start) : `#${reference.segment_id}`}
          </button>
        );
      })}
    </div>
  );
}

function SectionEmptyState({ label }) {
  return <div className="empty-state">No {label.toLowerCase()} identified in this lecture.</div>;
}

function ItemList({ items, label, segments, onReferenceClick }) {
  if (!items.length) return <SectionEmptyState label={label} />;
  return (
    <div className="item-list">
      {items.map((item, index) => (
        <article className="study-item" key={`${item.content}-${index}`}>
          <span className="item-number">{String(index + 1).padStart(2, "0")}</span>
          <div>
            <p>{item.content}</p>
            <SourceReferences references={item.source_refs} segments={segments} onReferenceClick={onReferenceClick} />
          </div>
        </article>
      ))}
    </div>
  );
}

function Definitions({ items, segments, onReferenceClick }) {
  if (!items.length) return <SectionEmptyState label="definitions" />;
  return (
    <div className="definition-list">
      {items.map((item) => (
        <article className="definition" key={item.term}>
          <div className="definition-term">{item.term}</div>
          <div>
            <p>{item.explanation}</p>
            <SourceReferences references={item.source_refs} segments={segments} onReferenceClick={onReferenceClick} />
          </div>
        </article>
      ))}
    </div>
  );
}

function Questions({ items, segments, onReferenceClick }) {
  if (!items.length) return <SectionEmptyState label="exam questions" />;
  return (
    <div className="question-list">
      {items.map((item, index) => (
        <article className="question" key={`${item.question}-${index}`}>
          <div className="question-topline">
            <span>Question {String(index + 1).padStart(2, "0")}</span>
            <b>{item.type}</b>
          </div>
          <p>{item.question}</p>
          <SourceReferences references={item.source_refs} segments={segments} onReferenceClick={onReferenceClick} />
        </article>
      ))}
    </div>
  );
}

function TranscriptDrawer({ activeSegmentId, segments, onClose }) {
  const segmentRefs = useRef({});
  const activeSegment = findSegment(segments, activeSegmentId);
  useEffect(() => {
    if (activeSegmentId !== null) {
      segmentRefs.current[activeSegmentId]?.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }, [activeSegmentId]);

  return (
    <aside className="transcript-drawer" aria-label="Transcript panel">
      <div className="drawer-header">
        <div>
          <span className="eyebrow">Original source</span>
          <h2>Transcript</h2>
        </div>
        <button className="icon-button" onClick={onClose} type="button" aria-label="Close transcript">
          ×
        </button>
      </div>
      {activeSegment ? (
        <div className="active-source">
          Showing segment {activeSegment.id} at {formatTimestamp(activeSegment.start)}
        </div>
      ) : (
        <div className="missing-source">The referenced transcript segment is unavailable.</div>
      )}
      <div className="transcript-list">
        {segments.map((segment) => (
          <div
            className={`transcript-segment ${segment.id === activeSegmentId ? "is-active" : ""}`}
            key={segment.id}
            ref={(element) => {
              segmentRefs.current[segment.id] = element;
            }}
          >
            <div className="timestamp">
              <span>{formatTimestamp(segment.start)}</span>
              <span>{formatTimestamp(segment.end)}</span>
            </div>
            <p>{segment.text}</p>
          </div>
        ))}
      </div>
    </aside>
  );
}

function App() {
  const [selectedLectureId, setSelectedLectureId] = useState(null);
  const [lectureDetail, setLectureDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(null);
  const [lectures, setLectures] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Existing study UI state
  const [activeSection, setActiveSection] = useState("overview");
  const [studySearch, setStudySearch] = useState("");
  const [transcriptOpen, setTranscriptOpen] = useState(false);
  const [activeSegmentId, setActiveSegmentId] = useState(null);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const fetchLecturesAndSubjects = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [lecturesRes, subjectsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/lectures`),
        fetch(`${API_BASE_URL}/subjects`),
      ]);

      if (!lecturesRes.ok) {
        throw new Error(`Lectures API returned status ${lecturesRes.status}`);
      }

      const lecturesData = await lecturesRes.json();
      const subjectsData = subjectsRes.ok ? await subjectsRes.json() : [];

      setLectures(lecturesData);
      setSubjects(subjectsData);
    } catch (err) {
      setError(err.message || "Failed to load data");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLecturesAndSubjects();
  }, [fetchLecturesAndSubjects]);

  useEffect(() => {
    if (selectedLectureId === null) {
      setLectureDetail(null);
      setDetailError(null);
      return;
    }

    let cancelled = false;
    setDetailLoading(true);
    setDetailError(null);
    setLectureDetail(null);
    fetch(`${API_BASE_URL}/lectures/${selectedLectureId}`)
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Lecture API returned status ${response.status}`);
        }
        return response.json();
      })
      .then((data) => {
        if (!cancelled) setLectureDetail(data);
      })
      .catch((err) => {
        if (!cancelled) setDetailError(err.message || "Failed to load lecture");
      })
      .finally(() => {
        if (!cancelled) setDetailLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [selectedLectureId]);

  const handleDeleteLecture = async (lectureId) => {
    const res = await fetch(`${API_BASE_URL}/lectures/${lectureId}`, {
      method: "DELETE",
    });
    if (!res.ok) {
      throw new Error(`Failed to delete lecture: ${res.status}`);
    }
    setLectures((prev) => prev.filter((lec) => lec.id !== lectureId));
    if (selectedLectureId === lectureId) setSelectedLectureId(null);
  };

  const material = lectureDetail?.material || {};
  const segments = lectureDetail?.segments || [];
  const metadata = {
    duration: lectureDetail?.duration_seconds || 0,
    segmentCount: segments.length,
    referenceCount: Object.values(material)
      .filter(Array.isArray)
      .reduce((total, items) => total + items.reduce((count, item) => count + (item.source_refs?.length || 0), 0), 0),
  };

  const sectionMatches = useMemo(() => {
    const query = studySearch.trim().toLowerCase();
    if (!query) return {};
    return Object.fromEntries(
      sectionDefinitions.slice(1).map(({ id }) => [
        id,
        (material[id] || []).filter((item) => {
          const searchable =
            id === "definitions"
              ? `${item.term} ${item.explanation}`
              : id === "exam_questions"
              ? `${item.question} ${item.type}`
              : item.content;
          return searchable.toLowerCase().includes(query);
        }),
      ])
    );
  }, [material, studySearch]);

  const totalMatches = Object.values(sectionMatches).reduce(
    (total, matches) => total + matches.length,
    0
  );

  function openSource(segmentId) {
    setActiveSegmentId(findSegment(segments, segmentId) ? segmentId : null);
    setTranscriptOpen(true);
  }

  function selectSection(sectionId) {
    setActiveSection(sectionId);
    setMobileNavOpen(false);
  }

  function updateStudySearch(value) {
    setStudySearch(value);
    if (!value.trim()) return;
    const query = value.trim().toLowerCase();
    const firstMatch = sectionDefinitions.slice(1).find(({ id }) =>
      (material[id] || []).some((item) => {
        const searchable =
          id === "definitions"
            ? `${item.term} ${item.explanation}`
            : id === "exam_questions"
            ? `${item.question} ${item.type}`
            : item.content;
        return searchable.toLowerCase().includes(query);
      })
    );
    if (firstMatch) setActiveSection(firstMatch.id);
  }

  const section = sectionDefinitions.find((item) => item.id === activeSection);
  const currentItems = studySearch.trim()
    ? sectionMatches[activeSection] || []
    : material[activeSection] || [];

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-left">
          <button
            className="brand brand-button"
            onClick={() => setSelectedLectureId(null)}
            type="button"
            aria-label="NoteCompanion Home"
          >
            <span className="brand-mark">ON</span>
            <span>Offline Notes</span>
          </button>
          <nav className="topbar-nav" aria-label="Main Navigation">
            <button
              className={`nav-link ${selectedLectureId === null ? "is-active" : ""}`}
              onClick={() => setSelectedLectureId(null)}
              type="button"
            >
              My Lectures
            </button>
          </nav>
        </div>

        <div className="topbar-context">
          {selectedLectureId !== null ? (
            <button
              className="back-link-btn"
              onClick={() => setSelectedLectureId(null)}
              type="button"
            >
              ← Back to My Lectures
            </button>
          ) : (
            <>
              <span className="status-dot" /> Student Workspace
            </>
          )}
        </div>

        {selectedLectureId !== null && (
          <button
            className="mobile-menu-button"
            onClick={() => setMobileNavOpen((open) => !open)}
            type="button"
          >
            Sections <span>☰</span>
          </button>
        )}
      </header>

      {selectedLectureId === null ? (
        <MyLectures
          lectures={lectures}
          subjects={subjects}
          loading={loading}
          error={error}
          onSelectLecture={(id) => setSelectedLectureId(id)}
          onDeleteLecture={handleDeleteLecture}
          onRetry={fetchLecturesAndSubjects}
        />
      ) : detailLoading ? (
        <main className="workspace" aria-live="polite">
          <section className="notes-area">
            <p>Loading lecture...</p>
          </section>
        </main>
      ) : detailError ? (
        <main className="workspace" aria-live="polite">
          <section className="notes-area">
            <p>{detailError}</p>
          </section>
        </main>
      ) : lectureDetail ? (
        /* Reused intact existing lecture-study workspace */
        <main className="workspace">
          <section className="lecture-header">
            <div>
              <div className="breadcrumb">
                Lecture workspace <span>/</span> {lectureDetail.subject || "General"}
              </div>
              <h1>{lectureDetail.title}</h1>
              <p className="subtitle">
                Lecture-specific transcript and study material.
              </p>
            </div>
            <div className="lecture-stats">
              <div>
                <strong>{Math.round(metadata.duration / 60)} min</strong>
                <span>lecture length</span>
              </div>
              <div>
                <strong>{metadata.segmentCount}</strong>
                <span>transcript segments</span>
              </div>
              <div>
                <strong>{metadata.referenceCount}</strong>
                <span>source links</span>
              </div>
            </div>
          </section>

          <div className="workspace-grid">
            <nav className={`section-nav ${mobileNavOpen ? "is-open" : ""}`} aria-label="Study sections">
              <div className="nav-heading">Study guide</div>
              {sectionDefinitions.map((item) => {
                const count =
                  item.id === "overview"
                    ? null
                    : (studySearch.trim()
                        ? sectionMatches[item.id]?.length
                        : material[item.id]?.length) || 0;
                return (
                  <button
                    className={`section-button ${activeSection === item.id ? "is-active" : ""}`}
                    key={item.id}
                    onClick={() => selectSection(item.id)}
                    type="button"
                  >
                    <span className="section-number">{item.icon}</span>
                    <span>{item.label}</span>
                    {count !== null && <em>{count}</em>}
                  </button>
                );
              })}
              <div className="nav-footer">
                <span className="legend-dot" /> Grounded in transcript
                <br />
                <small>{metadata.referenceCount} verified references</small>
              </div>
            </nav>

            <section className="notes-area" aria-live="polite">
              <div className="notes-toolbar">
                <div>
                  <span className="eyebrow">Section {section.icon}</span>
                  <h2>{section.label}</h2>
                </div>
                <label className="search-box">
                  <span>⌕</span>
                  <input
                    aria-label="Search study material"
                    onChange={(event) => updateStudySearch(event.target.value)}
                    placeholder="Search notes"
                    value={studySearch}
                  />
                  <kbd>⌘ K</kbd>
                </label>
              </div>

              {studySearch.trim() && (
                <div className="search-summary">
                  {totalMatches} {totalMatches === 1 ? "match" : "matches"} across the study guide{" "}
                  <button onClick={() => setStudySearch("")} type="button">
                    Clear search
                  </button>
                </div>
              )}

              {activeSection === "overview" ? (
                <div className="overview-content">
                  <div className="overview-lead">
                    <span className="section-kicker">Lecture overview</span>
                    <p>{material.lecture_overview || "No overview available."}</p>
                  </div>
                  <div className="summary-block">
                    <span className="section-kicker">In brief</span>
                    <p>{material.summary || "No summary available."}</p>
                  </div>
                  <div className="overview-callout">
                    <span>Study note</span>
                    <p>Use the source links beside each idea to see the original wording and timestamp.</p>
                  </div>
                </div>
              ) : activeSection === "definitions" ? (
                <Definitions items={currentItems} segments={segments} onReferenceClick={openSource} />
              ) : activeSection === "exam_questions" ? (
                <Questions items={currentItems} segments={segments} onReferenceClick={openSource} />
              ) : (
                <ItemList items={currentItems} label={section.label} segments={segments} onReferenceClick={openSource} />
              )}
            </section>
          </div>
        </main>
      ) : null}

      {selectedLectureId !== null && transcriptOpen && (
        <TranscriptDrawer activeSegmentId={activeSegmentId} segments={segments} onClose={() => setTranscriptOpen(false)} />
      )}
      {selectedLectureId !== null && transcriptOpen && (
        <button
          className="drawer-backdrop"
          aria-label="Close transcript"
          onClick={() => setTranscriptOpen(false)}
          type="button"
        />
      )}
    </div>
  );
}

export default App;
