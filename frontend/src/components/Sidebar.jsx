import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function MenuNode({ item, level = 0 }) {
  const [open, setOpen] = useState(true)
  const navigate = useNavigate()
  const location = useLocation()
  const hasChildren = item.children?.length > 0
  const isActive = item.path && location.pathname === item.path

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
        {item.icon && <span className="text-base shrink-0">{item.icon}</span>}
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
