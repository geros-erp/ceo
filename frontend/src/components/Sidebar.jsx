import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as FiIcons from 'react-icons/fi'
import * as LuIcons from 'react-icons/lu'

// Combinamos ambos sets de iconos
const Icons = { ...FiIcons, ...LuIcons };

function MenuNode({ item, level = 0 }) {
  const [open, setOpen] = useState(true)
  const navigate = useNavigate()
  const location = useLocation()
  const hasChildren = item.children?.length > 0
  const isActive = item.path && location.pathname === item.path

  // Normalización robusta de nombres de iconos
  const getIconComponent = (iconName) => {
    if (!iconName) return Icons['FiCircle'];

    // 1. Limpiar espacios y nombres comunes de clases (ej: "fi fi-users" -> "users")
    let cleanName = iconName.split(' ').pop().replace(/^fi-/, '');

    // 2. Convertir kebab-case o snake_case a PascalCase (ej: "shopping-cart" -> "ShoppingCart")
    const pascalName = cleanName
      .replace(/[-_ ]+(.)/g, (_, c) => c.toUpperCase())
      .replace(/^(.)/, (c) => c.toUpperCase());

    // 3. Intentar encontrar con el nombre exacto (PascalCase)
    const exactMatch = Icons[pascalName];
    if (exactMatch) return exactMatch;

    // 4. Intentar con prefijos Fi (Feather) o Lu (Lucide)
    const prefixes = ['Fi', 'Lu'];
    for (const pref of prefixes) {
      const prefixed = pascalName.startsWith(pref) ? pascalName : `${pref}${pascalName}`;
      if (Icons[prefixed]) return Icons[prefixed];
    }

    console.warn(`⚠️ Icono no encontrado en react-icons (Fi/Lu): "${iconName}"`);
    return Icons['FiCircle']; // Icono por defecto para no dejar el espacio vacío
  };

  const IconComponent = getIconComponent(item.icon);

  const handleClick = () => {
    if (hasChildren) setOpen(o => !o)
    else if (item.path) navigate(item.path)
  }

  return (
    <li>
      <div
        onClick={handleClick}
        style={{ paddingLeft: `${0.75 + level * 0.875}rem` }}
        className={`flex items-center gap-2 py-2 pr-3 mx-1.5 rounded-md cursor-pointer text-sm select-none transition-colors
          ${isActive ? 'bg-indigo-600 text-white' : 'text-indigo-200 hover:bg-white/10 hover:text-white'}
          ${hasChildren ? 'font-semibold text-indigo-300' : ''}`}
      >
        {IconComponent && <IconComponent className="w-5 h-5 shrink-0" />}
        <span className="flex-1">{item.label}</span>
        {hasChildren && <span className="text-xs text-indigo-400">{open ? '▾' : '▸'}</span>}
      </div>
      {hasChildren && open && (
        <ul>
          {item.children.map(child => (
            <MenuNode key={child.id} item={child} level={level + 1} />
          ))}
        </ul>
      )}
    </li>
  )
}

export default function Sidebar() {
  const { menu } = useAuth()

  return (
    <aside className="w-60 min-w-[240px] bg-indigo-950 min-h-[calc(100vh-56px)] overflow-y-auto py-3">
      <nav>
        <ul className="list-none p-0 m-0">
          {menu.map(item => <MenuNode key={item.id} item={item} />)}
        </ul>
      </nav>
    </aside>
  )
}
