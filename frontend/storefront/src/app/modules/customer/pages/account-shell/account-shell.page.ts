import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '@core/services/auth.service';

interface ItemMenu {
  rota: string;
  rotuloMenu: string;
  iconeEmoji: string;
  exact?: boolean;
}

@Component({
  selector: 'app-account-shell-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './account-shell.page.html',
  styleUrls: ['./account-shell.page.scss'],
})
export class AccountShellPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly drawerAberto = signal(false);
  readonly nomeUsuario = this.authService.currentUser;

  readonly itensMenu: ItemMenu[] = [
    { rota: '/minha-conta',           rotuloMenu: 'Visão geral', iconeEmoji: '📋', exact: true },
    { rota: '/minha-conta/perfil',    rotuloMenu: 'Perfil',      iconeEmoji: '👤' },
    { rota: '/minha-conta/enderecos', rotuloMenu: 'Endereços',   iconeEmoji: '📍' },
    { rota: '/minha-conta/cartoes',   rotuloMenu: 'Cartões',     iconeEmoji: '💳' },
    { rota: '/minha-conta/pets',      rotuloMenu: 'Pets',        iconeEmoji: '🐾' },
    { rota: '/minha-conta/pedidos',   rotuloMenu: 'Pedidos',     iconeEmoji: '📦' },
  ];

  abrirDrawer(): void { this.drawerAberto.set(true); }
  fecharDrawer(): void { this.drawerAberto.set(false); }

  sairDaConta(): void {
    this.authService.logout().subscribe({
      complete: () => this.router.navigateByUrl('/'),
    });
  }
}
