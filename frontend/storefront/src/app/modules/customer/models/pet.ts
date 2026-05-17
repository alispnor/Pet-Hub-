export type Especie = 'CACHORRO' | 'GATO' | 'AVE' | 'PEIXE' | 'REPTIL' | 'OUTROS';
export type Porte = 'PEQUENO' | 'MEDIO' | 'GRANDE' | 'GIGANTE';

export interface PetResponse {
  id: number;
  nome: string;
  especie: Especie;
  raca: string | null;
  dataNascimento: string | null;
  pesoKg: number | null;
  porte: Porte | null;
  observacoes: string | null;
  fotoUrl: string | null;
}

export interface CreatePetRequest {
  nome: string;
  especie: Especie;
  raca?: string;
  dataNascimento?: string;
  pesoKg?: number;
  porte?: Porte;
  observacoes?: string;
}

export type UpdatePetRequest = Partial<CreatePetRequest>;

export const ESPECIES_LABEL: Record<Especie, string> = {
  CACHORRO: 'Cachorro',
  GATO: 'Gato',
  AVE: 'Ave',
  PEIXE: 'Peixe',
  REPTIL: 'Réptil',
  OUTROS: 'Outros',
};

export const PORTES_LABEL: Record<Porte, string> = {
  PEQUENO: 'Pequeno',
  MEDIO: 'Médio',
  GRANDE: 'Grande',
  GIGANTE: 'Gigante',
};
