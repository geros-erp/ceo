# 🚀 GUÍA RÁPIDA DE INICIO

## Para Continuar el Desarrollo

### 1. Iniciar Servicios

**Opción A: Usar VS Code (Recomendado)**
```
Presiona: Ctrl + Shift + B
```
Esto iniciará automáticamente backend y frontend en terminales integrados.

**Opción B: Manual**
```bash
# Terminal 1 - Backend
cd backend
mvnw.cmd spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm run dev
```

---

### 2. Acceder a la Aplicación

- **Frontend:** http://localhost:5173
- **Backend API:** https://localhost:8443/api
- **Credenciales de prueba:**
  - Username: `admin`
  - Password: `admin123`

---

### 3. Probar Funcionalidades Implementadas

#### A. Timeout de Sesión
1. Iniciar sesión
2. Ir a "Política de Contraseñas"
3. Cambiar "Timeout de sesión" a 120 segundos (2 minutos)
4. Guardar cambios
5. No tocar nada por 1 minuto → Aparece modal de advertencia
6. Esperar 1 minuto más → Sesión se cierra automáticamente

#### B. Logs de Auditoría
1. Iniciar sesión
2. Ir a "Política de Contraseñas"
3. Cambiar cualquier valor (ej: timeout de sesión)
4. Guardar cambios
5. Ir a "Log de Seguridad"
6. Buscar evento "PASSWORD_POLICY_UPDATED"
7. Verificar que incluye valores anteriores y nuevos

---

### 4. Estructura del Proyecto

```
geros/
├── backend/          # Spring Boot API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/geros/backend/
│   │   │   │   ├── security/      # Autenticación y JWT
│   │   │   │   ├── policy/        # Políticas de seguridad
│   │   │   │   ├── securitylog/   # Logs de auditoría
│   │   │   │   └── user/          # Gestión de usuarios
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── db/init.sql
│   │   └── test/
│   └── pom.xml
│
├── frontend/         # React + Vite
│   ├── src/
│   │   ├── components/
│   │   │   ├── SessionTimeout.jsx        # ✅ NUEVO
│   │   │   └── SessionWarningModal.jsx   # ✅ NUEVO
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── PasswordPolicy.jsx
│   │   │   └── SecurityLog.jsx
│   │   ├── api/
│   │   │   └── auth.js
│   │   └── context/
│   │       └── AuthContext.jsx
│   └── package.json
│
├── docs/             # Documentación
│   ├── SESSION_TIMEOUT.md              # ✅ NUEVO
│   ├── RESUMEN_SEGURIDAD.md            # ✅ NUEVO
│   ├── PREVENCION_ROBO_SESION.md       # ✅ NUEVO
│   ├── RESUMEN_EJECUTIVO.md            # ✅ NUEVO
│   └── GUIA_RAPIDA.md                  # ✅ ESTE ARCHIVO
│
└── .vscode/
    └── tasks.json    # ✅ Configuración de tareas
```

---

### 5. Comandos Útiles

#### Backend
```bash
# Compilar
cd backend
mvnw.cmd clean compile

# Ejecutar tests
mvnw.cmd test

# Empaquetar
mvnw.cmd package

# Ver dependencias
mvnw.cmd dependency:tree
```

#### Frontend
```bash
# Instalar dependencias
cd frontend
npm install

# Desarrollo
npm run dev

# Build para producción
npm run build

# Preview de build
npm run preview
```

#### Base de Datos
```bash
# Conectar a PostgreSQL
psql -U postgres -d geros

# Ver usuarios
SELECT email, username, active_sessions FROM auth.users;

# Resetear sesiones
UPDATE auth.users SET active_sessions = 0;

# Ver logs de seguridad (últimos 10)
SELECT * FROM auth.security_log ORDER BY timestamp DESC LIMIT 10;
```

---

### 6. Archivos de Configuración Importantes

#### Backend: `application.properties`
```properties
# Puerto HTTPS
server.port=8443

# JWT
jwt.secret=geros-secret-key-change-in-production
jwt.expiration=86400000  # 24 horas

# Sesiones concurrentes
app.auth.max-concurrent-sessions=5

# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/geros
spring.datasource.username=postgres
spring.datasource.password=postgres
```

#### Frontend: `vite.config.js`
```javascript
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'https://localhost:8443',
        secure: false,
        changeOrigin: true,
      },
    },
  },
})
```

---

### 7. Solución de Problemas Comunes

