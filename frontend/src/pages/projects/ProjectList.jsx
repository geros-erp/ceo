import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getProjectsByContract, deleteProject } from '../../api/projects';
import { getContractById } from '../../api/contracts';
import { toast } from 'react-hot-toast';
import { FiPlus, FiEdit2, FiTrash2, FiSearch, FiArrowLeft } from 'react-icons/fi';
import Layout from '../../components/Layout';

export default function ProjectList() {
  const { contractId } = useParams();
  const navigate = useNavigate();
  const [projects, setProjects] = useState([]);
  const [contractDetails, setContractDetails] = useState('');
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const data = await getProjectsByContract(contractId, page, 10, search);
      setProjects(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      toast.error('Error al cargar proyectos');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getContractById(contractId).then(data => {
      setContractDetails(`${data.codigo} - ${data.descripcion}`);
    }).catch(() => {});
  }, [contractId]);

  useEffect(() => {
    fetchProjects();
  }, [contractId, page, search]);

  const handleDelete = async (id) => {
    if (window.confirm('¿Está seguro de eliminar este proyecto?')) {
      try {
        await deleteProject(id);
        toast.success('Proyecto eliminado');
        fetchProjects();
      } catch (err) {
        toast.error('Error al eliminar proyecto');
      }
    }
  };

  return (
    <Layout>
      <div className="max-w-7xl mx-auto bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden mt-6">
        
        {/* Header */}
        <div className="bg-gray-800 text-white px-6 py-4 flex justify-between items-center">
          <div>
            <h1 className="text-xl font-semibold">Proyectos del Contrato</h1>
            <p className="text-sm text-gray-300 mt-1">{contractDetails}</p>
          </div>
          <div className="flex space-x-3">
            <button 
              onClick={() => navigate('/contracts')}
              className="flex items-center space-x-2 bg-gray-700 hover:bg-gray-600 border border-gray-600 text-white px-4 py-2 rounded transition-colors text-sm font-medium"
            >
              <FiArrowLeft />
              <span>Volver a Contratos</span>
            </button>
            <button 
              onClick={() => navigate(`/contracts/${contractId}/projects/new`)}
              className="flex items-center space-x-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded transition-colors text-sm font-medium"
            >
              <FiPlus />
              <span>Nuevo Proyecto</span>
            </button>
          </div>
        </div>

        {/* Toolbar */}
        <div className="p-4 border-b flex justify-between items-center bg-gray-50/50">
          <div className="relative w-72">
            <input 
              type="text" 
              placeholder="Buscar p. ej. código, contrato..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors text-sm"
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
                <th className="px-6 py-3">NOMBRE</th>
                <th className="px-6 py-3">CONTRATO</th>
                <th className="px-6 py-3">ZONA</th>
                <th className="px-6 py-3">ESTADO</th>
                <th className="px-6 py-3 text-right">ACCIONES</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan="6" className="px-6 py-10 text-center text-gray-500">
                    Cargando proyectos...
                  </td>
                </tr>
              ) : projects.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-10 text-center text-gray-500">
                    No se encontraron proyectos.
                  </td>
                </tr>
              ) : (
                projects.map(project => (
                  <tr key={project.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4 font-medium text-gray-900">{project.codigo}</td>
                    <td className="px-6 py-4">{project.nombre}</td>
                    <td className="px-6 py-4 text-indigo-600 font-medium">{project.contractCodigo}</td>
                    <td className="px-6 py-4">{project.zona || 'N/A'}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded-full text-xs font-semibold ${project.estado === 'Activo' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                        {project.estado}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                      <button 
                        onClick={() => navigate(`/contracts/${contractId}/projects/${project.id}`)}
                        className="p-1.5 text-blue-600 hover:bg-blue-50 rounded transition-colors"
                        title="Editar"
                      >
                        <FiEdit2 size={16} />
                      </button>
                      <button 
                        onClick={() => handleDelete(project.id)}
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

        {/* Pagination */}
        {!loading && projects.length > 0 && (
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
