# 🎨 Pet Hub — Brand Guidelines

> Identidade visual e tom de voz do Pet Hub. Aplicar consistentemente em storefront, admin, materiais de marketing e comunicação.

## Paleta de cores

| Token | Hex | Tailwind | Uso |
|---|---|---|---|
| **Primary** | `#6366F1` | `indigo-500` | Cor principal — botões CTA, links ativos, destaques tech |
| **Primary dark** | `#4F46E5` | `indigo-600` | Hover, estados pressionados, ênfase em headings |
| **Accent** | `#F59E0B` | `amber-500` | Energia, promoções, "Compre agora", flash sales |
| **Success** | `#10B981` | `emerald-500` | Confirmações, pedido aprovado, estoque OK |
| **Danger** | `#EF4444` | `red-500` | Erros, pedido cancelado, estoque crítico |
| **Warning** | `#F59E0B` | `amber-500` | Atenção (compartilha com Accent quando faz sentido) |
| **Neutral** | escala slate | `slate-50` → `slate-900` | Texto, fundos, bordas, divisores |
| **Background (light)** | `#FFFFFF` | `white` | Fundo padrão do storefront |
| **Background (dark)** | `#0F172A` | `slate-900` | Fundo dark mode (admin tem dark mode toggle) |

### Combinações recomendadas

- **CTA primário:** fundo `indigo-500`, texto `white`, hover `indigo-600`.
- **CTA promocional (flash sale):** fundo `amber-500`, texto `slate-900`, hover `amber-600`.
- **Body text:** `slate-700` sobre `white` (light) / `slate-200` sobre `slate-900` (dark).
- **Headings:** `slate-900` (light) / `white` (dark).
- **Bordas e divisores:** `slate-200` (light) / `slate-800` (dark).

### Acessibilidade

Todos os pares texto/fundo definidos acima atendem WCAG AA (contraste ≥ 4.5:1 para texto normal, ≥ 3:1 para texto grande). Não usar `indigo-500` como texto em fundo `white` para parágrafos longos — só para links e elementos curtos.

## Tipografia

| Família | Uso | Pesos |
|---|---|---|
| **Inter** | UI, texto corrido, formulários, tabelas | 400 (regular), 500 (medium), 600 (semibold) |
| **Poppins** | Headings de marketing, hero, títulos de seção | 500 (medium), 600 (semibold), 700 (bold) |

Fonte: Google Fonts. Carregar via `<link>` ou self-hosted (preferível em produção para performance).

### Escala tipográfica

Tailwind defaults com pequenos ajustes:

| Token | Tamanho | Uso |
|---|---|---|
| `text-xs` | 12px | Legendas, metadata, status badges |
| `text-sm` | 14px | Body em UI densa (admin), labels |
| `text-base` | 16px | Body padrão do storefront |
| `text-lg` | 18px | Lead paragraphs |
| `text-xl` → `text-2xl` | 20–24px | Subtítulos |
| `text-3xl` → `text-5xl` | 30–48px | Headings de seção, hero (Poppins) |

### Exemplo de aplicação no logo

```
Pet Hub
^^^ ^^^
│   │
│   └─ "Hub" em Poppins 700, indigo-500
└──── "Pet" em Poppins 500, slate-900
```

Quando texto-only (até ter logo desenhado), seguir esse pattern.

## Logo

**Conceito:** Hexágono ou círculo "hub" com pata estilizada no centro. O hexágono comunica "tecnologia / componente", a pata comunica "pet". Juntos: "hub tech para pets".

**Variações esperadas:**
- Versão colorida (indigo + amber) sobre fundo branco
- Versão monocromática (slate-900) para impressão B&W
- Versão clara (white) para fundo escuro
- Versão somente-ícone (favicon, app icon)

Até existir o desenho, usar versão tipográfica: `**Pet** Hub` com `Pet` em peso 500 slate-900 e `Hub` em peso 700 indigo-500.

## Iconografia

**Biblioteca:** [Lucide Icons](https://lucide.dev/) (open source, MIT, ~1500 ícones, estilo coerente).

**Padrões:**
- Tamanho default: 20px (`size={20}` ou `w-5 h-5` no Tailwind)
- Stroke width: 2 (padrão Lucide)
- Cor: herda do texto (`currentColor`) por default; usar `indigo-500` apenas em ícones de destaque
- Espaçamento ao lado de texto: `gap-2` (8px)

## Tom de voz

**Atributos:** próximo, técnico-confiável, descontraído mas não infantil, claro.

**Pessoa:** trate o cliente por "você" (PT-BR informal). Evite vocativos infantis ("amiguinho", "tutor pet"). O cliente é um adulto que quer comprar boa tecnologia para o pet dele.

**Termos preferidos:**
- ✅ "pet", "cachorro", "gato" (específico melhor que genérico)
- ✅ "produto tech", "smart collar", "GPS tracker"
- ❌ "petzinho", "doguinho", "miaú"
- ❌ "produto incrível!!!", "imperdível!!!" (sem hipérbole vazia, sem múltiplos pontos de exclamação)

**Em mensagens de erro:**
- Diga o que aconteceu, por quê, e como resolver.
- ❌ "Erro: operação falhou."
- ✅ "Não conseguimos calcular o frete agora. Tente outro CEP ou aguarde alguns segundos."

**Em confirmações:**
- Confirme o que foi feito, em uma linha. Sem celebração excessiva.
- ❌ "🎉🎉🎉 Parabéns!!! Seu pedido foi feito com sucesso!!! 🎉🎉🎉"
- ✅ "Pedido confirmado. Você vai receber atualizações por email."

## Aplicação no código

Toda a paleta acima fica em **tokens** — não usar hex direto em componentes. Configurar em:

- **TailwindCSS** (`tailwind.config.js`): estender `colors` com tokens semânticos (`primary`, `accent`, `success`, `danger`).
- **SCSS** (se aplicável): `src/styles/_tokens.scss` com `:root { --color-primary: #6366F1; ... }`.

Exemplo Tailwind:

```js
// frontend/{storefront,admin}/tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#6366F1', dark: '#4F46E5' },
        accent:  '#F59E0B',
        success: '#10B981',
        danger:  '#EF4444',
      },
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        display: ['Poppins', 'system-ui', 'sans-serif'],
      },
    },
  },
};
```

Assim, trocar a paleta no futuro é uma mudança em um lugar.
