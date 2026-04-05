import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { useBlockBrowserHistory } from './hooks/useBlockBrowserHistory'
import SessionTimeout from './components/SessionTimeout'
// Auth
import Login        from './pages/auth/Login'
import ForgotPassword  from './pages/auth/ForgotPassword'
import ResetPassword   from './pages/auth/ResetPassword'
import ChangePassword from './pages/auth/ChangePassword'
import ValidateSite    from './pages/auth/ValidateSite'

// Dashboard
import DashboardHome from './pages/dashboard/DashboardHome'

// Users
import Users        from './pages/users/Users'

// Roles
import Roles        from './pages/roles/Roles'

// Security
import PasswordPolicy from './pages/security/PasswordPolicy'
import PasswordHistory from './pages/security/PasswordHistory'
import ReservedUsernames from './pages/security/ReservedUsernames'
import SecurityLog     from './pages/security/SecurityLog'

// Config
import AdConfig     from './pages/config/AdConfig'
import MailConfig   from './pages/config/MailConfig'
import MenuConfig      from './pages/config/MenuConfig'

// Contracts & Projects
import ContractList    from './pages/contracts/ContractList'
import ContractForm    from './pages/contracts/ContractForm'
import ProjectList     from './pages/projects/ProjectList'
import ProjectForm     from './pages/projects/ProjectForm'
function Unauthorized() {
  const { defaultPath } = useAuth()

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 px-6">
      <div className="bg-white rounded-xl shadow-md p-8 max-w-md w-full text-center">
        <h1 className="text-xl font-semibold text-gray-800 mb-2">Acceso denegado</h1>
        <p className="text-sm text-gray-600 mb-5">
          Tu perfil no tiene permisos para acceder a esta funcionalidad.
        </p>
        <Link
          to={defaultPath}
          className="inline-block bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium"
        >
          Ir a una opcion autorizada
        </Link>
      </div>
    </div>
  )
}

function PrivateRoute({ children, path }) {
  const { user, authzLoading, hasPathAccess, defaultPath } = useAuth()
  
  // Bloquear navegación histórica en rutas autenticadas
  useBlockBrowserHistory()

  if (!user) return <Navigate to="/login" replace />
  if (authzLoading) return <div className="min-h-screen flex items-center justify-center bg-gray-100 text-sm text-gray-500">Validando permisos...</div>
  if (path && !hasPathAccess(path))
    return defaultPath && defaultPath !== path
      ? <Navigate to={defaultPath} replace />
      : <Navigate to="/unauthorized" replace />
  return children
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <SessionTimeout />
        <Routes>
          <Route path="/login"            element={<Login />} />
          <Route path="/forgot-password"  element={<ForgotPassword />} />
          <Route path="/reset-password"   element={<ResetPassword />} />
          <Route path="/unauthorized"    element={<PrivateRoute><Unauthorized /></PrivateRoute>} />
          <Route path="/dashboard"       element={<PrivateRoute><DashboardHome /></PrivateRoute>} />
          <Route path="/users"           element={<PrivateRoute path="/users"><Users /></PrivateRoute>} />
          <Route path="/change-password" element={<PrivateRoute><ChangePassword /></PrivateRoute>} />
          <Route path="/policy"          element={<PrivateRoute path="/policy"><PasswordPolicy /></PrivateRoute>} />
          <Route path="/validate-site"  element={<PrivateRoute><ValidateSite /></PrivateRoute>} />
          <Route path="/roles"           element={<PrivateRoute path="/roles"><Roles /></PrivateRoute>} />
          <Route path="/ad-config"       element={<PrivateRoute path="/ad-config"><AdConfig /></PrivateRoute>} />
          <Route path="/mail-config"     element={<PrivateRoute path="/mail-config"><MailConfig /></PrivateRoute>} />
          <Route path="/menu-config"     element={<PrivateRoute path="/menu-config"><MenuConfig /></PrivateRoute>} />
          <Route path="/reserved-usernames" element={<PrivateRoute path="/reserved-usernames"><ReservedUsernames /></PrivateRoute>} />
          <Route path="/password-history"  element={<PrivateRoute path="/password-history"><PasswordHistory /></PrivateRoute>} />
          <Route path="/security-log"      element={<PrivateRoute path="/security-log"><SecurityLog /></PrivateRoute>} />
          <Route path="/contracts"       element={<PrivateRoute path="/contracts"><ContractList /></PrivateRoute>} />
          <Route path="/contracts/new"   element={<PrivateRoute path="/contracts"><ContractForm /></PrivateRoute>} />
          <Route path="/contracts/:id"   element={<PrivateRoute path="/contracts"><ContractForm /></PrivateRoute>} />
          
          <Route path="/contracts/:contractId/projects"        element={<PrivateRoute path="/contracts"><ProjectList /></PrivateRoute>} />
          <Route path="/contracts/:contractId/projects/new"    element={<PrivateRoute path="/contracts"><ProjectForm /></PrivateRoute>} />
          <Route path="/contracts/:contractId/projects/:id"    element={<PrivateRoute path="/contracts"><ProjectForm /></PrivateRoute>} />
          
          <Route path="*"                element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
