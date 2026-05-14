# Pet Hub — Design System

> **Last updated:** 2026-05-13
> **Owner:** Ali (al  Salahi)
> **Status:** v1.0 — Foundation
> **Scope:** Pet Hub web (Angular 17) + future Pet Diary alignment

---

## 1. Brand & Positioning

**One-liner:** Pet Hub is the premium tech ecosystem for pet care — IoT devices, GPS tracking, smart feeders, and an integrated marketplace, designed with the calm precision of Apple and the warmth of a trusted vet.

**Voice:**
- Confident, never cute
- Technical when it matters (specs, data), human when it counts (health, emotion)
- Brazilian Portuguese primary; English secondary
- Never uses pet-baby talk ("peludinho", "auau") in product UI — reserved for marketing only

**Audience:** Tech-savvy pet owners, 28–55, urban, premium spend bracket. They own iPhones, value good design, and treat pets as family.

---

## 2. Design Principles

1. **Calm by default, expressive when needed** — UI is mostly neutral; coral is a scalpel, not a paintbrush.
2. **Data clarity beats decoration** — In health/IoT contexts, legibility wins every time.
3. **Trustworthy in health, delightful in lifestyle** — Vaccination records look clinical; activity feed feels warm.
4. **One screen, one job** — No dashboards trying to do everything. Compose multiple focused views instead.
5. **Premium = restraint** — No gradients on text, no drop shadows on everything, no emoji as functional icons.

---

## 3. Color System

### Philosophy
Apple-like neutrals carry the UI. Coral (`#FF6B47`) is the brand accent, used for: primary CTAs, brand moments, critical highlights. Never for body text, never for large surfaces in light mode.

### Primary — Coral
```
coral-50   #FFF4F1
coral-100  #FFE4DC
coral-200  #FFC9B8
coral-300  #FFA68B
coral-400  #FF845E
coral-500  #FF6B47   ← brand primary
coral-600  #E5512E
coral-700  #BF3F22
coral-800  #8F2E18
coral-900  #5C1D0F
```

### Neutrals — Graphite scale (NEVER pure black)
```
graphite-0    #FFFFFF
graphite-50   #FAFAFA   ← light mode bg
graphite-100  #F4F4F5
graphite-200  #E4E4E7
graphite-300  #D4D4D8
graphite-400  #A1A1AA
graphite-500  #71717A
graphite-600  #52525B
graphite-700  #3F3F46
graphite-800  #27272A   ← dark mode surface
graphite-900  #18181B   ← dark mode bg
graphite-950  #0A0A0B   ← deepest dark, not #000
```

### Semantic
```
success-500   #10B981   (vaccinations up-to-date, device online)
warning-500   #F59E0B   (low battery, vaccine due soon)
danger-500    #EF4444   (device offline, missed medication)
info-500      #3B82F6   (informational, neutral data)
```

### Theme tokens (light/dark)

**Light mode:**
```css
--bg-base:        var(--graphite-50);
--bg-surface:     var(--graphite-0);
--bg-elevated:    var(--graphite-0);
--text-primary:   var(--graphite-900);
--text-secondary: var(--graphite-600);
--text-tertiary:  var(--graphite-400);
--border-subtle:  var(--graphite-200);
--border-strong:  var(--graphite-300);
--accent:         var(--coral-500);
--accent-hover:   var(--coral-600);
```

**Dark mode:**
```css
--bg-base:        var(--graphite-950);
--bg-surface:     var(--graphite-900);
--bg-elevated:    var(--graphite-800);
--text-primary:   var(--graphite-50);
--text-secondary: var(--graphite-300);
--text-tertiary:  var(--graphite-500);
--border-subtle:  var(--graphite-800);
--border-strong:  var(--graphite-700);
--accent:         var(--coral-400);   /* lifted for dark contrast */
--accent-hover:   var(--coral-300);
```

---

## 4. Typography

### Font stack
```
--font-display: 'Inter Display', 'SF Pro Display', -apple-system, sans-serif;
--font-body:    'Inter', 'SF Pro Text', -apple-system, sans-serif;
--font-mono:    'JetBrains Mono', 'SF Mono', Menlo, monospace;
```

> **Why Inter:** open-source, supports BR Portuguese diacritics, variable font, optical sizing. Inter Display for >24px headings.

### Scale (1.250 — Major Third)
```
text-xs    11px / 16px   — captions, timestamps
text-sm    13px / 20px   — secondary body, table cells
text-base  15px / 24px   — body default
text-lg    17px / 26px   — emphasized body
text-xl    20px / 28px   — section titles
text-2xl   24px / 32px   — card titles
text-3xl   30px / 38px   — page titles
text-4xl   36px / 44px   — hero, marketing
text-5xl   48px / 56px   — landing hero only
```

