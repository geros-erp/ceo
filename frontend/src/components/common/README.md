# Componentes Comunes Reutilizables

Esta carpeta contiene componentes UI reutilizables que eliminan duplicación de código en toda la aplicación.

## Componentes Disponibles

### 1. PageHeader
Encabezado consistente para todas las páginas con botón de navegación.

**Props:**
- `title` (string, requerido): Título de la página
- `subtitle` (string, opcional): Subtítulo descriptivo
- `backTo` (string, opcional): Ruta de navegación (default: '/dashboard')
- `actionButton` (ReactNode, opcional): Botón de acción personalizado

**Ejemplo:**
```jsx
import { PageHeader } from '../../components/common'

<PageHeader title="Gestión de Usuarios" />
```

### 2. Pagination
Componente de paginación para tablas con navegación anterior/siguiente.

**Props:**
- `page` (number, requerido): Página actual (base 0)
- `totalPages` (number, requerido): Total de páginas
- `total` (number, requerido): Total de registros
- `onPageChange` (function, requerido): Callback al cambiar página

**Ejemplo:**
```jsx
import { Pagination } from '../../components/common'

<Pagination 
  page={page} 
  totalPages={totalPages} 
  total={total} 
  onPageChange={setPage} 
/>
```

### 3. SearchBar
Barra de búsqueda con estilos consistentes.

**Props:**
- `value` (string, requerido): Valor actual
- `onChange` (function, requerido): Callback al cambiar valor
- `placeholder` (string, opcional): Texto placeholder (default: "Buscar...")
- `className` (string, opcional): Clases CSS adicionales

**Ejemplo:**
```jsx
import { SearchBar } from '../../components/common'

<SearchBar 
  placeholder="Buscar por nombre o email..."
  value={search} 
  onChange={e => setSearch(e.target.value)} 
/>
```

### 4. StatusBadge
Badge de estado con variantes de color predefinidas.

**Props:**
- `status` (string, opcional): Estado a mostrar
- `label` (string, opcional): Etiqueta personalizada (prioridad sobre status)
- `variant` (string, opcional): Variante de color (default: 'default')
  - Variantes: 'success', 'warning', 'error', 'info', 'purple', 'gray', 'default'

**Ejemplo:**
```jsx
import { StatusBadge } from '../../components/common'

<StatusBadge variant="success" label="✓ Activo" />
<StatusBadge variant="error" label="🔒 Bloqueado" />
<StatusBadge variant="warning" label="⚠ Cambio requerido" />
```

### 5. LoadingState
Estado de carga consistente para toda la aplicación.

**Props:**
- `message` (string, opcional): Mensaje de carga (default: "Cargando...")

**Ejemplo:**
```jsx
import { LoadingState } from '../../components/common'

{loading ? <LoadingState /> : <DataTable />}
{loading ? <LoadingState message="Cargando usuarios..." /> : <UserList />}
```

## Importación

Todos los componentes se pueden importar desde un solo punto:

```jsx
import { PageHeader, Pagination, SearchBar, StatusBadge, LoadingState } from '../../components/common'
```

O individualmente:

```jsx
import PageHeader from '../../components/common/PageHeader'
import Pagination from '../../components/common/Pagination'
```

## Archivos Actualizados

Los siguientes archivos ya están usando estos componentes:

1. `pages/users/Users.jsx` - PageHeader, Pagination, SearchBar, StatusBadge, LoadingState
2. `pages/roles/Roles.jsx` - PageHeader, StatusBadge
3. `pages/security/SecurityLog.jsx` - PageHeader, Pagination, StatusBadge, LoadingState
4. `pages/security/PasswordHistory.jsx` - PageHeader, LoadingState
5. `pages/security/PasswordPolicy.jsx` - PageHeader, LoadingState
6. `pages/security/ReservedUsernames.jsx` - PageHeader, LoadingState
7. `pages/config/MailConfig.jsx` - PageHeader, LoadingState
8. `pages/config/AdConfig.jsx` - PageHeader, LoadingState

## Beneficios

- **Reducción de código duplicado**: ~400-500 líneas eliminadas
- **Consistencia visual**: Todos los componentes usan los mismos estilos
- **Mantenibilidad**: Cambios en un solo lugar se reflejan en toda la app
- **Reutilización**: Fácil de usar en nuevas páginas
- **Tipado claro**: Props bien documentadas

## Próximos Pasos

Considerar crear componentes adicionales para:
- DataTable (tablas con estilos consistentes)
- Modal (modales reutilizables)
- FormInput (inputs de formulario)
- Button (botones con variantes)
