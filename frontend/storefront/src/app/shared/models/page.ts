/**
 * Espelha o PageableResponse<T> do backend (com/dto/PageableResponse).
 */
export interface PageableResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
