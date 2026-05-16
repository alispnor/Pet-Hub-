export type Bandeira = 'VISA' | 'MASTER' | 'AMEX' | 'ELO' | 'HIPERCARD' | 'OUTRO';
export type TipoPagamento = 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'BOLETO';

export interface FormaPagamentoResponse {
  id: number;
  tipo: TipoPagamento;
  apelido: string | null;
  bandeira: Bandeira | null;
  ultimosQuatroDigitos: string | null;
  nomeImpresso: string | null;
  validadeMes: number | null;
  validadeAno: number | null;
  padrao: boolean;
  ativo: boolean;
}

export interface CreateFormaPagamentoRequest {
  tipo: TipoPagamento;
  apelido?: string;
  gatewayToken?: string;
  bandeira?: Bandeira;
  ultimosQuatroDigitos?: string;
  nomeImpresso?: string;
  validadeMes?: number;
  validadeAno?: number;
  padrao?: boolean;
}

export interface TokenizeCardRequest {
  numero: string;                  // 13–19 dígitos, sem máscara
  cvv: string;                     // 3–4 dígitos
  nomeImpresso: string;
  validadeMes: number;             // 1–12
  validadeAno: number;             // 2024–2100
}

export interface TokenizeCardResponse {
  token: string;
  bandeira: Bandeira;
  ultimosQuatroDigitos: string;
}
