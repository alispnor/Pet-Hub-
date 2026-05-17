import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { ROLE_LABEL } from '@core/models/auth';

@Component({
  selector: 'app-dashboard-home-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.page.html',
})
export class DashboardHomePage {
  private readonly auth = inject(AdminAuthService);

  readonly user = this.auth.currentUser;
  readonly rotuloRole = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_LABEL[role] : '';
  });
}
