<template>
  <div class="min-h-screen">
    <!-- Hero header -->
    <div class="border-b border-gray-800/60 px-6 py-6">
      <div class="max-w-6xl mx-auto flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 class="text-xl font-bold text-white">
            Mes tableaux
            <span class="text-brand-500 ml-1">({{ tableaux.length }})</span>
          </h1>
          <p class="text-gray-500 text-sm mt-0.5">Bonjour, <span class="text-gray-300">{{ auth.user?.pseudo }}</span> 👋</p>
        </div>
        <button @click="openCreate" class="btn btn-primary gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          Nouveau tableau
        </button>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-6 py-6">
      <!-- Loading -->
      <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="i in 3" :key="i" class="card p-5 h-36 animate-pulse">
          <div class="w-8 h-8 bg-gray-800 rounded-lg mb-3"></div>
          <div class="h-4 bg-gray-800 rounded w-3/4 mb-2"></div>
          <div class="h-3 bg-gray-800 rounded w-1/2"></div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="tableaux.length === 0" class="flex flex-col items-center justify-center py-24 text-center">
        <div class="w-20 h-20 bg-gray-800/50 rounded-2xl flex items-center justify-center text-4xl mb-5">📋</div>
        <h3 class="text-lg font-semibold text-white mb-2">Aucun tableau</h3>
        <p class="text-gray-500 text-sm mb-6 max-w-xs">Créez votre premier tableau Kanban pour commencer à organiser votre travail.</p>
        <button @click="openCreate" class="btn btn-primary">Créer un tableau</button>
      </div>

      <!-- Grid tableaux -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <router-link v-for="t in tableaux" :key="t.id"
          :to="`/board/${t.id}`"
          class="card-hover p-5 block group relative overflow-hidden">
          <!-- Color accent -->
          <div class="absolute top-0 left-0 right-0 h-0.5 rounded-t-xl transition-all"
            :style="`background: ${t.couleur}`"></div>

          <div class="flex items-start justify-between mb-4">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center text-white font-bold text-base shadow-lg"
              :style="`background: ${t.couleur}33; border: 1px solid ${t.couleur}66; color: ${t.couleur}`">
              {{ t.titre[0]?.toUpperCase() }}
            </div>
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button @click.prevent="openEdit(t)"
                class="btn-ghost btn-xs rounded-lg p-1.5 text-gray-500 hover:text-white">✎</button>
              <button @click.prevent="confirmDelete(t)"
                class="btn-ghost btn-xs rounded-lg p-1.5 text-gray-500 hover:text-red-400">✕</button>
            </div>
          </div>

          <h3 class="font-semibold text-white mb-1 line-clamp-1 group-hover:text-brand-300 transition-colors">
            {{ t.titre }}
          </h3>
          <p v-if="t.description" class="text-xs text-gray-500 line-clamp-2 mb-3">{{ t.description }}</p>

          <div class="flex items-center justify-between pt-3 border-t border-gray-800/60">
            <div class="flex -space-x-1">
              <div v-for="m in (t.membres ?? []).slice(0,4)" :key="m.id"
                class="w-5 h-5 rounded-full flex items-center justify-center text-white text-xs font-bold ring-1 ring-gray-900"
                :style="`background: #${Math.abs(hashCode(m.utilisateur?.pseudo ?? '')).toString(16).slice(0,6).padStart(6,'3')}`">
                {{ m.utilisateur?.pseudo?.[0]?.toUpperCase() }}
              </div>
            </div>
            <span class="text-xs text-gray-600">{{ formatDate(t.createdAt) }}</span>
          </div>
        </router-link>

        <!-- Bouton ajout rapide -->
        <button @click="openCreate"
          class="card border-dashed border-gray-700/60 hover:border-brand-600/60 hover:bg-brand-900/10 p-5 flex flex-col items-center justify-center gap-2 text-gray-600 hover:text-brand-400 transition-all min-h-[140px]">
          <div class="w-10 h-10 rounded-xl border-2 border-dashed border-current flex items-center justify-center text-xl">+</div>
          <span class="text-sm font-medium">Nouveau tableau</span>
        </button>
      </div>
    </div>

    <!-- ══ Modal créer/éditer tableau ══ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showModal" class="modal-bg" @click.self="closeModal">
          <div class="modal max-w-md">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-lg font-bold text-white">{{ editTarget ? 'Modifier le tableau' : 'Nouveau tableau' }}</h2>
              <button @click="closeModal" class="btn-ghost btn-sm rounded-lg p-1.5">✕</button>
            </div>

            <div class="space-y-4">
              <div v-if="formErr" class="alert-error">{{ formErr }}</div>

              <div>
                <label class="label">Titre <span class="text-red-400">*</span></label>
                <input v-model="form.titre" class="input" placeholder="Mon tableau Kanban" ref="titleInput" />
              </div>
              <div>
                <label class="label">Description</label>
                <textarea v-model="form.description" class="input h-20 resize-none"
                  placeholder="Description optionnelle..."></textarea>
              </div>
              <div>
                <label class="label">Couleur</label>
                <div class="flex gap-2 flex-wrap mt-1">
                  <button v-for="c in COLORS" :key="c"
                    @click="form.couleur = c"
                    class="w-8 h-8 rounded-lg border-2 transition-all hover:scale-110"
                    :style="`background: ${c}33; border-color: ${form.couleur === c ? c : 'transparent'}`">
                    <span v-if="form.couleur === c" class="text-sm" :style="`color:${c}`">✓</span>
                  </button>
                </div>
              </div>

              <div class="flex gap-2 pt-1">
                <button @click="submitForm" :disabled="saving" class="btn btn-primary flex-1">
                  {{ saving ? '...' : (editTarget ? 'Enregistrer' : 'Créer') }}
                </button>
                <button @click="closeModal" class="btn btn-secondary">Annuler</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { tableauxAPI } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth     = useAuthStore()
