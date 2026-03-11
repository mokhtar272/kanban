<template>
  <nav class="fixed top-0 inset-x-0 z-50 h-14 bg-[#0f1117]/90 backdrop-blur-md border-b border-gray-800/60">
    <div class="max-w-7xl mx-auto px-4 flex items-center justify-between h-full gap-4">

      <!-- Logo -->
      <router-link to="/" class="flex items-center gap-2 group">
        <div class="w-7 h-7 bg-brand-600 rounded-lg flex items-center justify-center text-white text-sm font-bold shadow shadow-brand-900/50">K</div>
        <span class="font-bold text-white text-base tracking-tight hidden sm:block">KanbanApp</span>
      </router-link>

      <!-- Breadcrumb board -->
      <div v-if="route.name === 'board' && boardTitle" class="flex items-center gap-2 text-sm">
        <router-link to="/" class="text-gray-500 hover:text-gray-300 transition-colors">Tableaux</router-link>
        <span class="text-gray-700">/</span>
        <span class="text-white font-medium truncate max-w-[200px]">{{ boardTitle }}</span>
      </div>

      <div class="flex-1"></div>

      <!-- Actions -->
      <div class="flex items-center gap-2">
        <router-link v-if="auth.isAdmin" to="/admin"
          class="btn btn-ghost btn-sm text-xs text-purple-400 hover:text-purple-300 hover:bg-purple-900/20">
          🛠 Admin
        </router-link>

        <!-- User menu -->
        <div class="relative" ref="menuRef">
          <button @click="open = !open"
            class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-800 transition-colors">
            <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold ring-2 ring-gray-700"
              :style="`background: ${avatarColor}`">
              {{ auth.user?.pseudo?.[0]?.toUpperCase() }}
            </div>
            <span class="hidden sm:block text-sm text-gray-300 max-w-[100px] truncate">{{ auth.user?.pseudo }}</span>
            <svg class="w-3 h-3 text-gray-500 transition-transform" :class="open ? 'rotate-180' : ''"
              fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>

          <Transition name="dropdown">
            <div v-if="open"
              class="absolute right-0 mt-2 w-56 bg-[#161b27] border border-gray-700/80 rounded-xl shadow-2xl py-1.5 z-50">
              <div class="px-4 py-3 border-b border-gray-800">
                <p class="text-xs text-gray-500">Connecté en tant que</p>
                <p class="text-sm font-semibold text-white truncate mt-0.5">{{ auth.user?.pseudo }}</p>
                <p class="text-xs text-gray-500 truncate">{{ auth.user?.email }}</p>
                <span v-if="auth.isAdmin" class="badge-purple text-xs mt-1">Admin</span>
              </div>
              <router-link to="/" class="menu-item" @click="open = false">
                <span>📋</span> Mes tableaux
              </router-link>
              <div class="border-t border-gray-800 mt-1 pt-1">
                <button @click="logout" class="menu-item text-red-400 hover:text-red-300 hover:bg-red-900/20 w-full">
                  <span>🚪</span> Déconnexion
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth      = useAuthStore()
const router    = useRouter()
const route     = useRoute()
const open      = ref(false)
const menuRef   = ref(null)
const boardTitle = inject('boardTitle', ref(''))

// Couleur avatar déterministe basée sur le pseudo
const colors = ['#3b82f6','#8b5cf6','#ec4899','#10b981','#f59e0b','#6366f1','#14b8a6']
const avatarColor = computed(() => {
  const s = auth.user?.pseudo || ''
  return colors[s.charCodeAt(0) % colors.length]
})

function logout() { auth.logout(); open.value = false; router.push('/login') }
function outside(e) { if (menuRef.value && !menuRef.value.contains(e.target)) open.value = false }
onMounted(()  => document.addEventListener('click', outside))
onUnmounted(() => document.removeEventListener('click', outside))
</script>

<style scoped>
.menu-item {
  @apply flex items-center gap-2.5 px-4 py-2 text-sm text-gray-300
         hover:bg-gray-800/70 hover:text-white transition-colors cursor-pointer;
}
.dropdown-enter-active, .dropdown-leave-active { transition: all .15s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-6px) scale(.97); }
</style>
