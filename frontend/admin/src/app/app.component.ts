import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent implements OnInit {
  private readonly auth = inject(AdminAuthService);

  ngOnInit(): void {
    this.auth.tryRefreshOnBoot().subscribe();
  }
}
