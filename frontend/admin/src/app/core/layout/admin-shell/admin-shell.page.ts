import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { ROLE_BADGE_CLASSES, ROLE_LABEL } from '@core/models/auth';
import { ToastStackComponent } from '@shared/components/toast-stack/toast-stack.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
  habilitada: boolean;
  exact?: boolean;
}

const CHAVE_SIDEBAR_PINADA = 'pethub:admin:sidebar:pinned';

@Component({
  selector: 'app-admin-shell-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ToastStackComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './admin-shell.page.html',
  styleUrls: ['./admin-shell.page.scss'],
})
export class AdminShellPage {
  private readonly auth = inject(AdminAuthService);

  readonly user = this.auth.currentUser;
  readonly rolePrincipal = this.auth.rolePrincipal;
  readonly sidebarPinada = signal<boolean>(this.lerEstadoInicial());
  readonly drawerAberto = signal(false);

  readonly badgeClasses = computed(() => {
    const role = this.rolePrincipal();
    return role ? ROLE_BADGE_CLASSES[role] : '';
  });

  readonly rotuloRole = computed(() => {
    const role = this.rolePrincipal();
    return role ? ROLE_LABEL[role] : '';
  });

  readonly itensMenu: ItemMenu[] = [
    { rota: '/dashboard',     rotulo: 'Dashboard',     icone: '📊', habilitada: true, exact: true },
    { rota: '/produtos',      rotulo: 'Catálogo',      icone: '📦', habilitada: false },
    { rota: '/pedidos',       rotulo: 'Pedidos',       icone: '📋', habilitada: false },
    { rota: '/comercial',     rotulo: 'Comercial',     icone: '🏷️', habilitada: false },
    { rota: '/clientes',      rotulo: 'Clientes',      icone: '👥', habilitada: false },
    { rota: '/relatorios',    rotulo: 'Relatórios',    icone: '📈', habilitada: false },
    { rota: '/configuracoes', rotulo: 'Configurações', icone: '⚙️', habilitada: false },
  ];

  alternarPin(): void {
    const novoEstado = !this.sidebarPinada();
    this.sidebarPinada.set(novoEstado);
    localStorage.setItem(CHAVE_SIDEBAR_PINADA, novoEstado ? '1' : '0');
  }

  abrirDrawer(): void { this.drawerAberto.set(true); }
  fecharDrawer(): void { this.drawerAberto.set(false); }

  sair(): void {
    this.auth.logout().subscribe();
  }

  private lerEstadoInicial(): boolean {
    return localStorage.getItem(CHAVE_SIDEBAR_PINADA) === '1';
  }
}
