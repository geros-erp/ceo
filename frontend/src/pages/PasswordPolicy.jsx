import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPolicy, updatePolicy } from '../api/policy'
import Layout from '../components/Layout'

const LABELS = {
  enabled:                    { label: 'Política activa',                        type: 'toggle',  description: 'Activar o desactivar todas las validaciones de contraseña' },
  minLength:                  { label: 'Longitud mínima',                        type: 'number',  min: 1,  max: 64   },
  maxLength:                  { label: 'Longitud máxima',                        type: 'number',  min: 1,  max: 256  },
  requireUppercase:           { label: 'Requerir mayúsculas',                    type: 'toggle',  description: 'Al menos una letra mayúscula (A-Z)' },
  requireLowercase:           { label: 'Requerir minúsculas',                    type: 'toggle',  description: 'Al menos una letra minúscula (a-z)' },
  requireNumbers:             { label: 'Requerir números',                       type: 'toggle',  description: 'Al menos un dígito (0-9)' },
  requireSpecialChars:        { label: 'Requerir caracteres especiales',         type: 'toggle',  description: 'Al menos un carácter como !@#$%^&*' },
  maxFailedAttempts:          { label: 'Máximo de intentos fallidos',            type: 'number',  min: 1,  max: 20   },
  lockDurationMinutes:        { label: 'Duración del bloqueo (minutos)',         type: 'number',  min: 0,  max: 1440 },
  expirationDays:             { label: 'Expiración de contraseña (días)',        type: 'number',  min: 0,  max: 365  },
  notifyBeforeExpirationDays: { label: 'Avisar antes del vencimiento (días)',    type: 'number',  min: 0,  max: 90   },
  passwordHistoryCount:       { label: 'Contraseñas a recordar',                 type: 'number',  min: 0,  max: 24   },
  maxSequenceLength:          { label: 'Longitud máxima de secuencia (0=desact.)',type: 'number', min: 0,  max: 10   },
  sessionTimeoutSeconds:      { label: 'Timeout de sesión (segundos)',           type: 'number',  min: 60, max: 86400, description: 'Tiempo de inactividad antes de cerrar sesión automáticamente' },
  enableAntiPhishing:         { label: 'Sello de Seguridad (Anti-Phishing)',     type: 'toggle',  description: 'Muestra frase e imagen personalizada al usuario antes del login para verificar el sitio' },
}

const GROUPS = [
  { title: 'Longitud',               keys: ['minLength', 'maxLength'] },
  { title: 'Requisitos de caracteres', keys: ['requireUppercase', 'requireLowercase', 'requireNumbers', 'requireSpecialChars'] },
  { title: 'Seguridad de cuenta',    keys: ['maxFailedAttempts', 'lockDurationMinutes', 'expirationDays', 'notifyBeforeExpirationDays', 'passwordHistoryCount', 'maxSequenceLength'] },
  { title: 'Sesión y autenticación', keys: ['sessionTimeoutSeconds', 'enableAntiPhishing'] },
]

function Toggle({ checked, onChange, disabled }) {
  return (
    <label className="relative inline-flex items-center cursor-pointer">
      <input type="checkbox" checked={checked} onChange={onChange} disabled={disabled} className="sr-only peer" />
      <div className="w-11 h-6 bg-gray-300 peer-checked:bg-indigo-600 rounded-full transition-colors peer-disabled:opacity-50" />
      <div className="absolute left-0.5 top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform peer-checked:translate-x-5" />
    </label>
  )
}

export default function PasswordPolicy() {
  const [form, setForm]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving]   = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError]     = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getPolicy().then(({ data }) => setForm(data)).catch(() => setError('Error al cargar')).finally(() => setLoading(false))
  }, [])

  const handleChange = (key, value) => setForm(f => ({ ...f, [key]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError(''); setSuccess(false)
    try { const { data } = await updatePolicy(form); setForm(data); setSuccess(true); setTimeout(() => setSuccess(false), 3000) }
    catch (err) { setError(err.response?.data?.message || 'Error al guardar') }
    finally { setSaving(false) }
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
        <button onClick={() => navigate('/dashboard')} className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md">← Volver</button>
        <span className="text-white font-semibold">Política de Contraseñas</span>
      </header>
      <Layout>
        <div className="p-6 max-w-2xl mx-auto w-full">
          {loading ? (
            <div className="flex items-center justify-center min-h-[320px] text-gray-500">Cargando...</div>
          ) : (
          <form onSubmit={handleSubmit} className="space-y-5">

            {/* Toggle principal */}
            <div className={`flex items-center justify-between bg-white rounded-lg p-4 shadow-sm border-l-4 ${form.enabled ? 'border-indigo-500' : 'border-gray-300'}`}>
              <div>
                <p className="font-medium text-gray-800">{LABELS.enabled.label}</p>
                <p className="text-xs text-gray-500">{LABELS.enabled.description}</p>
              </div>
              <Toggle checked={form.enabled} onChange={e => handleChange('enabled', e.target.checked)} />
            </div>

            {/* Grupos */}
            {GROUPS.map(group => (
              <div key={group.title} className={`space-y-2 ${!form.enabled ? 'opacity-50 pointer-events-none' : ''}`}>
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">{group.title}</h3>
                {group.keys.map(key => {
                  const meta = LABELS[key]
                  return (
                    <div key={key} className="flex items-center justify-between bg-white rounded-lg px-4 py-3 shadow-sm">
                      <div>
                        <p className="text-sm font-medium text-gray-800">{meta.label}</p>
                        {meta.description && <p className="text-xs text-gray-500">{meta.description}</p>}
                      </div>
                      {meta.type === 'toggle'
                        ? <Toggle checked={form[key]} onChange={e => handleChange(key, e.target.checked)} disabled={!form.enabled} />
                        : <input type="number" value={form[key]} min={meta.min} max={meta.max} disabled={!form.enabled}
                            onChange={e => handleChange(key, parseInt(e.target.value))}
                            className="w-20 text-center border border-gray-300 rounded-md px-2 py-1.5 text-sm focus:outline-none focus:border-indigo-500 disabled:bg-gray-50" />
                      }
                    </div>
                  )
                })}
              </div>
            ))}

            {error   && <p className="text-red-600 text-sm">{error}</p>}
            {success && <p className="text-green-600 text-sm">✓ Configuración guardada correctamente</p>}

            <div className="flex justify-end">
              <button type="submit" disabled={saving}
                className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-6 py-2 rounded-md text-sm font-medium transition-colors">
                {saving ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </div>
          </form>
          )}
        </div>
      </Layout>
    </div>
  )
}
