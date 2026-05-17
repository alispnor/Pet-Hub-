export type Role = 'ROLE_CLIENTE' | 'ROLE_ADMIN_LOJA' | 'ROLE_GERENTE' | 'ROLE_OPERADOR';

export interface AdminUser {
  id: number;
  nome: string;
  email: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface AuthResponse {
  accessToken: string;
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
