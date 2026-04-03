import React from 'react';
import { useSessionTimeout } from '../hooks/useSessionTimeout';
import { useAuth } from '../context/AuthContext'; // Ajusta la ruta a tu contexto de auth
import { useNavigate } from 'react-router-dom';

export const SessionTimeoutHandler = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Usar el tiempo del backend o 1800 (30 min) por defecto
  const timeout = user ? (user.sessionTimeoutSeconds || 1800) : 0;

  useSessionTimeout(timeout, () => {
    // Aquí podrías disparar un modal antes de cerrar, 
    // pero por requerimiento cerraremos de inmediato.
    logout();
    alert("Su sesión ha expirado por inactividad. Por favor, ingrese de nuevo.");
    navigate('/login');
  });

  return <>{children}</>;
};