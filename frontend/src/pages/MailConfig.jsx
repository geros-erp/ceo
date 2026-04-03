import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMailConfig, updateMailConfig } from '../api/mailconfig'
import Layout from '../components/Layout'

const EMPTY = { enabled: false, host: '', port: 587, username: '', password: '', useTls: true, useSsl: false, fromAddress: '', fromName: '' }

function normalizeMailConfig(data = {}) {
  return {
    ...EMPTY,
    ...data,
    host: data.host ?? '',
    username: data.username ?? '',
    password: '',
    fromAddress: data.fromAddress ?? '',
    fromName: data.fromName ?? '',
    port: data.port ?? EMPTY.port,
  }
}

function Toggle({ checked, onChange, disabled }) {
  return (
    <label className="relative inline-flex items-center cursor-pointer">
      <input type="checkbox" checked={checked} onChange={onChange} disabled={disabled} className="sr-only peer" />
      <div className="w-11 h-6 bg-gray-300 peer-checked:bg-indigo-600 rounded-full transition-colors peer-disabled:opacity-50" />
      <div className="absolute left-0.5 top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform peer-checked:translate-x-5" />
    </label>
  )
}

function Row({ label, children }) {
  return (
    <div className="flex items-center justify-between bg-white rounded-lg px-4 py-3 shadow-sm">
      <span className="text-sm font-medium text-gray-800">{label}</span>
      {children}
    </div>
  )
}

export default function MailConfig() {
  const [form, setForm]       = useState(EMPTY)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving]   = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError]     = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getMailConfig()
      .then(({ data }) => setForm(normalizeMailConfig(data)))
      .catch(() => setError('Error al cargar'))
      .finally(() => setLoading(false))
  }, [])

  const set = (name, value) => setForm(f => ({ ...f, [name]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError(''); setSuccess(false)
    try { await updateMailConfig({ ...form, port: parseInt(form.port) }); setSuccess(true); setTimeout(() => setSuccess(false), 3000) }
    catch (err) { setError(err.response?.data?.message || 'Error al guardar') }
    finally { setSaving(false) }
  }

  const inputCls = (disabled) => `border border-gray-300 rounded-md px-3 py-1.5 text-sm focus:outline-none focus:border-indigo-500 w-64 ${disabled ? 'bg-gray-50 text-gray-400' : ''}`

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
        <button onClick={() => navigate('/dashboard')} className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md">← Volver</button>
        <span className="text-white font-semibold">Correo Electrónico (SMTP)</span>
      </header>
      <Layout>
        <div className="p-6 max-w-2xl mx-auto w-full">
          {loading ? (
            <div className="flex items-center justify-center min-h-[320px] text-gray-500">Cargando...</div>
          ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className={`flex items-center justify-between bg-white rounded-lg p-4 shadow-sm border-l-4 ${form.enabled ? 'border-indigo-500' : 'border-gray-300'}`}>
              <div><p className="font-medium text-gray-800">Envío de correos activo</p><p className="text-xs text-gray-500">Habilitar envío automático de credenciales al crear usuarios</p></div>
              <Toggle checked={form.enabled} onChange={e => set('enabled', e.target.checked)} />
            </div>

            <div className={`space-y-2 ${!form.enabled ? 'opacity-50 pointer-events-none' : ''}`}>
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Servidor SMTP</h3>
              <Row label="Host"><input value={form.host} onChange={e => set('host', e.target.value)} placeholder="smtp.gmail.com" className={inputCls(!form.enabled)} /></Row>
              <Row label="Puerto"><input type="number" value={form.port} onChange={e => set('port', e.target.value)} min={1} max={65535} className="w-24 border border-gray-300 rounded-md px-2 py-1.5 text-sm text-center focus:outline-none focus:border-indigo-500" /></Row>
              <Row label="Usar TLS (STARTTLS)"><Toggle checked={form.useTls} onChange={e => set('useTls', e.target.checked)} disabled={!form.enabled} /></Row>
              <Row label="Usar SSL"><Toggle checked={form.useSsl} onChange={e => set('useSsl', e.target.checked)} disabled={!form.enabled} /></Row>

              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider pt-2">Autenticación</h3>
              <Row label="Usuario"><input value={form.username} onChange={e => set('username', e.target.value)} placeholder="correo@empresa.com" className={inputCls(!form.enabled)} /></Row>
              <Row label="Contraseña"><input type="password" value={form.password} onChange={e => set('password', e.target.value)} placeholder="Dejar vacío para no cambiar" className={inputCls(!form.enabled)} /></Row>

              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider pt-2">Remitente</h3>
              <Row label="Correo remitente"><input value={form.fromAddress} onChange={e => set('fromAddress', e.target.value)} placeholder="noreply@empresa.com" className={inputCls(!form.enabled)} /></Row>
              <Row label="Nombre remitente"><input value={form.fromName} onChange={e => set('fromName', e.target.value)} placeholder="Sistema Geros" className={inputCls(!form.enabled)} /></Row>
            </div>

            {error   && <p className="text-red-600 text-sm">{error}</p>}
            {success && <p className="text-green-600 text-sm">✓ Configuración guardada correctamente</p>}
            <div className="flex justify-end">
              <button type="submit" disabled={saving} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-6 py-2 rounded-md text-sm font-medium transition-colors">
                {saving ? 'Guardando...' : 'Guardar configuración'}
              </button>
            </div>
          </form>
          )}
        </div>
      </Layout>
    </div>
  )
}
