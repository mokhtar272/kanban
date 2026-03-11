import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/services/api'

export const useAuthStore = defineStore('auth', () => {
  const user  = ref(JSON.parse(localStorage.getItem('kanban_user')  || 'null'))
  const token = ref(localStorage.getItem('kanban_token') || '')
  const error = ref(null)

  const isAuthenticated = computed(() => !!user.value)
  const isAdmin         = computed(() => user.value?.role === 'ADMIN')

  function save(tok, usr) {
    token.value = tok
    user.value  = usr
    localStorage.setItem('kanban_token', tok)
    localStorage.setItem('kanban_user',  JSON.stringify(usr))
  }

  async function login(pseudo, password) {
    error.value = null
    const res = await authAPI.login({ pseudo, password })
    if (!res.success || !res.token) { error.value = res.message || 'Identifiants incorrects'; return false }
    save(res.token, res.user)
    return true
  }

  async function register(data) {
    error.value = null
    const res = await authAPI.register(data)
    if (!res.success || !res.token) { error.value = res.message || 'Erreur inscription'; return false }
    save(res.token, res.user)
    return true
  }

  function logout() {
    user.value = null; token.value = ''
    localStorage.removeItem('kanban_token')
    localStorage.removeItem('kanban_user')
  }

  return { user, token, error, isAuthenticated, isAdmin, login, register, logout }
})
