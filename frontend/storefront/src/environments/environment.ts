/**
 * Environment de dev — apiBase é relativo para usar o proxy do `ng serve`
 * (proxy.conf.json) que encaminha /api → http://localhost:8080. Em prod
 * o build substitui via fileReplacements para environment.prod.ts.
 */
export const environment = {
  production: false,
  apiBase: '/api/v1',
  // Endpoints autenticados que precisam carregar o cookie HttpOnly (refresh, /me/*).
  // O AuthInterceptor decide `withCredentials` com base nessa lista.
  credentialedPaths: ['/auth/', '/me/', '/customers/', '/checkout/', '/cart', '/orders'] as ReadonlyArray<string>,
};
