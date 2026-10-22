export const sectionDefinitions = [
  { id: "overview", label: "Overview", icon: "01" },
  { id: "key_points", label: "Key Points", icon: "02" },
  { id: "important_concepts", label: "Important Concepts", icon: "03" },
  { id: "definitions", label: "Definitions", icon: "04" },
  { id: "examples", label: "Examples", icon: "05" },
  { id: "formulas", label: "Formulas", icon: "06" },
  { id: "exam_questions", label: "Exam Questions", icon: "07" },
  { id: "revision_points", label: "Revision Points", icon: "08" },
  { id: "assignments", label: "Assignments", icon: "09" },
  { id: "announcements", label: "Announcements", icon: "10" },
];

export function formatTimestamp(seconds) {
  const totalSeconds = Math.max(0, Math.floor(seconds));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const remainder = totalSeconds % 60;
  return [hours, minutes, remainder]
    .map((value, index) => (index === 0 ? String(value).padStart(2, "0") : String(value).padStart(2, "0")))
    .join(":");
}

export function findSegment(segments, segmentId) {
  return segments.find((segment) => segment.id === segmentId);
}
