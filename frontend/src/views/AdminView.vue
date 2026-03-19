<template>
  <div class="max-w-5xl mx-auto px-6 py-8">
    <!-- Header -->
    <div class="flex items-center gap-3 mb-8">
      <div class="w-10 h-10 bg-purple-700 rounded-xl flex items-center justify-center text-white text-lg">🛠</div>
      <div>
        <h1 class="text-xl font-bold text-white">Administration</h1>
        <p class="text-gray-500 text-sm">Gestion de la plateforme</p>
      </div>
    </div>

    <!-- Stats cards -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="card p-5">
        <div class="flex items-center justify-between mb-3">
          <span class="text-gray-500 text-sm">Utilisateurs</span>
          <span class="text-2xl">👤</span>
        </div>
        <p class="text-3xl font-bold text-white">{{ stats.nbUtilisateurs ?? '—' }}</p>
        <p class="text-xs text-green-400 mt-1">Comptes actifs</p>
      </div>
      <div class="card p-5">
        <div class="flex items-center justify-between mb-3">
          <span class="text-gray-500 text-sm">Tableaux</span>
          <span class="text-2xl">📋</span>
        </div>
        <p class="text-3xl font-bold text-white">{{ stats.nbTableaux ?? '—' }}</p>
        <p class="text-xs text-blue-400 mt-1">Boards Kanban</p>
      </div>
      <div class="card p-5">
        <div class="flex items-center justify-between mb-3">
          <span class="text-gray-500 text-sm">Tâches</span>
          <span class="text-2xl">✅</span>
        </div>
        <p class="text-3xl font-bold text-white">{{ stats.nbTaches ?? '—' }}</p>
        <p class="text-xs text-purple-400 mt-1">Total créées</p>
      </div>
    </div>

    <!-- Activité utilisateurs -->
    <div v-if="activite.length > 0" class="card p-5 mb-6">
      <h2 class="font-semibold text-white mb-4">📊 Activité des utilisateurs</h2>
      <div class="space-y-2">
        <div v-for="(a, idx) in activite" :key="a.userId"
          class="flex items-center gap-3">
          <span class="text-xs text-gray-600 w-4">{{ idx+1 }}</span>
          <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0"
            :style="`background: ${avatarColor(a.pseudo)}`">
            {{ a.pseudo?.[0]?.toUpperCase() }}
          </div>
          <span class="text-sm text-gray-300 w-28 truncate">{{ a.pseudo }}</span>
          <div class="flex-1 bg-gray-800 rounded-full h-2">
            <div class="h-2 rounded-full transition-all"
              :style="`width: ${Math.round((a.actions / activite[0].actions) * 100)}%; background: ${avatarColor(a.pseudo)}`">
            </div>
          </div>
          <span class="text-xs text-gray-400 w-16 text-right">{{ a.actions }} action(s)</span>
        </div>
      </div>
    </div>

    <!-- Erreur globale -->
    <div v-if="globalError" class="alert-error mb-4">{{ globalError }}</div>

    <!-- Utilisateurs -->
    <div class="card overflow-hidden">
      <div class="flex items-center justify-between px-5 py-4 border-b border-gray-800">
        <h2 class="font-semibold text-white">Utilisateurs</h2>
        <input v-model="search" class="input-sm w-48" placeholder="🔍 Rechercher..." />
      </div>

      <div v-if="loading" class="p-8 text-center text-gray-500">Chargement...</div>

      <table v-else class="w-full text-sm">
        <thead class="bg-gray-900/60">
          <tr>
            <th class="text-left px-5 py-3 text-gray-500 font-medium text-xs uppercase tracking-wide">Utilisateur</th>
            <th class="text-left px-5 py-3 text-gray-500 font-medium text-xs uppercase tracking-wide hidden md:table-cell">Email</th>
            <th class="text-left px-5 py-3 text-gray-500 font-medium text-xs uppercase tracking-wide">Rôle</th>
            <th class="text-left px-5 py-3 text-gray-500 font-medium text-xs uppercase tracking-wide hidden sm:table-cell">Inscrit</th>
            <th class="px-5 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-800/50">
          <tr v-for="u in filteredUsers" :key="u.id" class="hover:bg-gray-800/20 transition-colors">
            <td class="px-5 py-3">
              <div class="flex items-center gap-2.5">
                <div class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold"
                  :style="`background: ${avatarColor(u.pseudo)}`">
                  {{ u.pseudo?.[0]?.toUpperCase() }}
                </div>
                <div>
                  <p class="text-white font-medium text-sm">{{ u.pseudo }}</p>
                  <p class="text-gray-500 text-xs md:hidden">{{ u.email }}</p>
                </div>
              </div>
            </td>
            <td class="px-5 py-3 text-gray-400 text-sm hidden md:table-cell">{{ u.email }}</td>
            <td class="px-5 py-3">
              <span :class="u.role === 'ADMIN' ? 'badge-purple' : 'badge-gray'" class="badge text-xs">
                {{ u.role }}
              </span>
            </td>
            <td class="px-5 py-3 text-gray-500 text-xs hidden sm:table-cell">{{ formatDate(u.createdAt) }}</td>
            <td class="px-5 py-3 text-right">
              <button v-if="u.id !== auth.user?.id"
                @click="deleteUser(u)"
                class="btn btn-ghost btn-xs text-red-500 hover:text-red-400 hover:bg-red-900/20">
                Supprimer
              </button>
              <span v-else class="text-xs text-gray-600">Vous</span>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!loading && filteredUsers.length === 0" class="px-5 py-8 text-center text-gray-500 text-sm">
        Aucun utilisateur trouvé.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth        = useAuthStore()
const stats       = ref({})
const activite    = ref([])
const users       = ref([])
const loading     = ref(true)
const search      = ref('')
const globalError = ref('')

const AVATAR_COLORS = ['#3b82f6','#8b5cf6','#ec4899','#10b981','#f59e0b','#6366f1','#14b8a6']
function avatarColor(s='') { return AVATAR_COLORS[(s?.charCodeAt(0) || 0) % AVATAR_COLORS.length] }
function formatDate(d) { if (!d) return '—'; return new Date(d).toLocaleDateString('fr-FR') }

const filteredUsers = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(u =>
    u.pseudo?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
  )
})

async function deleteUser(u) {
  if (!confirm(`Supprimer définitivement "${u.pseudo}" ? Cette action est irréversible.`)) return
  globalError.value = ''
  const res = await adminAPI.deleteUser(u.id)
  if (!res.success) { globalError.value = res.message || 'Erreur lors de la suppression'; return }
  users.value = users.value.filter(x => x.id !== u.id)
  if (stats.value.nbUtilisateurs) stats.value.nbUtilisateurs--
}

onMounted(async () => {
  const [sRes, uRes] = await Promise.all([adminAPI.stats(), adminAPI.utilisateurs()])
  if (!sRes.success) { globalError.value = sRes.message || 'Impossible de charger les statistiques' }
  else {
    stats.value   = sRes.data ?? {}
    activite.value = (sRes.data?.activiteUtilisateurs ?? [])
  }
  if (!uRes.success) { globalError.value = uRes.message || 'Impossible de charger les utilisateurs' }
  else users.value = uRes.data ?? []
  loading.value = false
})
</script>
