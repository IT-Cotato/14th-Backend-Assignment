export interface ApiResponse<T> {
  success: boolean;
  status: number;
  data: T;
  timestamp: string;
}

export type Part = "PM" | "DE" | "BE" | "FE";

export interface Author {
  nickname: string;
  generation: number;
  part: Part;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
