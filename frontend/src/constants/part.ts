import type { Part } from "../api/types";

export const PART_OPTIONS: { value: Part; label: string }[] = [
  { value: "PM", label: "PM (기획)" },
  { value: "DE", label: "DE (디자인)" },
  { value: "BE", label: "BE (백엔드)" },
  { value: "FE", label: "FE (프론트엔드)" },
];
