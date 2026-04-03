import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import { getUsers } from '../api/users'
import { getRoles } from '../api/roles'
import Layout from '../components/Layout'

export default function DashboardHome() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    lockedUsers: 0,
    totalRoles: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [usersRes, rolesRes] = await Promise.all([
          getUsers({ page: 0, size: 100 }),
          getRoles()
        ])
        
        const users = usersRes.data.content
        setStats({
          totalUsers: usersRes.data.totalElements,
          activeUsers: users.filter(u => u.isActive && !u.locked).length,
          lockedUsers: users.filter(u => u.locked).length,
          totalRoles: rolesRes.data.length,
        })
      } catch (error) {
        console.error('Error cargando estadísticas:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchStats()
  }, [])

  const StatCard = ({ title, value, icon, color, subtitle, onClick }) => (
    <div 
      className={`bg-white rounded-xl shadow-sm p-6 border-l-4 ${color} hover:shadow-md transition-shadow cursor-pointer`}
      onClick={onClick}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-600">{title}</p>
          <p className="text-3xl font-bold text-gray-900 mt-2">{value}</p>
          {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
        </div>
        <div className="text-4xl opacity-80">{icon}</div>
      </div>
    </div>
  )

  const QuickAction = ({ title, description, icon, onClick, color }) => (
    <button
      onClick={onClick}
      className={`bg-white rounded-lg shadow-sm p-4 hover:shadow-md transition-all border-l-4 ${color} text-left w-full`}
    >
      <div className="flex items-start gap-3">
        <div className="text-2xl">{icon}</div>
        <div className="flex-1">
          <h3 className="font-semibold text-gray-900 text-sm">{title}</h3>
          <p className="text-xs text-gray-600 mt-1">{description}</p>
        </div>
        <span className="text-gray-400">→</span>
      </div>
    </button>
  )

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
            <p className="text-gray-600 mt-4">Cargando dashboard...</p>
          </div>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="p-6 max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 mt-1">Bienvenido, {user?.email}</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <StatCard
            title="Total Usuarios"
            value={stats.totalUsers}
            icon="👥"
            color="border-blue-500"
            subtitle="Usuarios registrados"
            onClick={() => navigate('/users')}
          />
          <StatCard
            title="Usuarios Activos"
            value={stats.activeUsers}
            icon="✓"
            color="border-green-500"
            subtitle="Usuarios habilitados"
            onClick={() => navigate('/users')}
          />
          <StatCard
            title="Usuarios Bloqueados"
            value={stats.lockedUsers}
            icon="🔒"
            color="border-red-500"
            subtitle="Requieren atención"
            onClick={() => navigate('/users')}
          />
          <StatCard
            title="Roles Configurados"
            value={stats.totalRoles}
            icon="🎭"
            color="border-purple-500"
            subtitle="Perfiles de acceso"
            onClick={() => navigate('/roles')}
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Acciones Rápidas</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <QuickAction
                title="Gestionar Usuarios"
                description="Ver, crear y editar usuarios del sistema"
                icon="👤"
                color="border-blue-500"
                onClick={() => navigate('/users')}
              />
              <QuickAction
                title="Configurar Roles"
                description="Administrar perfiles y permisos"
                icon="🎭"
                color="border-purple-500"
                onClick={() => navigate('/roles')}
              />
              <QuickAction
                title="Políticas de Seguridad"
                description="Configurar políticas de contraseñas"
                icon="🔐"
                color="border-green-500"
                onClick={() => navigate('/policy')}
              />
              <QuickAction
                title="Logs de Seguridad"
                description="Revisar actividad y auditoría"
                icon="📊"
                color="border-orange-500"
                onClick={() => navigate('/security-log')}
              />
            </div>
          </div>

          <div className="bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl shadow-sm p-6 text-white">
            <h3 className="font-semibold mb-4 text-lg">Información del Sistema</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between items-center">
                <span className="opacity-90">Versión:</span>
                <span className="font-medium">1.0.0</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="opacity-90">Última sesión:</span>
                <span className="font-medium text-xs">{user?.previousLoginAt || 'N/A'}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="opacity-90">IP actual:</span>
                <span className="font-medium">{user?.currentLoginIp || 'N/A'}</span>
              </div>
            </div>
          </div>
        </div>

        {user?.passwordExpiresInDays != null && user.passwordExpiresInDays < 7 && (
          <div className="mt-6 bg-amber-50 border-l-4 border-amber-500 p-4 rounded-lg">
            <div className="flex items-start gap-3">
              <span className="text-2xl">⚠️</span>
              <div className="flex-1">
                <h3 className="font-semibold text-amber-800">Contraseña próxima a expirar</h3>
                <p className="text-sm text-amber-700 mt-1">
                  Tu contraseña expira en {user.passwordExpiresInDays} día{user.passwordExpiresInDays !== 1 ? 's' : ''}. 
                  <button 
                    onClick={() => navigate('/change-password')}
                    className="ml-2 underline font-medium hover:text-amber-900"
                  >
                    Cambiarla ahora
                  </button>
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}
