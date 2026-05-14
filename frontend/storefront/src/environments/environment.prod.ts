export const environment = {
  production: true,
  apiBase: 'https://api.pethub.com/api/v1',
  credentialedPaths: ['/auth/', '/me/', '/customers/', '/checkout/', '/cart', '/orders'] as ReadonlyArray<string>,
};
