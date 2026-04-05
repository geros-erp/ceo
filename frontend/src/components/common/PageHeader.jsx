import { useNavigate } from 'react-router-dom'

export default function PageHeader({ title, subtitle, backTo = '/dashboard', actionButton }) {
  const navigate = useNavigate()

  return (
    <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
      <button 
        onClick={() => navigate(backTo)} 
        className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md"
      >
        ← Volver
      </button>
      <span className="text-white font-semibold">{title}</span>
      {actionButton && <div className="ml-auto">{actionButton}</div>}
    </header>
  )
}
