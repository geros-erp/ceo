import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getContracts, deleteContract } from '../../api/contracts';
import { toast } from 'react-hot-toast';
import { FiPlus, FiEdit2, FiTrash2, FiSearch, FiLayers } from 'react-icons/fi';
import Layout from '../../components/Layout';

export default function ContractList() {
  const navigate = useNavigate();
  const [contracts, setContracts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchContracts = async () => {
    setLoading(true);
    try {
      const data = await getContracts(page, 10, search);
      setContracts(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      toast.error('Error al cargar contratos');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchContracts();
  }, [page, search]);

  const handleDelete = async (id) => {
    if (window.confirm('¿Está seguro de eliminar este contrato?')) {
      try {
        await deleteContract(id);
        toast.success('Contrato eliminado');
        fetchContracts();
      } catch (err) {
        toast.error('Error al eliminar contrato');
      }
    }
  };

  return (
    <Layout>
      <div className="max-w-7xl mx-auto bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden mt-6">
        
        {/* Header */}
        <div className="bg-gray-800 text-white px-6 py-4 flex justify-between items-center">
          <h1 className="text-xl font-semibold">Gestión de Contratos</h1>
          <button 
            onClick={() => navigate('/contracts/new')}
            className="flex items-center space-x-2 bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded transition-colors text-sm font-medium"
          >
            <FiPlus />
            <span>Nuevo Contrato</span>
          </button>
        </div>

        {/* Toolbar */}
        <div className="p-4 border-b flex justify-between items-center bg-gray-50/50">
          <div className="relative w-72">
            <input 
              type="text" 
              placeholder="Buscar por código, desc..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors text-sm"
            />
            <FiSearch className="absolute left-3 top-2.5 text-gray-400" />
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-600">
            <thead className="bg-gray-50 border-b text-gray-700 font-semibold uppercase text-xs">
              <tr>
                <th className="px-6 py-3">CÓDIGO</th>
                <th className="px-6 py-3">DESCRIPCIÓN</th>
                <th className="px-6 py-3">CONTRATISTA</th>
                <th className="px-6 py-3">ESTADO</th>
                <th className="px-6 py-3">INICIO - FIN</th>
                <th className="px-6 py-3 text-right">ACCIONES</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan="6" className="px-6 py-10 text-center text-gray-500">
                    Cargando contratos...
                  </td>
                </tr>
              ) : contracts.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-10 text-center text-gray-500">
                    No se encontraron contratos.
                  </td>
                </tr>
              ) : (
                contracts.map(contract => (
                  <tr key={contract.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4 font-medium text-gray-900">{contract.codigo}</td>
                    <td className="px-6 py-4">{contract.descripcion}</td>
                    <td className="px-6 py-4">{contract.contratista || 'N/A'}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded-full text-xs font-semibold ${contract.estado === 'Activo' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                        {contract.estado}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-xs whitespace-nowrap">
                      {contract.fechaInicio || 'N/A'} <br/> {contract.fechaFin || 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                      <button 
                        onClick={() => navigate(`/contracts/${contract.id}/projects`)}
                        className="p-1.5 text-indigo-600 hover:bg-indigo-50 rounded transition-colors"
                        title="Ver Proyectos"
                      >
                        <FiLayers size={16} />
                      </button>
                      <button 
                        onClick={() => navigate(`/contracts/${contract.id}`)}
                        className="p-1.5 text-blue-600 hover:bg-blue-50 rounded transition-colors"
                        title="Editar"
                      >
                        <FiEdit2 size={16} />
                      </button>
                      <button 
                        onClick={() => handleDelete(contract.id)}
                        className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors"
                        title="Eliminar"
                      >
                        <FiTrash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Placeholder */}
        {!loading && contracts.length > 0 && (
          <div className="p-4 border-t flex justify-between items-center text-sm text-gray-600">
            <span>Página {page + 1} de {totalPages}</span>
            <div className="space-x-2">
              <button 
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className="px-3 py-1 border rounded hover:bg-gray-50 disabled:opacity-50"
              >
                Anterior
              </button>
              <button 
                disabled={page === totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="px-3 py-1 border rounded hover:bg-gray-50 disabled:opacity-50"
              >
                Siguiente
              </button>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
