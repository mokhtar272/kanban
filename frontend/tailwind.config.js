export default {
  content: ['./index.html','./src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50:  '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe',
          300: '#93c5fd', 400: '#60a5fa', 500: '#3b82f6',
          600: '#2563eb', 700: '#1d4ed8', 800: '#1e40af', 900: '#1e3a5f'
        }
      },
      animation: {
        'fade-in':    'fadeIn .15s ease-out',
        'slide-up':   'slideUp .2s ease-out',
        'slide-down': 'slideDown .15s ease-out',
      },
      keyframes: {
        fadeIn:    { from: { opacity: 0 },                     to: { opacity: 1 } },
        slideUp:   { from: { opacity: 0, transform: 'translateY(8px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
        slideDown: { from: { opacity: 0, transform: 'translateY(-8px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
      }
    }
  },
  plugins: []
}
