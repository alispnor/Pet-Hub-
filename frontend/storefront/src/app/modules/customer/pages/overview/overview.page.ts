import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-overview-page',
  standalone: true,
  imports: [CommonModule],
  template: `<section class="px-4 py-6"><h1 class="text-2xl font-display">Visão geral</h1><p class="mt-2 text-graphite-600">Em construção (Task 11).</p></section>`,
})
export class OverviewPage {}
