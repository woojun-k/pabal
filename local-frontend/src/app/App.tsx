import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AppLayout } from './layout/AppLayout'
import { ContactsRoute } from './routes/ContactsRoute'
import { MessagesRoute } from './routes/MessagesRoute'
import { RootRedirect } from './routes/RootRedirect'
import { SessionGuard } from './routes/SessionGuard'
import { SettingsRoute } from './routes/SettingsRoute'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route element={<AppLayout />}>
          <Route path="/settings" element={<SettingsRoute />} />
          <Route element={<SessionGuard />}>
            <Route path="/rooms" element={<MessagesRoute />} />
            <Route path="/rooms/:roomId" element={<MessagesRoute />} />
            <Route path="/contacts" element={<ContactsRoute />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
