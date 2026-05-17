export class ForbiddenLoginError extends Error {
  constructor() {
    super('Forbidden login: user does not have admin role');
    this.name = 'ForbiddenLoginError';
  }
}
