import { Routes } from '@angular/router';

import { MainLayoutComponent } from '@core/layout/main-layout/main-layout.component';
import { authGuard } from '@core/guards/auth.guard';
import { checkoutStepGuard } from '@modules/checkout/guards/checkout-step.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@modules/home/pages/home/home.page').then((moduleHome) => moduleHome.HomePage),
      },
      {
        path: 'login',
        loadComponent: () =>
          import('@modules/auth/pages/login/login.page').then((moduleLogin) => moduleLogin.LoginPage),
      },
      {
        path: 'cadastro',
        loadComponent: () =>
          import('@modules/auth/pages/register/register.page').then((moduleRegister) => moduleRegister.RegisterPage),
      },
      {
        path: 'produtos',
        loadComponent: () =>
          import('@modules/catalog/pages/product-list/product-list.page').then((moduleProductList) => moduleProductList.ProductListPage),
      },
      {
        path: 'produtos/:sku',
        loadComponent: () =>
          import('@modules/catalog/pages/product-detail/product-detail.page').then((moduleProductDetail) => moduleProductDetail.ProductDetailPage),
      },
      {
        path: 'carrinho',
        loadComponent: () =>
          import('@modules/cart/pages/cart/cart.page').then((moduleCart) => moduleCart.CartPage),
      },
      {
        path: 'checkout/endereco',
        canActivate: [authGuard, checkoutStepGuard(['cart-not-empty'])],
        loadComponent: () =>
          import('@modules/checkout/pages/address/address.page')
            .then((moduleAddress) => moduleAddress.CheckoutAddressPage),
      },
      {
        path: 'checkout/frete',
        canActivate: [authGuard, checkoutStepGuard(['enderecoEntregaId', 'enderecoCobrancaId'])],
        loadComponent: () =>
          import('@modules/checkout/pages/shipping/shipping.page')
            .then((moduleShipping) => moduleShipping.CheckoutShippingPage),
      },
      {
        path: 'checkout/pagamento',
        canActivate: [authGuard, checkoutStepGuard(['opcaoFreteCodigo'])],
        loadComponent: () =>
          import('@modules/checkout/pages/payment/payment.page')
            .then((modulePayment) => modulePayment.CheckoutPaymentPage),
      },
      {
        path: 'checkout/revisao',
        canActivate: [authGuard, checkoutStepGuard(['formaPagamentoId'])],
        loadComponent: () =>
          import('@modules/checkout/pages/review/review.page')
            .then((moduleReview) => moduleReview.CheckoutReviewPage),
      },
      {
        path: 'checkout/sucesso/:numero',
        canActivate: [authGuard],
        loadComponent: () =>
          import('@modules/checkout/pages/success/success.page')
            .then((moduleSuccess) => moduleSuccess.CheckoutSuccessPage),
      },
      {
        path: 'minha-conta',
        canActivate: [authGuard],
        loadComponent: () =>
          import('@modules/home/pages/home/home.page').then((moduleHome) => moduleHome.HomePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
