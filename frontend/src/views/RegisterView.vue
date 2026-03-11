<template>
  <div class="min-h-screen bg-[#0f1117] flex items-center justify-center p-6">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="w-12 h-12 bg-brand-600 rounded-2xl flex items-center justify-center text-white text-2xl font-bold mx-auto mb-4">K</div>
        <h1 class="text-2xl font-bold text-white">Créer un compte</h1>
        <p class="text-gray-500 text-sm mt-1">Rejoignez KanbanApp et organisez vos projets</p>
      </div>

      <div class="card p-6">
        <div v-if="auth.error" class="alert-error mb-5 animate-slide-down">{{ auth.error }}</div>

        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="label">Prénom</label>
              <input v-model="form.prenom" class="input" placeholder="Alice" />
            </div>
            <div>
              <label class="label">Nom</label>
              <input v-model="form.nom" class="input" placeholder="Dupont" />
            </div>
          </div>
          <div>
            <label class="label">Pseudo <span class="text-red-400">*</span></label>
            <input v-model="form.pseudo" class="input" placeholder="alice_dupont"
              @input="checkPseudo" autocomplete="username" />
            <p v-if="pseudoErr" class="text-xs text-red-400 mt-1">{{ pseudoErr }}</p>
          </div>
          <div>
            <label class="label">Email <span class="text-red-400">*</span></label>
            <input v-model="form.email" type="email" class="input" placeholder="alice@example.com" />
          </div>
          <div>
            <label class="label">Mot de passe <span class="text-red-400">*</span></label>
            <div class="relative">
              <input v-model="form.password" :type="showPwd ? 'text' : 'password'"
                class="input pr-10" placeholder="Min. 6 caractères" />
              <button @click="showPwd = !showPwd"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 text-xs">
                {{ showPwd ? '🙈' : '👁' }}
              </button>
            </div>
            <!-- Indicateur force mot de passe -->
            <div v-if="form.password" class="mt-1.5 flex gap-1">
              <div v-for="i in 4" :key="i" class="h-1 flex-1 rounded-full transition-all"
                :class="i <= pwdStrength ? pwdColor : 'bg-gray-800'"></div>
            </div>
          </div>

          <button @click="submit" :disabled="loading || !!pseudoErr" class="btn btn-primary w-full py-2.5 mt-2">
            <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ loading ? 'Création...' : 'Créer mon compte' }}
          </button>
        </div>
      </div>

      <p class="text-center text-sm text-gray-500 mt-5">
        Déjà un compte ?
        <router-link to="/login" class="text-brand-400 hover:text-brand-300 font-medium">Se connecter</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth    = useAuthStore()
const router  = useRouter()
const loading = ref(false)
const showPwd = ref(false)
const pseudoErr = ref('')
const form    = ref({ pseudo: '', email: '', password: '', nom: '', prenom: '' })

function checkPseudo() {
  const p = form.value.pseudo
  if (!p) { pseudoErr.value = ''; return }
  if (p.length < 3)    pseudoErr.value = 'Minimum 3 caractères'
  else if (!/^[a-zA-Z0-9_-]+$/.test(p)) pseudoErr.value = 'Caractères autorisés : lettres, chiffres, _ -'
  else pseudoErr.value = ''
}

const pwdStrength = computed(() => {
  const p = form.value.password
  if (!p) return 0
  let s = 0
  if (p.length >= 6) s++
  if (p.length >= 10) s++
  if (/[A-Z]/.test(p) && /[0-9]/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return s
})
const pwdColor = computed(() => {
  if (pwdStrength.value <= 1) return 'bg-red-500'
  if (pwdStrength.value === 2) return 'bg-orange-500'
  if (pwdStrength.value === 3) return 'bg-yellow-500'
  return 'bg-green-500'
})

async function submit() {
  if (!form.value.pseudo || !form.value.email || !form.value.password) return
  if (pseudoErr.value) return
  loading.value = true
  const ok = await auth.register(form.value)
  loading.value = false
  if (ok) router.push('/')
}
</script>
