import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);

  readonly usuarioLogado = this.authService.isAuthenticated;
  readonly nomeUsuario = computed(() => this.authService.currentUser()?.nome ?? '');

  sairDaConta(): void {
    this.authService.logout().subscribe();
  }
}
