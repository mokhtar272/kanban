<template>
  <div class="min-h-screen bg-[#0f1117] flex">
    <!-- Left panel decoratif -->
    <div class="hidden lg:flex flex-1 bg-gradient-to-br from-brand-900/40 to-purple-900/20 items-center justify-center p-12 border-r border-gray-800/40">
      <div class="max-w-sm text-center">
        <div class="w-20 h-20 bg-brand-600 rounded-2xl flex items-center justify-center text-white text-4xl font-bold mx-auto mb-6 shadow-2xl shadow-brand-900/50">K</div>
        <h2 class="text-2xl font-bold text-white mb-3">Organisez votre travail</h2>
        <p class="text-gray-400 leading-relaxed">Gérez vos projets avec des tableaux Kanban visuels et intuitifs.</p>
        <div class="mt-8 grid grid-cols-3 gap-3">
          <div v-for="c in ['#3b82f6','#8b5cf6','#10b981']" :key="c"
            class="h-16 rounded-xl opacity-60" :style="`background: ${c}22; border: 1px solid ${c}44`">
          </div>
        </div>
      </div>
    </div>

    <!-- Right panel login -->
    <div class="flex-1 lg:max-w-md flex items-center justify-center p-8">
      <div class="w-full max-w-sm">
        <div class="mb-8">
          <div class="w-10 h-10 bg-brand-600 rounded-xl flex items-center justify-center text-white font-bold text-lg mb-4 lg:hidden">K</div>
          <h1 class="text-2xl font-bold text-white">Bon retour 👋</h1>
          <p class="text-gray-500 text-sm mt-1">Connectez-vous à votre espace</p>
        </div>

        <div v-if="auth.error" class="alert-error mb-4 animate-slide-down">
          {{ auth.error }}
        </div>

        <div class="space-y-4">
          <div>
            <label class="label">Pseudo</label>
            <input v-model="form.pseudo" class="input" placeholder="votre_pseudo"
              @keyup.enter="submit" autocomplete="username" />
          </div>
          <div>
            <label class="label">Mot de passe</label>
            <div class="relative">
              <input v-model="form.password" :type="showPwd ? 'text' : 'password'"
                class="input pr-10" placeholder="••••••••"
                @keyup.enter="submit" autocomplete="current-password" />
              <button @click="showPwd = !showPwd"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 text-xs">
                {{ showPwd ? '🙈' : '👁' }}
              </button>
            </div>
          </div>

          <button @click="submit" :disabled="loading" class="btn btn-primary w-full py-2.5 mt-2">
            <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            <span>{{ loading ? 'Connexion...' : 'Se connecter' }}</span>
          </button>
        </div>

        <p class="text-center text-sm text-gray-500 mt-6">
          Pas encore de compte ?
          <router-link to="/register" class="text-brand-400 hover:text-brand-300 font-medium transition-colors">
            Créer un compte
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth    = useAuthStore()
const router  = useRouter()
const loading = ref(false)
const showPwd = ref(false)
const form    = ref({ pseudo: '', password: '' })

async function submit() {
  if (!form.value.pseudo || !form.value.password) return
  loading.value = true
  const ok = await auth.login(form.value.pseudo, form.value.password)
  loading.value = false
  if (ok) router.push('/')
}
</script>
