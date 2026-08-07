import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AppLayout } from './layout/AppLayout'
import { ClientGuard } from './routes/ClientGuard'
import { ContactsRoute } from './routes/ContactsRoute'
import { MessagesRoute } from './routes/MessagesRoute'
import { RootRedirect } from './routes/RootRedirect'
import { SettingsRoute } from './routes/SettingsRoute'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route element={<AppLayout />}>
          <Route path="/settings" element={<SettingsRoute />} />
          <Route path="/client/:tenantId" element={<ClientGuard />}>
            <Route index element={<MessagesRoute />} />
            <Route path="contacts" element={<ContactsRoute />} />
            <Route path=":roomId" element={<MessagesRoute />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
