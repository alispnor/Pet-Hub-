/**
 * Tailwind config alinhado ao ai-memory/design-system.md.
 * Cores, fontes e spacing seguem os tokens v1 — não adicionar valores soltos
 * sem registrar na seção correspondente do design-system.
 */
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        coral: {
          50:  '#FFF4F1',
          100: '#FFE4DC',
          200: '#FFC9B8',
          300: '#FFA68B',
          400: '#FF845E',
          500: '#FF6B47',
          600: '#E5512E',
          700: '#BF3F22',
          800: '#8F2E18',
          900: '#5C1D0F',
        },
        graphite: {
          0:   '#FFFFFF',
          50:  '#FAFAFA',
          100: '#F4F4F5',
          200: '#E4E4E7',
          300: '#D4D4D8',
          400: '#A1A1AA',
          500: '#71717A',
          600: '#52525B',
          700: '#3F3F46',
          800: '#27272A',
          900: '#18181B',
          950: '#0A0A0B',
        },
        success: { 500: '#10B981' },
        warning: { 500: '#F59E0B' },
        danger:  { 500: '#EF4444' },
        info:    { 500: '#3B82F6' },
      },
      fontFamily: {
        display: ['"Inter Display"', '"SF Pro Display"', '-apple-system', 'sans-serif'],
        sans:    ['"Inter"', '"SF Pro Text"', '-apple-system', 'sans-serif'],
        mono:    ['"JetBrains Mono"', '"SF Mono"', 'Menlo', 'monospace'],
      },
      fontSize: {
        xs:   ['11px', { lineHeight: '16px' }],
        sm:   ['13px', { lineHeight: '20px' }],
        base: ['15px', { lineHeight: '24px' }],
        lg:   ['17px', { lineHeight: '26px' }],
        xl:   ['20px', { lineHeight: '28px' }],
        '2xl':['24px', { lineHeight: '32px', letterSpacing: '-0.02em' }],
        '3xl':['30px', { lineHeight: '38px', letterSpacing: '-0.02em' }],
        '4xl':['36px', { lineHeight: '44px', letterSpacing: '-0.02em' }],
        '5xl':['48px', { lineHeight: '56px', letterSpacing: '-0.02em' }],
      },
      borderRadius: {
        DEFAULT: '8px',
        sm: '6px',
        lg: '12px',
        xl: '16px',
        '2xl': '24px',
      },
      boxShadow: {
        soft:    '0 1px 2px rgba(24,24,27,0.04), 0 2px 8px rgba(24,24,27,0.04)',
        elev:    '0 4px 16px rgba(24,24,27,0.08)',
        focus:   '0 0 0 4px rgba(255,107,71,0.18)',
      },
      maxWidth: {
        page: '1200px',
        reading: '64ch',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ],
};