const tableaux = ref([])
const loading  = ref(true)
const showModal= ref(false)
const saving   = ref(false)
const formErr  = ref('')
const editTarget = ref(null)
const titleInput = ref(null)

const COLORS = ['#3b82f6','#8b5cf6','#ec4899','#10b981','#f59e0b','#ef4444','#6366f1','#14b8a6','#f97316']
const form   = ref({ titre: '', description: '', couleur: '#3b82f6' })

function hashCode(s) { return [...s].reduce((h,c) => (Math.imul(31,h)+c.charCodeAt(0))|0, 0) }
function formatDate(d) { if (!d) return ''; return new Date(d).toLocaleDateString('fr-FR') }

async function load() {
  const res = await tableauxAPI.getMes(auth.user.id)
  tableaux.value = res.data ?? []
  loading.value  = false
}

function openCreate() {
  editTarget.value = null
  form.value = { titre: '', description: '', couleur: '#3b82f6' }
  formErr.value = ''
  showModal.value = true
  nextTick(() => titleInput.value?.focus())
}

function openEdit(t) {
  editTarget.value = t
  form.value = { titre: t.titre, description: t.description ?? '', couleur: t.couleur ?? '#3b82f6' }
  formErr.value = ''
  showModal.value = true
  nextTick(() => titleInput.value?.focus())
}

function closeModal() { showModal.value = false; editTarget.value = null }

async function submitForm() {
  if (!form.value.titre.trim()) { formErr.value = 'Le titre est requis'; return }
  saving.value = true; formErr.value = ''
  const res = editTarget.value
    ? await tableauxAPI.update(editTarget.value.id, form.value)
    : await tableauxAPI.create(form.value)
  saving.value = false
  if (res.success !== false) { closeModal(); await load() }
  else formErr.value = res.message || 'Erreur'
}

async function confirmDelete(t) {
  if (!confirm(`Supprimer "${t.titre}" et toutes ses données ?`)) return
  await tableauxAPI.delete(t.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: all .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
