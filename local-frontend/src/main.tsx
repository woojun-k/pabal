import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './App.css'
import App from './app/App.tsx'
import { env } from './shared/config/env.ts'

const enableMockBackend = async () => {
  if (env.backendMode !== 'mock') {
    return
  }

  const { worker } = await import('./mocks/browser.ts')
  await worker.start({
    onUnhandledRequest: 'bypass',
  })
}

const render = () => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

void enableMockBackend().then(render)
