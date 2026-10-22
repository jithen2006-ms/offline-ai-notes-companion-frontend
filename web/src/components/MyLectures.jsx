import { useMemo, useState } from "react";

const STATUS_MAP = {
  RECEIVED: { label: "Received", ready: false, style: "status-received", desc: "Audio received" },
  TRANSCRIBING: { label: "Transcribing lecture...", ready: false, style: "status-transcribing", desc: "Generating transcript..." },
  TRANSCRIBED: { label: "Transcript ready", ready: false, style: "status-transcribed", desc: "Transcript ready" },
  GENERATING_NOTES: { label: "Preparing study material...", ready: false, style: "status-generating", desc: "Extracting notes & concepts..." },
  READY: { label: "Ready to study", ready: true, style: "status-ready", desc: "Click to open study guide" },
  FAILED: { label: "Processing failed", ready: false, style: "status-failed", desc: "Processing could not complete" },
};

function formatDuration(seconds) {
  if (!seconds || isNaN(seconds)) return null;
  const mins = Math.round(seconds / 60);
  if (mins < 60) return `${mins} min`;
  const hrs = Math.floor(mins / 60);
  const remMins = mins % 60;
  return remMins > 0 ? `${hrs}h ${remMins}m` : `${hrs}h`;
}

function formatDate(dateString) {
  if (!dateString) return "";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return date.toLocaleDateString("en-GB", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  } catch {
    return dateString;
  }
}

function getDateGroup(dateString) {
  if (!dateString) return "Older";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "Older";

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const target = new Date(date.getFullYear(), date.getMonth(), date.getDate());

    const diffDays = Math.round((today - target) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return "Today";
    if (diffDays === 1) return "Yesterday";
    if (diffDays <= 7) return "Earlier this week";
    return "Older";
  } catch {
    return "Older";
  }
}

function SkeletonLoader() {
  return (
    <div className="skeleton-grid" aria-label="Loading lectures">
      {[1, 2, 3].map((n) => (
        <div className="skeleton-card" key={n}>
          <div className="skeleton-line skeleton-subject" />
          <div className="skeleton-line skeleton-title" />
          <div className="skeleton-line skeleton-meta" />
          <div className="skeleton-line skeleton-status" />
        </div>
      ))}
    </div>
  );
}

function EmptyState() {
  return (
    <div className="library-empty-state" tabIndex={0}>
      <div className="empty-icon-box">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
      </div>
      <h2>No lectures yet</h2>
      <p>Your recorded classes will appear here automatically after processing.</p>
    </div>
  );
}

function ErrorState({ onRetry }) {
  return (
    <div className="library-error-state" tabIndex={0}>
      <div className="error-icon-box">!</div>
      <h2>Unable to load your lectures</h2>
      <p>There was a problem connecting to the server. Please check your connection and try again.</p>
      <button className="retry-button" onClick={onRetry} type="button">
        Try again
      </button>
    </div>
  );
}

