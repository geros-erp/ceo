import api from './auth';

export const getProjectsByContract = async (contractId, page = 0, size = 10, search = '') => {
  const response = await api.get(`/projects/contract/${contractId}`, {
    params: { page, size, search }
  });
  return response.data;
};

export const getProjectById = async (id) => {
  const response = await api.get(`/projects/${id}`);
  return response.data;
};

export const createProject = async (data) => {
  const response = await api.post('/projects', data);
  return response.data;
};

export const updateProject = async (id, data) => {
  const response = await api.put(`/projects/${id}`, data);
  return response.data;
};

export const deleteProject = async (id) => {
  const response = await api.delete(`/projects/${id}`);
  return response.data;
};
