export type Genero = 'MASCULINO' | 'FEMININO' | 'NAO_INFORMADO' | 'OUTRO';

export interface PerfilResponse {
  id: number;
  nome: string;
  email: string;
  cpfMascarado: string | null;
  dataNascimento: string | null;
  genero: Genero | null;
  telefoneAdicional: string | null;
  aceiteTermos: boolean;
  aceiteMarketing: boolean;
}

export interface UpdatePerfilRequest {
  dataNascimento?: string | null;
  genero?: Genero | null;
  telefoneAdicional?: string | null;
  aceiteTermos?: boolean;
  aceiteMarketing?: boolean;
}

export const GENEROS_LABEL: Record<Genero, string> = {
  MASCULINO: 'Masculino',
  FEMININO: 'Feminino',
  NAO_INFORMADO: 'Prefiro não informar',
  OUTRO: 'Outro',
};
