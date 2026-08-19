import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import {
  appPaths,
  devServerProxyPrefixes,
  localBackendOrigin,
} from './src/shared/config/paths'

const proxy = Object.fromEntries(
  devServerProxyPrefixes.map((path) => [
    path,
    {
      target: localBackendOrigin,
      changeOrigin: true,
      ws: path === appPaths.websocket,
    },
  ]),
)

// https://vite.dev/config/
export default defineConfig({
  define: {
    global: 'globalThis',
  },
  plugins: [react(), tailwindcss()],
  server: {
    proxy,
  },
})
