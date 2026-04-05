# Estructura de Carpetas - Frontend Pages

## Nueva Organización

La carpeta `pages/` ha sido reorganizada por módulos funcionales para mejorar la mantenibilidad y escalabilidad del proyecto.

```
pages/
├── auth/                    # Autenticación y gestión de sesión
│   ├── Login.jsx
│   ├── ForgotPassword.jsx
│   ├── ResetPassword.jsx
│   ├── ChangePassword.jsx
│   ├── ValidateSite.jsx
│   └── index.js
│
├── dashboard/               # Páginas del dashboard
│   ├── Dashboard.jsx
│   ├── DashboardHome.jsx
│   └── index.js
│
├── users/                   # Gestión de usuarios
│   ├── Users.jsx
│   └── index.js
│
├── roles/                   # Gestión de roles
│   ├── Roles.jsx
│   └── index.js
│
├── security/                # Seguridad y políticas
│   ├── PasswordPolicy.jsx
│   ├── PasswordHistory.jsx
│   ├── ReservedUsernames.jsx
│   ├── SecurityLog.jsx
│   └── index.js
│
├── config/                  # Configuraciones del sistema
│   ├── AdConfig.jsx
│   ├── MailConfig.jsx
│   ├── MenuConfig.jsx
│   └── index.js
│
├── contracts/               # Gestión de contratos
│   ├── ContractList.jsx
│   ├── ContractForm.jsx
│   └── index.js
│
└── projects/                # Gestión de proyectos
    ├── ProjectList.jsx
    ├── ProjectForm.jsx
    └── index.js
```

## Ventajas de esta estructura

### 1. Organización por dominio
Cada carpeta agrupa páginas relacionadas funcionalmente, facilitando la navegación y comprensión del código.

### 2. Escalabilidad
Fácil agregar nuevas páginas o módulos sin afectar la estructura existente.

### 3. Imports simplificados
Los archivos `index.js` permiten imports más limpios:

```javascript
// Antes
import Login from './pages/Login'
import ForgotPassword from './pages/ForgotPassword'

// Ahora
import { Login, ForgotPassword } from './pages/auth'
```

### 4. Separación de responsabilidades
Cada módulo tiene su propio espacio, reduciendo acoplamiento.

### 5. Mantenibilidad
Más fácil localizar y modificar páginas específicas.

## Convenciones

### Nomenclatura de archivos
- **PascalCase** para componentes: `UserList.jsx`, `ContractForm.jsx`
- **Sufijos descriptivos**: 
  - `*List.jsx` para listados
  - `*Form.jsx` para formularios
  - `*Detail.jsx` para vistas de detalle

### Estructura de carpetas
- Una carpeta por módulo funcional
- Archivo `index.js` para exports centralizados
- Máximo 2 niveles de profundidad

### Imports
Usar imports nombrados desde el index:
```javascript
import { ComponentA, ComponentB } from './pages/module'
```

## Migración completada

✅ Todos los archivos movidos a sus respectivas carpetas
✅ Imports actualizados en `App.jsx`
✅ Archivos `index.js` creados para cada módulo
✅ Estructura documentada

## Próximos pasos recomendados

1. **Crear componentes compartidos** para reducir duplicación:
   - `<DataTable />` - Tabla reutilizable con paginación
   - `<SearchBar />` - Barra de búsqueda
   - `<PageHeader />` - Encabezado de página
   - `<ActionButtons />` - Botones de acción

2. **Agregar tests** por módulo:
   ```
   pages/
   ├── auth/
   │   ├── Login.jsx
   │   ├── Login.test.jsx
   │   └── ...
   ```

3. **Documentar cada módulo** con README.md si es necesario

4. **Implementar lazy loading** para optimizar carga:
   ```javascript
   const Login = lazy(() => import('./pages/auth/Login'))
   ```
