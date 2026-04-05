import { useState } from 'react'

export default function ValidateSite() {
  const [copied, setCopied] = useState(false)

  const origin = window.location.origin
  const protocol = window.location.protocol
  const hostname = window.location.hostname
  const port = window.location.port || (protocol === 'https:' ? '443' : '80')

  const expectedHost = import.meta.env.VITE_EXPECTED_HOST || ''
  const hostMatches = expectedHost ? hostname === expectedHost : null
  const isSecure = protocol === 'https:'

  const info = {
    origin,
    protocol,
    hostname,
    port,
    expectedHost: expectedHost || '(no configurado)',
    hostMatches: hostMatches === null ? '(no aplicable)' : hostMatches,
    isSecure,
  }

  const copyInfo = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(info, null, 2))
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (e) {
      setCopied(false)
    }
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-semibold mb-4">Verificar autenticidad del sitio</h1>
      <p className="mb-4 text-sm text-gray-600">Estas comprobaciones ayudan al usuario a validar elementos básicos de la autenticidad del sitio desde su navegador.</p>

      <div className="bg-white rounded-md shadow p-4 max-w-2xl">
        <dl className="grid grid-cols-1 gap-2">
          <div className="flex justify-between"><dt className="text-sm text-gray-500">Origen</dt><dd className="text-sm">{origin}</dd></div>
          <div className="flex justify-between"><dt className="text-sm text-gray-500">Protocolo</dt><dd className="text-sm">{protocol} {isSecure ? ' (Seguro)' : ' (No seguro)'}</dd></div>
          <div className="flex justify-between"><dt className="text-sm text-gray-500">Host</dt><dd className="text-sm">{hostname}</dd></div>
          <div className="flex justify-between"><dt className="text-sm text-gray-500">Puerto</dt><dd className="text-sm">{port}</dd></div>
          <div className="flex justify-between"><dt className="text-sm text-gray-500">Host esperado</dt><dd className="text-sm">{expectedHost || <span className="text-gray-400">(no configurado)</span>}</dd></div>
          <div className="flex justify-between"><dt className="text-sm text-gray-500">Coincidencia con esperado</dt><dd className="text-sm">{hostMatches === null ? <span className="text-gray-400">(no aplicable)</span> : hostMatches ? 'Sí' : 'No'}</dd></div>
        </dl>

        <div className="mt-4 flex gap-2">
          <button onClick={copyInfo} className="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-md text-sm">Copiar resultado</button>
          <a className="inline-block text-sm text-indigo-600 underline" href="#" onClick={(e)=>{e.preventDefault(); window.location.reload()}}>Refrescar</a>
          {copied && <span className="text-sm text-green-600">Copiado</span>}
        </div>
      </div>

      <div className="mt-6 text-xs text-gray-500">
        <p>Notas:</p>
        <ul className="list-disc ml-6">
          <li>No es posible desde el navegador leer el certificado TLS/SSL ni encabezados de respuesta sensibles.</li>
          <li>Para validaciones fuertes considere implementar en backend una ruta que devuelva una firma o sello del servidor que el cliente pueda verificar.</li>
        </ul>
      </div>
    </div>
  )
}