### Weights
- 400 regular — body
- 500 medium — emphasized body, buttons
- 600 semibold — headings, navigation active
- 700 bold — rare, marketing only

### Letter spacing
- Headings ≥24px: `-0.02em` (tighten)
- All caps labels: `+0.08em`
- Body: default (0)

---

## 5. Spacing

**Base unit: 4px.** All spacing is a multiple.

```
space-0    0
space-1    4px
space-2    8px
space-3    12px
space-4    16px   ← most common
space-5    20px
space-6    24px
space-8    32px
space-10   40px
space-12   48px
space-16   64px
space-20   80px
space-24   96px
```

**Component-level conventions:**
- Card padding: `space-6` (24px)
- Form field gap: `space-4` (16px)
- Section gap: `space-12` (48px)
- Page gutter: `space-6` mobile, `space-10` desktop

---

## 6. Border Radius

```
radius-none   0
radius-sm     4px    — inputs, small chips
radius-md     8px    — buttons, cards (default)
radius-lg     12px   — modals, large cards
radius-xl     16px   — feature cards, hero sections
radius-2xl    24px   — marketing surfaces
radius-full   9999px — avatars, pills, FAB
```

**Pet Hub rule:** never mix radii in the same component. Card uses `radius-lg`? Buttons inside also `radius-md` or `radius-lg`, never `radius-2xl`.

---

## 7. Shadows (Light Mode)

```
shadow-xs:  0 1px 2px rgba(0,0,0,0.04)
shadow-sm:  0 2px 4px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)
shadow-md:  0 4px 12px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.04)
shadow-lg:  0 12px 32px rgba(0,0,0,0.12), 0 4px 8px rgba(0,0,0,0.06)
shadow-xl:  0 24px 48px rgba(0,0,0,0.16)
```

**Dark mode:** use `rgba(0,0,0,0.4)` and reduce y-offset by 30%. Or replace shadow with subtle border (`var(--border-subtle)`).

**Pet Hub rule:** elevation tells a story. Card resting = `shadow-sm`. Card hover = `shadow-md`. Modal = `shadow-xl`. No floating elements without elevation justification.

---

## 8. Component Specs

### Button
```
Variants:    primary | secondary | ghost | danger
Sizes:       sm (32px) | md (40px) | lg (48px)
Radius:      radius-md
Font weight: 500
States:      default, hover, active, focus, disabled, loading
```

- **Primary:** `bg: coral-500`, `text: white`, hover `bg: coral-600`
- **Secondary:** `bg: transparent`, `border: 1px graphite-300`, `text: graphite-900`
- **Ghost:** no bg, no border, `text: graphite-700`, hover `bg: graphite-100`
- **Danger:** `bg: danger-500`, used for destructive actions only

Focus ring: `2px solid coral-500 with 2px offset` (light) / `coral-400` (dark).

### Input
```
Height:      40px (default), 48px (large)
Padding:     0 16px
Radius:      radius-md
Border:      1px var(--border-strong)
Bg:          var(--bg-surface)
Focus:       border var(--accent), ring 3px coral-100 (light) / coral-900 (dark)
Error:       border var(--danger-500), helper text danger-500
```

### Card
```
Bg:          var(--bg-surface)
Border:      1px var(--border-subtle) [dark mode] OR none [light]
Shadow:      shadow-sm [light only]
Radius:      radius-lg
Padding:     space-6
```

### Badge / Tag
```
Height:      24px
Padding:     0 8px
Radius:      radius-full
Font:        text-xs, weight 500
Variants:    neutral, success, warning, danger, info, brand
```

### Table
- Header: `bg: graphite-50` (light) / `graphite-800` (dark), text `text-xs uppercase`, weight 500, color `graphite-600`
- Row height: 56px (comfortable), 44px (compact)
- Divider: 1px `var(--border-subtle)` bottom only
- Hover row: `bg: graphite-50` (light) / `graphite-800` (dark)
- Selected row: `bg: coral-50` (light) / `coral-900/20` (dark)

---

## 9. Layout

**Breakpoints:**
```
sm  640px
md  768px
lg  1024px
xl  1280px
2xl 1536px
```

**Container max-width:** `1280px` for app, `1440px` for marketing.

**Grid:** 12 columns, gutter `space-6` (24px), margin `space-6` mobile / `space-10` desktop.

**App shell:** sidebar 240px (collapsed 64px), top bar 64px, content area fluid.

---

## 10. Iconography

