import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdConfig, updateAdConfig } from '../../api/adconfig'
import Layout from '../../components/Layout'
import { PageHeader, LoadingState } from '../../components/common'

const EMPTY = { enabled: false, host: '', port: 389, domain: '', baseDn: '', bindUser: '', bindPassword: '', useSsl: false }

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

export default function AdConfig() {
  const [form, setForm]       = useState(EMPTY)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving]   = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError]     = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getAdConfig().then(({ data }) => setForm({ ...EMPTY, ...data })).catch(() => setError('Error al cargar')).finally(() => setLoading(false))
  }, [])

  const set = (name, value) => setForm(f => ({ ...f, [name]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError(''); setSuccess(false)
    try { await updateAdConfig({ ...form, port: parseInt(form.port) }); setSuccess(true); setTimeout(() => setSuccess(false), 3000) }
    catch (err) { setError(err.response?.data?.message || 'Error al guardar') }
    finally { setSaving(false) }
  }

  const inputCls = (disabled) => `border border-gray-300 rounded-md px-3 py-1.5 text-sm focus:outline-none focus:border-indigo-500 w-64 ${disabled ? 'bg-gray-50 text-gray-400' : ''}`

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <PageHeader title="Directorio Activo (AD)" />
      <Layout>
        <div className="p-6 max-w-2xl mx-auto w-full">
          {loading ? (
            <LoadingState />
          ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className={`flex items-center justify-between bg-white rounded-lg p-4 shadow-sm border-l-4 ${form.enabled ? 'border-indigo-500' : 'border-gray-300'}`}>
              <div><p className="font-medium text-gray-800">Autenticación AD activa</p><p className="text-xs text-gray-500">Habilitar inicio de sesión mediante Directorio Activo</p></div>
              <Toggle checked={form.enabled} onChange={e => set('enabled', e.target.checked)} />
            </div>

            <div className={`space-y-2 ${!form.enabled ? 'opacity-50 pointer-events-none' : ''}`}>
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Conexión</h3>
              <Row label="Host / IP"><input value={form.host} onChange={e => set('host', e.target.value)} placeholder="192.168.1.10" className={inputCls(!form.enabled)} /></Row>
              <Row label="Puerto"><input type="number" value={form.port} onChange={e => set('port', e.target.value)} min={1} max={65535} className="w-24 border border-gray-300 rounded-md px-2 py-1.5 text-sm text-center focus:outline-none focus:border-indigo-500" /></Row>
              <Row label="Usar SSL (LDAPS)"><Toggle checked={form.useSsl} onChange={e => set('useSsl', e.target.checked)} disabled={!form.enabled} /></Row>

              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider pt-2">Dominio</h3>
              <Row label="Dominio"><input value={form.domain} onChange={e => set('domain', e.target.value)} placeholder="empresa.local" className={inputCls(!form.enabled)} /></Row>
              <Row label="Base DN"><input value={form.baseDn} onChange={e => set('baseDn', e.target.value)} placeholder="DC=empresa,DC=local" className={inputCls(!form.enabled)} /></Row>

              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider pt-2">Credenciales de enlace</h3>
              <Row label="Bind User"><input value={form.bindUser} onChange={e => set('bindUser', e.target.value)} placeholder="CN=ldap-user,DC=empresa,DC=local" className={inputCls(!form.enabled)} /></Row>
              <Row label="Bind Password"><input type="password" value={form.bindPassword} onChange={e => set('bindPassword', e.target.value)} placeholder="Dejar vacío para no cambiar" className={inputCls(!form.enabled)} /></Row>
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
