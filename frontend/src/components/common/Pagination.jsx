export default function Pagination({ page, totalPages, total, onPageChange }) {
  return (
    <div className="flex items-center justify-center gap-4 mt-4 text-sm text-gray-500">
      <button 
        disabled={page === 0} 
        onClick={() => onPageChange(page - 1)}
        className="border border-gray-300 bg-white px-3 py-1.5 rounded-md disabled:opacity-40 hover:bg-gray-50"
      >
        ‹ Anterior
      </button>
      <span>Página {page + 1} de {totalPages || 1} — {total} registros</span>
      <button 
        disabled={page + 1 >= totalPages} 
        onClick={() => onPageChange(page + 1)}
        className="border border-gray-300 bg-white px-3 py-1.5 rounded-md disabled:opacity-40 hover:bg-gray-50"
      >
        Siguiente ›
      </button>
    </div>
  )
}