**Library:** [Lucide Icons](https://lucide.dev) — clean, consistent, open-source, fits Apple-like aesthetic.

```
icon-sm   16px   — inline with text-sm
icon-md   20px   — default, inline with text-base
icon-lg   24px   — buttons, prominent UI
icon-xl   32px   — empty states, features
```

**Stroke width:** `1.5px` (default) or `2px` (small sizes <16px).

**Pet Hub specifics:**
- Device status: filled circle (online: success-500, offline: graphite-400, low-battery: warning-500)
- Pet species: use Lucide `Dog`, `Cat`, `Bird`, `Fish`. Never emoji.
- GPS/location: Lucide `MapPin` only.

---

## 11. Motion

**Durations:**
```
duration-fast    150ms   — hover, focus, small state changes
duration-base    250ms   — most transitions
duration-slow    400ms   — page transitions, modals
duration-slower  600ms   — celebratory moments (rare)
```

**Easings:**
```
ease-out:     cubic-bezier(0.16, 1, 0.3, 1)   ← default for entries
ease-in-out:  cubic-bezier(0.65, 0, 0.35, 1)  ← bidirectional
ease-spring:  cubic-bezier(0.34, 1.56, 0.64, 1) ← playful, rare
```

**Rules:**
- Respect `prefers-reduced-motion`
- Never animate `width/height` (use `transform: scale`)
- Modals fade + scale (0.96 → 1) over 250ms
- Toasts slide-in from top-right, fade-out

---

## 12. Accessibility

- **Contrast minimum:** 4.5:1 for body text, 3:1 for large text, 3:1 for UI components.
- **Coral-500 on white:** ratio 3.4 — **OK for large text and UI, NOT for body text**. Body uses graphite-900.
- **Focus ring:** always visible, 2px solid `var(--accent)` with 2px offset.
- **Touch targets:** minimum 44×44px (iOS HIG standard).
- **Form labels:** always present, never placeholder-as-label.
- **Color is never the only signal:** device offline = gray icon + "Offline" text label.

---

## 13. Pet Hub–Specific Patterns

### Device Status Card
Shows IoT device (collar, feeder, tracker). Required elements:
- Device icon (24px, Lucide)
- Device name (text-base, weight 500)
- Pet name it's attached to (text-sm, graphite-600)
- Status pill (Online / Offline / Syncing / Low Battery)
- Battery level (icon + %)
- Last sync timestamp (text-xs)
- Quick action menu (`...`)

### GPS Map Component
- Map style: light minimalist (Mapbox/Leaflet with custom style)
- Pet location pin: coral-500 with white border, 32px
- Geofence overlay: coral-500 at 20% opacity
- Never use default red Google Maps pins.

### Product Card (E-commerce)
- Image aspect ratio: 1:1, `radius-lg`
- Brand (text-xs, graphite-500, uppercase)
- Product name (text-base, weight 500, max 2 lines)
- Rating (5 stars, coral-400 filled, graphite-300 empty) + count
- Price (text-lg, weight 600). Original price strikethrough if discount.
- "Add to cart" button: secondary by default; primary on hover.

### Health Record Row
- Date (text-sm, weight 500, graphite-900)
- Type icon (vaccine / vet visit / medication)
- Title (text-base, weight 500)
- Vet/provider (text-sm, graphite-600)
- Status badge (Completed / Scheduled / Overdue)

---

## 14. Anti-Patterns (NEVER DO)

- ❌ Emoji as functional icons (🐶 in nav, 🦴 as button)
- ❌ Coral on coral (e.g., coral button on coral banner)
- ❌ Pure black (`#000`) — use `graphite-950`
- ❌ Pure white text on coral-500 < 16px (contrast fails)
- ❌ More than 2 fonts loaded
- ❌ Gradients on text or icons
- ❌ Drop shadow on every element ("shadow soup")
- ❌ Mixed border radii in the same component
- ❌ Skeumorphic textures (paw prints background, leather, wood)
- ❌ Comic Sans–adjacent fonts (Quicksand, Comfortaa, Fredoka)
- ❌ "Cute" microcopy in health/safety contexts

---

## 15. Implementation Notes (Angular 17)

**File structure:**
```
src/
├── styles/
│   ├── tokens/
│   │   ├── _colors.scss
│   │   ├── _typography.scss
│   │   ├── _spacing.scss
│   │   ├── _radius.scss
│   │   ├── _shadows.scss
│   │   └── _motion.scss
│   ├── themes/
│   │   ├── _light.scss
│   │   └── _dark.scss
│   └── styles.scss      ← imports everything
└── app/
    └── shared/
        └── ui/          ← standalone components matching this spec
```

**Theme toggle:** use `class="dark"` on `<html>`, switch via Angular service + `localStorage`. Respect `prefers-color-scheme` on first visit.

**Tailwind (if used alongside):** configure `tailwind.config.js` to consume these tokens as `theme.extend.colors`, etc. — single source of truth.

---

## 16. Open Decisions (TODO)

- [ ] Confirm Inter or test SF Pro Web alternative
- [ ] Decide on Mapbox vs Leaflet for GPS component
- [ ] Define empty-state illustration style (custom SVG vs stock)
- [ ] Logo lockup and minimum size rules (separate brand doc)
- [ ] Email template design system (separate doc)