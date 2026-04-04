import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  
  server: {
    proxy: {
      '/api': {
        target: 'https://127.0.0.1:8443',
        secure: false, // acepta certificado autofirmado en desarrollo
        changeOrigin: true,
      },
    },
  },

  build: {
    // Minificación básica
    minify: true,
    
    // NO generar source maps en producción
    sourcemap: mode !== 'production',

    // Dividir código en chunks
    rollupOptions: {
      output: {
        // Ofuscar nombres de archivos en producción
        entryFileNames: mode === 'production' 
          ? 'assets/[name].[hash].js'
          : 'assets/[name].js',
        chunkFileNames: mode === 'production'
          ? 'assets/[name].[hash].js'
          : 'assets/[name].js',
        assetFileNames: mode === 'production'
          ? 'assets/[name].[hash].[ext]'
          : 'assets/[name].[ext]',
      },
    },

    // Optimizaciones adicionales
    cssCodeSplit: true,
    assetsInlineLimit: 4096,
    chunkSizeWarningLimit: 500,
  },

  // Optimización de dependencias
  optimizeDeps: {
    include: ['react', 'react-dom', 'react-router-dom', 'axios'],
  },
}))
