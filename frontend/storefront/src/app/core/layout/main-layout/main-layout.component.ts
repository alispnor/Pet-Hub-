import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

import { AuthService } from '@core/services/auth.service';
import { CartService } from '@modules/cart/services/cart.service';
import { ToastStackComponent } from '@shared/components/toast-stack/toast-stack.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastStackComponent, ConfirmDialogComponent],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss'],
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);

  readonly usuarioLogado = this.authService.isAuthenticated;
  readonly nomeUsuario = computed(() => this.authService.currentUser()?.nome ?? '');
  readonly quantidadeItensCarrinho = this.cartService.totalItens;

  sairDaConta(): void {
    this.authService.logout().subscribe();
  }
}
