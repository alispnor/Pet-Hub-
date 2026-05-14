export type Role =
  | 'ROLE_CLIENTE'
  | 'ROLE_OPERADOR'
  | 'ROLE_GERENTE'
  | 'ROLE_ADMIN_LOJA';

export type TipoUsuario = 'CLIENTE' | 'ADMIN';

export interface AuthenticatedUser {
  id: number;
  nome: string;
  email: string;
  tipo: TipoUsuario;
  roles: Role[];
  ativo: boolean;
}

/**
 * Resposta do /auth/login e /auth/refresh — refreshToken vem sempre null
 * porque está no cookie HttpOnly. Aqui só consumimos accessToken e usuário.
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string | null;
  expiresIn: number;
  usuario: AuthenticatedUser;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterClienteRequest {
  nome: string;
  email: string;
  senha: string;
  telefone?: string | null;
}
