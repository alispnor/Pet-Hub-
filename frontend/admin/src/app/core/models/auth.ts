export type Role = 'ROLE_CLIENTE' | 'ROLE_ADMIN_LOJA' | 'ROLE_GERENTE' | 'ROLE_OPERADOR';

export type TipoUsuario = 'CLIENTE' | 'ADMIN';

export interface AdminUser {
  id: number;
  nome: string;
  email: string;
  tipo: TipoUsuario;
  roles: Role[];
  ativo: boolean;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string | null;
  expiresIn: number;
  usuario: AdminUser;
}

export const ROLES_ADMIN: readonly Role[] = [
  'ROLE_ADMIN_LOJA',
  'ROLE_GERENTE',
  'ROLE_OPERADOR',
];

export const ROLE_LABEL: Record<Role, string> = {
  ROLE_CLIENTE: 'Cliente',
  ROLE_ADMIN_LOJA: 'Admin Loja',
  ROLE_GERENTE: 'Gerente',
  ROLE_OPERADOR: 'Operador',
};

export const ROLE_BADGE_CLASSES: Record<Role, string> = {
  ROLE_CLIENTE: 'bg-graphite-100 text-graphite-700',
  ROLE_OPERADOR: 'bg-blue-100 text-blue-800',
  ROLE_GERENTE: 'bg-amber-100 text-amber-800',
  ROLE_ADMIN_LOJA: 'bg-red-100 text-red-800',
};

/**
 * Quando o usuário tem múltiplas roles, exibe a de maior prioridade
 * (ADMIN_LOJA > GERENTE > OPERADOR).
 */
export function rolePrincipal(roles: Role[] | undefined): Role | null {
  if (!roles || roles.length === 0) return null;
  if (roles.includes('ROLE_ADMIN_LOJA')) return 'ROLE_ADMIN_LOJA';
  if (roles.includes('ROLE_GERENTE')) return 'ROLE_GERENTE';
  if (roles.includes('ROLE_OPERADOR')) return 'ROLE_OPERADOR';
  return roles[0];
}

export function temAlgumaRoleAdmin(roles: Role[]): boolean {
  return roles.some(role => ROLES_ADMIN.includes(role));
}
