export default function SearchBar({ value, onChange, placeholder = "Buscar...", className = "" }) {
  return (
    <input 
      className={`flex-1 max-w-sm border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 ${className}`}
      placeholder={placeholder}
      value={value} 
      onChange={onChange}
    />
  )
}
