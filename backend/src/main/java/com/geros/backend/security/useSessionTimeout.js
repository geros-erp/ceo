import { useEffect, useCallback, useRef } from 'react';

/**
 * Hook para controlar el cierre de sesión por inactividad.
 * @param {number} timeoutInSeconds - Segundos permitidos de inactividad.
 * @param {function} onLogout - Acción a ejecutar cuando se agote el tiempo.
 */
export const useSessionTimeout = (timeoutInSeconds, onLogout) => {
  const timerRef = useRef(null);

  const resetTimer = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    if (!timeoutInSeconds || timeoutInSeconds <= 0) return;

    timerRef.current = setTimeout(() => {
      onLogout();
    }, timeoutInSeconds * 1000);
  }, [timeoutInSeconds, onLogout]);

  useEffect(() => {
    // Eventos que reinician el contador de inactividad
    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
    
    const handleActivity = () => resetTimer();

    events.forEach(event => document.addEventListener(event, handleActivity));
    
    // Iniciar el temporizador al cargar el hook
    resetTimer();

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      events.forEach(event => document.removeEventListener(event, handleActivity));
    };
  }, [resetTimer]);

  return { resetTimer };
};