#### Backend no inicia
```bash
# Verificar si el puerto está en uso
netstat -ano | findstr :8443

# Matar proceso si es necesario
taskkill /F /PID <PID>

# Verificar PostgreSQL
netstat -ano | findstr :5432
```

#### Frontend no conecta con Backend
1. Verificar que backend esté corriendo en puerto 8443
2. Verificar proxy en `vite.config.js`
3. Abrir consola del navegador para ver errores
4. Verificar que HTTPS esté habilitado

#### Error de certificado SSL
- Es normal en desarrollo (certificado autofirmado)
- En navegador: Aceptar riesgo y continuar
- En producción: Usar certificado válido

#### Sesión no expira
1. Verificar que `sessionTimeoutSeconds` esté configurado
2. Verificar que `SessionTimeout` componente esté montado
3. Abrir consola del navegador para ver logs
4. Verificar que no haya errores en el componente

---

### 8. Próximos Pasos de Desarrollo

#### Esta Semana
1. [ ] Implementar validación de IP en tokens
2. [ ] Crear tests para timeout de sesión
3. [ ] Documentar API endpoints

#### Próxima Semana
1. [ ] Implementar sistema de refresh tokens
2. [ ] Agregar rate limiting
3. [ ] Mejorar manejo de errores

---

### 9. Recursos y Referencias

#### Documentación Técnica
- [SESSION_TIMEOUT.md](./SESSION_TIMEOUT.md) - Detalles del timeout
- [RESUMEN_SEGURIDAD.md](./RESUMEN_SEGURIDAD.md) - Estado de seguridad
- [PREVENCION_ROBO_SESION.md](./PREVENCION_ROBO_SESION.md) - Plan de seguridad
- [RESUMEN_EJECUTIVO.md](./RESUMEN_EJECUTIVO.md) - Resumen ejecutivo

#### Enlaces Útiles
- Spring Boot: https://spring.io/projects/spring-boot
- React: https://react.dev
- Vite: https://vitejs.dev
- JWT: https://jwt.io
- OWASP: https://owasp.org

---

### 10. Checklist Diario

#### Al Iniciar el Día
- [ ] Hacer pull de cambios recientes
- [ ] Iniciar servicios (Ctrl + Shift + B)
- [ ] Verificar que todo funciona
- [ ] Revisar issues/tareas pendientes

#### Durante el Desarrollo
- [ ] Hacer commits frecuentes
- [ ] Escribir tests para nuevo código
- [ ] Actualizar documentación si es necesario
- [ ] Probar cambios localmente

#### Al Finalizar el Día
- [ ] Hacer push de cambios
- [ ] Actualizar estado de tareas
- [ ] Documentar problemas encontrados
- [ ] Detener servicios

---

### 11. Contactos de Emergencia

#### Problemas Técnicos
- Revisar logs en `backend/storage/security-log-exports/`
- Consultar documentación en `/docs`
- Verificar configuración en `application.properties`

#### Errores de Base de Datos
```sql
-- Verificar conexión
SELECT version();

-- Ver tablas
\dt auth.*

-- Ver estructura de tabla
\d auth.users
```

---

### 12. Tips y Trucos

#### VS Code
- `Ctrl + Shift + B` - Reiniciar servicios
- `Ctrl + Shift + P` - Paleta de comandos
- `Ctrl + `` - Toggle terminal
- `Ctrl + P` - Buscar archivo

#### Git
```bash
# Ver estado
git status

# Crear rama
git checkout -b feature/nombre

# Commit
git add .
git commit -m "Descripción"

# Push
git push origin feature/nombre
```

#### Debugging
- Backend: Agregar breakpoints en IntelliJ/VS Code
- Frontend: Usar React DevTools
- Network: Usar DevTools → Network tab
- Console: Siempre revisar consola del navegador

---

## 🎯 Objetivo Inmediato

**Validar que todo funciona correctamente:**

1. ✅ Iniciar servicios
2. ✅ Login exitoso
3. ✅ Modificar timeout de sesión
4. ✅ Verificar log de auditoría
5. ✅ Probar cierre automático de sesión

**Si todo funciona → Continuar con validación de IP**

---

## 📞 ¿Necesitas Ayuda?

1. Revisar documentación en `/docs`
2. Consultar logs de error
3. Verificar configuración
4. Buscar en issues del proyecto
5. Preguntar al equipo

---

**¡Éxito en el desarrollo! 🚀**

---

**Última actualización:** 2026-04-03  
**Versión:** 1.0  
**Mantenido por:** Equipo GEROS
