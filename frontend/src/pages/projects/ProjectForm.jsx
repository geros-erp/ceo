import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { getProjectById, createProject, updateProject } from '../../api/projects';
import { toast } from 'react-hot-toast';
import Layout from '../../components/Layout';

export default function ProjectForm() {
  const { contractId, id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const isEditing = Boolean(id);

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { estado: 'Activo' }
  });

  useEffect(() => {
    if (isEditing) {
      setLoading(true);
      getProjectById(id)
        .then(data => reset(data))
        .catch(err => toast.error('Error al cargar proyecto'))
        .finally(() => setLoading(false));
    }
  }, [id, reset]);

  const onSubmit = async (data) => {
    setLoading(true);
    try {
      const payload = { ...data, contractId };
      if (isEditing) {
        await updateProject(id, payload);
        toast.success('Proyecto actualizado');
      } else {
        await createProject(payload);
        toast.success('Proyecto creado exitosamente');
      }
      navigate(`/contracts/${contractId}/projects`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al guardar proyecto');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-5xl mx-auto bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden mt-6">
        
        {/* Header */}
        <div className="bg-gray-800 text-white px-6 py-4">
          <h1 className="text-xl font-semibold">
            {isEditing ? 'Editar Proyecto' : 'Nuevo Proyecto'}
          </h1>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-6">
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Código</label>
              <input 
                type="text" 
                {...register('codigo', { required: true })}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Nombre del Proyecto</label>
              <input 
                type="text" 
                {...register('nombre', { required: true })}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm text-gray-600 mb-1">Zona</label>
              <input 
                type="text" 
                {...register('zona')}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Fecha Inicio</label>
                <input 
                  type="date" 
                  {...register('fechaInicio')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Fecha Fin</label>
                <input 
                  type="date" 
                  {...register('fechaFin')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm text-gray-600 mb-1">Estado</label>
              <select 
                {...register('estado')}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                <option value="Activo">Activo</option>
                <option value="Inactivo">Inactivo</option>
                <option value="Finalizado">Finalizado</option>
              </select>
            </div>
          </div>

          <div className="pt-2">
            <label className="block text-sm text-gray-600 mb-1">Observaciones</label>
            <textarea 
              rows="4"
              {...register('observaciones')}
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors resize-y"
            ></textarea>
          </div>

          {/* Footer Actions */}
          <div className="flex justify-end pt-4 space-x-3 mt-2">
            <button 
              type="button" 
              onClick={() => navigate(`/contracts/${contractId}/projects`)}
              className="px-5 py-2 min-w-32 border border-gray-300 text-gray-600 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200 transition-colors"
            >
              Cancelar
            </button>
            <button 
              type="submit" 
              disabled={loading}
              className="px-5 py-2 min-w-32 bg-blue-700 text-white rounded-md hover:bg-blue-800 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors font-medium"
            >
              {loading ? 'Guardando...' : 'Guardar Proyecto'}
            </button>
          </div>

        </form>
      </div>
    </Layout>
  );
}
