export default function StatusBadge({ status, label, variant = 'default' }) {
  const variants = {
    success: 'bg-green-100 text-green-700',
    warning: 'bg-yellow-100 text-yellow-700',
    error: 'bg-red-100 text-red-600',
    info: 'bg-blue-100 text-blue-700',
    purple: 'bg-purple-100 text-purple-700',
    gray: 'bg-gray-100 text-gray-600',
    default: 'bg-slate-100 text-slate-700'
  }

  return (
    <span className={`text-xs px-2 py-0.5 rounded-full w-fit ${variants[variant] || variants.default}`}>
      {label || status}
    </span>
  )
}
