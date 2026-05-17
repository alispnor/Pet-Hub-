import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Timeline, EtapaTimeline } from '@modules/orders/models/order';

@Component({
  selector: 'app-order-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-timeline.component.html',
  styleUrls: ['./order-timeline.component.scss'],
})
export class OrderTimelineComponent {
  @Input({ required: true }) timeline!: Timeline;
  @Input() compact = false;

  ehTerminalNegativo(etapa: EtapaTimeline): boolean {
    const nomeNormalizado = etapa.nome.toLowerCase();
    return nomeNormalizado.includes('cancel') || nomeNormalizado.includes('rejei');
  }

  ehTerminalPositivoComAlerta(etapa: EtapaTimeline): boolean {
    return etapa.nome.toLowerCase().includes('devolv');
  }

  classesBolinha(etapa: EtapaTimeline): string {
    if (this.ehTerminalNegativo(etapa)) {
      return 'bg-red-500 text-white border-red-500';
    }
    if (this.ehTerminalPositivoComAlerta(etapa)) {
      return 'bg-amber-500 text-white border-amber-500';
    }
    switch (etapa.status) {
      case 'CONCLUIDA':
        return 'bg-coral-600 text-white border-coral-600';
      case 'ATUAL':
        return 'bg-coral-600 text-white border-coral-600 ring-4 ring-coral-200 motion-safe:animate-pulse';
      case 'PENDENTE':
        return 'bg-white text-graphite-400 border-graphite-300 border-2';
    }
  }

  classesConector(etapa: EtapaTimeline): string {
    if (this.ehTerminalNegativo(etapa)) return 'bg-red-300';
    if (etapa.status === 'PENDENTE') return 'border-dashed border-graphite-300 border-l-2 md:border-l-0 md:border-t-2 bg-transparent';
    return 'bg-coral-600';
  }

  classesLabel(etapa: EtapaTimeline): string {
    if (this.ehTerminalNegativo(etapa)) return 'text-red-700 font-semibold';
    if (etapa.status === 'ATUAL') return 'text-graphite-900 font-semibold';
    if (etapa.status === 'PENDENTE') return 'text-graphite-500';
    return 'text-graphite-700';
  }

  ariaCurrent(etapa: EtapaTimeline): 'step' | null {
    return etapa.status === 'ATUAL' ? 'step' : null;
  }
}
