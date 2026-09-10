import { useState, useEffect, type FormEvent } from 'react'
import { motion } from 'framer-motion'
import { CheckCircle2, AlertCircle, ArrowRight, User } from 'lucide-react'
import { userService } from '../../../services/user.service'
import { useAuth } from '../../../contexts/AuthContext'
import { NAVY, BLUE, CYAN, WHITE, BODY, MUTE, HAIRLINE_CYAN } from '../theme'

const inputCls = 'w-full px-4 py-2.5 text-sm rounded-lg outline-none transition-all'
const inputStyle: React.CSSProperties = {
  background: WHITE,
  border: `1px solid ${HAIRLINE_CYAN}`,
  color: NAVY,
}
const focusHandlers = {
  onFocus: (e: React.FocusEvent<HTMLInputElement>) => {
    e.currentTarget.style.borderColor = BLUE
    e.currentTarget.style.boxShadow = '0 0 0 3px rgba(53,114,239,0.12)'
  },
  onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
    e.currentTarget.style.borderColor = HAIRLINE_CYAN
    e.currentTarget.style.boxShadow = 'none'
  },
}

function FieldGroup({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label className="block text-xs font-semibold uppercase" style={{ color: BLUE, letterSpacing: '0.1em' }}>
        {label}
        {hint && <span className="ml-1.5 text-xs font-normal normal-case" style={{ color: MUTE, letterSpacing: 0 }}>· {hint}</span>}
      </label>
      {children}
    </div>
  )
}

function SectionCard({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="p-7 rounded-2xl"
      style={{ background: WHITE, border: `1px solid ${HAIRLINE_CYAN}` }}
    >
      <div className="mb-6">
        <h2 className="text-lg font-semibold tracking-tight" style={{ color: NAVY, letterSpacing: '-0.02em' }}>
          {title}
        </h2>
        {subtitle && <p className="text-sm mt-1.5" style={{ color: BODY }}>{subtitle}</p>}
      </div>
      {children}
    </motion.div>
  )
}

export function ProfilePage() {
  const { updateSessionName } = useAuth()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  // Profile data
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  
  // Password change
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  useEffect(() => {
    userService.getProfile()
      .then(user => {
        setName(user.name)
        setEmail(user.email)
        setLoading(false)
      })
      .catch(err => {
        setError(err instanceof Error ? err.message : 'Error al cargar perfil')
        setLoading(false)
      })
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(false)

    if (!name.trim()) {
      setError('El nombre no puede estar vacío.')
      return
    }

    if (newPassword || currentPassword) {
      if (!currentPassword) {
        setError('Debes ingresar tu contraseña actual para cambiarla.')
        return
      }
      if (newPassword.length < 8) {
        setError('La nueva contraseña debe tener al menos 8 caracteres.')
        return
      }
      if (newPassword !== confirmPassword) {
        setError('Las contraseñas no coinciden.')
        return
      }
    }

    setSaving(true)
    try {
      await userService.updateProfile({
        name,
        currentPassword: currentPassword || undefined,
        newPassword: newPassword || undefined
      })

      updateSessionName?.(name)
      setSuccess(true)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      
      
      setTimeout(() => setSuccess(false), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al actualizar perfil')
    } finally {
      setSaving(false)
    }
  }

  if (loading && !error) {
    return <div className="p-8 text-center text-sm text-gray-500">Cargando perfil...</div>
  }

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <div className="w-12 h-12 rounded-full mb-4 flex items-center justify-center" style={{ background: BLUE, color: WHITE }}>
          <User size={24} strokeWidth={2} />
        </div>
        <h1 className="font-semibold tracking-tight text-3xl" style={{ color: NAVY }}>
          {name || 'Cargando...'}
        </h1>
        <p className="text-sm mt-2" style={{ color: BODY }}>
          {email || 'Cargando...'}
        </p>
      </div>

      {/* Feedback */}
      {error && (
        <div className="flex items-start gap-2.5 px-4 py-3 rounded-lg text-sm" style={{ background: '#fee', color: '#c00' }}>
          <AlertCircle size={15} strokeWidth={2} className="shrink-0 mt-0.5" />
          {error}
        </div>
      )}
      {success && (
        <motion.div
          initial={{ opacity: 0, scale: 0.97 }}
          animate={{ opacity: 1, scale: 1 }}
          className="flex items-center gap-2.5 px-4 py-3 rounded-lg text-sm"
          style={{ background: 'rgba(58,190,249,0.13)', color: NAVY, border: `1px solid ${HAIRLINE_CYAN}` }}
        >
          <CheckCircle2 size={15} strokeWidth={2} style={{ color: CYAN }} />
          Perfil actualizado correctamente.
        </motion.div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <SectionCard title="Información" subtitle="Este es el nombre que verán los demás usuarios del sistema.">
          <FieldGroup label="Nombre">
            <input
              type="text"
              className={inputCls}
              style={inputStyle}
              value={name}
              onChange={e => setName(e.target.value)}
              disabled={saving}
              {...focusHandlers}
            />
          </FieldGroup>
        </SectionCard>

        <SectionCard title="Seguridad" subtitle="Cambia tu contraseña si lo consideras necesario. Déjalo en blanco si no deseas cambiarla.">
          <div className="space-y-5">
            <FieldGroup label="Contraseña actual" hint="requerida solo para cambiarla">
              <input
                type="password"
                className={inputCls}
                style={inputStyle}
                value={currentPassword}
                onChange={e => setCurrentPassword(e.target.value)}
                disabled={saving}
                {...focusHandlers}
              />
            </FieldGroup>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <FieldGroup label="Nueva contraseña">
                <input
                  type="password"
                  className={inputCls}
                  style={inputStyle}
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  disabled={saving}
                  {...focusHandlers}
                />
              </FieldGroup>
              <FieldGroup label="Confirmar nueva">
                <input
                  type="password"
                  className={inputCls}
                  style={inputStyle}
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  disabled={saving}
                  {...focusHandlers}
                />
              </FieldGroup>
            </div>
          </div>
        </SectionCard>

        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={saving}
            className="group inline-flex items-center gap-2 px-6 py-3 rounded-full text-sm font-medium transition-all"
            style={{
              background: NAVY, color: WHITE, border: 'none', cursor: saving ? 'not-allowed' : 'pointer',
              opacity: saving ? 0.6 : 1, boxShadow: '0 10px 30px -10px rgba(5,12,156,0.5)',
            }}
          >
            {saving ? 'Guardando...' : 'Guardar cambios'}
            <ArrowRight size={16} strokeWidth={2} className="transition-transform group-hover:translate-x-1" />
          </button>
        </div>
      </form>
    </div>
  )
}
