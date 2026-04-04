import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { getContractById, createContract, updateContract } from '../../api/contracts';
import { toast } from 'react-hot-toast';
import Layout from '../../components/Layout';

export default function ContractForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const isEditing = Boolean(id);

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { estado: 'Activo' }
  });

  useEffect(() => {
    if (isEditing) {
      setLoading(true);
      getContractById(id)
        .then(data => reset(data))
        .catch(err => toast.error('Error al cargar contrato'))
        .finally(() => setLoading(false));
    }
  }, [id, reset]);

  const onSubmit = async (data) => {
    setLoading(true);
    try {
      if (isEditing) {
        await updateContract(id, data);
        toast.success('Contrato actualizado');
      } else {
        await createContract(data);
        toast.success('Contrato creado exitosamente');
      }
      navigate('/contracts');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error al guardar contrato');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-5xl mx-auto bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden mt-6">
        
        {/* Header */}
        <div className="bg-gray-800 text-white px-6 py-4">
          <h1 className="text-xl font-semibold">Formulario de Contrato</h1>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-8">
          
          {/* Section 1: Información General */}
          <div>
            <h2 className="text-sm font-semibold text-gray-700 uppercase mb-4 border-b pb-2">
              Información General
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              <div className="col-span-1 md:col-span-1">
                <label className="block text-sm text-gray-600 mb-1">Código Contrato</label>
                <input 
                  type="text" 
                  {...register('codigo', { required: true })}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
              <div className="col-span-1 md:col-span-1 lg:col-span-2">
                <label className="block text-sm text-gray-600 mb-1">Descripción</label>
                <input 
                  type="text" 
                  {...register('descripcion', { required: true })}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">Estado</label>
                <select 
                  {...register('estado')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors bg-white"
                >
                  <option value="Activo">Activo</option>
                  <option value="Inactivo">Inactivo</option>
                  <option value="Cancelado">Cancelado</option>
                </select>
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">Responsable</label>
                <input 
                  type="text" 
                  {...register('responsable')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">Contratista</label>
                <input 
                  type="text" 
                  {...register('contratista')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Información Económica */}
          <div>
            <h2 className="text-sm font-semibold text-gray-700 uppercase mb-4 border-b pb-2">
              Información Económica
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Valor Contrato</label>
                <input 
                  type="number" 
                  step="0.01"
                  {...register('valor')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Orden de Compra</label>
                <input 
                  type="text" 
                  {...register('ordenCompra')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Categoría</label>
                <input 
                  type="text" 
                  {...register('categoria')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
              </div>
            </div>
          </div>

          {/* Section 3: Fechas del Contrato */}
          <div>
            <h2 className="text-sm font-semibold text-gray-700 uppercase mb-4 border-b pb-2">
              Fechas del Contrato
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
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
          </div>

          {/* Section 4: Descripción Contractual */}
          <div>
            <h2 className="text-sm font-semibold text-gray-700 uppercase mb-4 border-b pb-2">
              Descripción Contractual
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Objeto</label>
                <textarea 
                  rows="4"
                  {...register('objeto')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors resize-y"
                ></textarea>
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Alcance</label>
                <textarea 
                  rows="4"
                  {...register('alcance')}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors resize-y"
                ></textarea>
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex justify-end pt-4 space-x-3 mt-6">
            <button 
              type="button" 
              onClick={() => navigate('/contracts')}
              className="px-5 py-2 min-w-32 border border-gray-300 text-gray-600 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200 transition-colors"
            >
              Cancelar
            </button>
            <button 
              type="submit" 
              disabled={loading}
              className="px-5 py-2 min-w-32 bg-emerald-700 text-white rounded-md hover:bg-emerald-800 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors font-medium"
            >
              {loading ? 'Guardando...' : 'Guardar Contrato'}
            </button>
          </div>

        </form>
      </div>
    </Layout>
  );
}
