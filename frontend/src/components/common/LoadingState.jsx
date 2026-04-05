export default function LoadingState({ message = "Cargando..." }) {
  return (
    <p className="text-center text-gray-500 py-8">{message}</p>
  )
}