export function MyLectures({
  lectures = [],
  subjects = [],
  loading = false,
  error = null,
  onSelectLecture,
  onDeleteLecture,
  onRetry,
}) {
  const [search, setSearch] = useState("");
  const [subjectFilter, setSubjectFilter] = useState("ALL");
  const [activeMenuId, setActiveMenuId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const filteredLectures = useMemo(() => {
    return lectures.filter((lec) => {
      const titleMatch = (lec.title || "").toLowerCase().includes(search.trim().toLowerCase());
      const subjectMatch = (lec.subject || "").toLowerCase().includes(search.trim().toLowerCase());
      const matchesSearch = titleMatch || subjectMatch;

      const matchesSubject =
        subjectFilter === "ALL" ||
        (lec.subject && lec.subject.toLowerCase() === subjectFilter.toLowerCase());

      return matchesSearch && matchesSubject;
    });
  }, [lectures, search, subjectFilter]);

  const groupedLectures = useMemo(() => {
    const groups = { Today: [], Yesterday: [], "Earlier this week": [], Older: [] };
    filteredLectures.forEach((lec) => {
      const groupKey = getDateGroup(lec.lecture_date || lec.created_at);
      if (!groups[groupKey]) groups[groupKey] = [];
      groups[groupKey].push(lec);
    });
    return groups;
  }, [filteredLectures]);

  async function confirmDelete() {
    if (!deleteTarget || !onDeleteLecture) return;
    try {
      setDeleting(true);
      await onDeleteLecture(deleteTarget.id);
      setDeleteTarget(null);
    } catch {
      // Error handling passed up
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="library-shell">
      <header className="library-header">
        <div>
          <span className="eyebrow">Student library</span>
          <h1>My Lectures</h1>
          <p className="subtitle">Your recorded classes</p>
        </div>

        {!loading && !error && lectures.length > 0 && (
          <div className="library-controls">
            <label className="search-input-wrapper">
              <span className="search-icon" aria-hidden="true">⌕</span>
              <input
                type="text"
                className="library-search-input"
                placeholder="Search lectures..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Search lectures"
              />
              {search && (
                <button
                  className="search-clear-btn"
                  onClick={() => setSearch("")}
                  type="button"
                  aria-label="Clear search"
                >
                  ×
                </button>
              )}
            </label>

            <select
              className="subject-filter-select"
              value={subjectFilter}
              onChange={(e) => setSubjectFilter(e.target.value)}
              aria-label="Filter by subject"
            >
              <option value="ALL">All Subjects</option>
              {subjects.map((sub) => (
                <option key={sub.id || sub.name} value={sub.name}>
                  {sub.name}
                </option>
              ))}
            </select>
          </div>
        )}
      </header>

      <main className="library-content">
        {loading ? (
          <SkeletonLoader />
        ) : error ? (
          <ErrorState onRetry={onRetry} />
        ) : lectures.length === 0 ? (
          <EmptyState />
        ) : filteredLectures.length === 0 ? (
          <div className="no-search-results">
            <p>No lectures matching &quot;{search}&quot;</p>
            <button
              className="clear-filter-btn"
              onClick={() => {
                setSearch("");
                setSubjectFilter("ALL");
              }}
              type="button"
            >
              Reset filters
            </button>
          </div>
        ) : (
          Object.entries(groupedLectures).map(([groupTitle, groupItems]) => {
            if (!groupItems.length) return null;
            return (
              <section className="date-group" key={groupTitle}>
                <h2 className="date-group-heading">{groupTitle}</h2>
                <div className="lecture-grid">
                  {groupItems.map((lecture) => {
                    const statusInfo = STATUS_MAP[lecture.status] || STATUS_MAP.RECEIVED;
                    const durationText = formatDuration(lecture.duration_seconds);
                    const formattedDate = formatDate(lecture.lecture_date || lecture.created_at);

                    return (
                      <article
                        key={lecture.id}
                        className={`lecture-card ${statusInfo.ready ? "is-ready" : "is-processing"}`}
                        onClick={() => statusInfo.ready && onSelectLecture(lecture.id)}
                        tabIndex={statusInfo.ready ? 0 : -1}
                        onKeyDown={(e) => {
                          if (statusInfo.ready && (e.key === "Enter" || e.key === " ")) {
                            e.preventDefault();
                            onSelectLecture(lecture.id);
                          }
                        }}
                      >
                        <div className="card-topline">
                          <span className="subject-tag">{lecture.subject || "General"}</span>
                          {onDeleteLecture && (
                            <div className="card-menu-wrapper" onClick={(e) => e.stopPropagation()}>
                              <button
                                className="menu-trigger-btn"
                                onClick={() => setActiveMenuId(activeMenuId === lecture.id ? null : lecture.id)}
                                type="button"
                                aria-label="Lecture options"
                              >
                                •••
                              </button>
                              {activeMenuId === lecture.id && (
                                <div className="card-dropdown-menu">
                                  <button
                                    className="dropdown-item item-delete"
                                    onClick={() => {
                                      setActiveMenuId(null);
                                      setDeleteTarget(lecture);
                                    }}
                                    type="button"
                                  >
                                    Delete lecture
                                  </button>
                                </div>
                              )}
                            </div>
                          )}
                        </div>

                        <h3 className="lecture-card-title">{lecture.title}</h3>

                        <div className="lecture-card-meta">
                          {formattedDate && <span>{formattedDate}</span>}
                          {formattedDate && durationText && <span className="meta-dot">•</span>}
                          {durationText && <span>{durationText}</span>}
                        </div>

                        <div className="card-bottom">
                          <span className={`status-badge ${statusInfo.style}`}>
                            <span className="badge-dot" />
                            {statusInfo.label}
                          </span>
                          {statusInfo.ready && <span className="action-arrow" aria-hidden="true">→</span>}
                        </div>
                      </article>
                    );
                  })}
                </div>
              </section>
            );
          })
        )}
      </main>

      {/* Delete Confirmation Modal */}
      {deleteTarget && (
        <div className="modal-backdrop" onClick={() => !deleting && setDeleteTarget(null)}>
          <div className="modal-container" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-labelledby="delete-dialog-title">
            <h3 id="delete-dialog-title">Delete lecture?</h3>
            <p>
              This will remove <strong>&quot;{deleteTarget.title}&quot;</strong>, including its transcript and generated study material.
            </p>
            <div className="modal-actions">
              <button
                className="modal-btn btn-cancel"
                onClick={() => setDeleteTarget(null)}
                disabled={deleting}
                type="button"
              >
                Cancel
              </button>
              <button
                className="modal-btn btn-delete"
                onClick={confirmDelete}
                disabled={deleting}
                type="button"
              >
                {deleting ? "Deleting..." : "Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
