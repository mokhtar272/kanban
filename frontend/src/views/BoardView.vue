<template>
  <div class="h-screen flex flex-col bg-[#0f1117] overflow-hidden">

    <!-- ── Board Header ─────────────────────────────────────── -->
    <div class="h-12 shrink-0 border-b border-gray-800/60 px-4 flex items-center gap-3">
      <!-- Titre + couleur -->
      <div v-if="tableau" class="flex items-center gap-2">
        <div class="w-3 h-3 rounded-full" :style="`background: ${tableau.couleur}`"></div>
        <span class="font-semibold text-white text-sm">{{ tableau.titre }}</span>
        <span class="text-gray-600 text-xs">({{ membres.length }} membre{{ membres.length > 1 ? 's' : '' }})</span>
      </div>

      <div class="flex-1"></div>

      <!-- Barre de recherche -->
      <div class="relative">
        <input v-model="searchQ" @input="doSearch"
          class="input-sm w-48 pl-7" placeholder="Rechercher..." />
        <span class="absolute left-2 top-1/2 -translate-y-1/2 text-gray-500 text-xs">🔍</span>
        <div v-if="searchQ && searchResults.length > 0"
          class="absolute top-full mt-1 left-0 right-0 bg-[#161b27] border border-gray-700 rounded-lg shadow-xl z-20 py-1 max-h-48 overflow-y-auto">
          <button v-for="r in searchResults" :key="r.id"
            @click="openTaskModal(r); searchQ = ''; searchResults = []"
            class="w-full text-left px-3 py-2 text-xs text-gray-300 hover:bg-gray-800 hover:text-white transition-colors truncate">
            {{ r.titre }}
          </button>
        </div>
      </div>

      <!-- Membres avatars -->
      <div class="flex -space-x-1.5">
        <div v-for="m in membres.slice(0,5)" :key="m.id"
          :title="m.utilisateur?.pseudo"
          class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold ring-2 ring-[#0f1117]"
          :style="`background: ${avatarColor(m.utilisateur?.pseudo)}`">
          {{ m.utilisateur?.pseudo?.[0]?.toUpperCase() }}
        </div>
      </div>

      <!-- Actions -->
      <button @click="showInvite = true" class="btn btn-secondary btn-sm text-xs">👤 Inviter</button>
      <button @click="showHistorique = true" class="btn btn-ghost btn-sm text-xs">📜</button>
      <button @click="showAddCol = true" class="btn btn-primary btn-sm text-xs gap-1">
        <span>+</span> Colonne
      </button>
    </div>

    <!-- Recherche — résultats actifs -->
    <div v-if="searchQ && searchResults.length === 0 && searchDone"
      class="px-4 py-1.5 bg-yellow-900/20 border-b border-yellow-800/30 text-xs text-yellow-400">
      Aucune tâche trouvée pour "{{ searchQ }}"
    </div>

    <!-- ── Colonnes Kanban ───────────────────────────────────── -->
    <div v-if="!boardLoading" class="flex-1 overflow-x-auto overflow-y-hidden">
      <div class="flex gap-3 h-full p-4 items-start" style="min-width: max-content">

        <!-- Colonne -->
        <div v-for="col in colonnes" :key="col.id"
          class="flex flex-col w-72 shrink-0 max-h-full"
          style="max-height: calc(100vh - 7rem)">

          <!-- Header colonne -->
          <div class="flex items-center justify-between px-3 py-2.5 rounded-t-xl bg-gray-900/60 border border-gray-800/60 border-b-0">
            <div class="flex items-center gap-2">
              <div class="w-2.5 h-2.5 rounded-full" :style="`background: ${col.couleur}`"></div>
              <span class="text-sm font-semibold text-gray-200">{{ col.titre }}</span>
              <span class="badge-gray text-xs px-1.5 py-0">{{ col.taches.length }}</span>
            </div>
            <div class="flex items-center gap-0.5">
              <button @click="openEditCol(col)" class="btn-ghost btn-xs p-1 rounded text-gray-600 hover:text-gray-300">✎</button>
              <button @click="deleteColonne(col)" class="btn-ghost btn-xs p-1 rounded text-gray-600 hover:text-red-400">✕</button>
            </div>
          </div>

          <!-- Zone tâches (droppable) -->
          <div class="flex-1 overflow-y-auto px-2 pt-1 pb-2 rounded-b-xl bg-gray-900/30 border border-gray-800/60 border-t-0 space-y-2"
            @dragover.prevent="dragOverCol = col.id"
            @dragleave="dragOverCol = null"
            @drop="onDrop($event, col.id)"
            :class="dragOverCol === col.id ? 'bg-brand-900/20 border-brand-700/40' : ''">

            <!-- Tâche card -->
            <div v-for="tache in col.taches" :key="tache.id"
              class="group relative bg-[#161b27] border rounded-lg p-3 cursor-pointer transition-all select-none"
              :class="[
                dragTacheId === tache.id ? 'opacity-40 scale-95 border-brand-600' : 'border-gray-800/70 hover:border-gray-700',
                'hover:shadow-lg hover:shadow-black/20'
              ]"
              draggable="true"
              @dragstart="onDragStart($event, tache, col.id)"
              @dragend="dragTacheId = null"
              @click="openTaskModal(tache)">

              <!-- Priorité -->
              <div class="flex items-center justify-between mb-2">
                <span :class="prioriteConfig[tache.priorite]?.class ?? 'badge-gray'" class="badge text-xs">
                  {{ prioriteConfig[tache.priorite]?.icon }} {{ prioriteConfig[tache.priorite]?.label }}
                </span>
                <button @click.stop="deleteTask(tache)"
                  class="opacity-0 group-hover:opacity-100 btn-ghost btn-xs p-0.5 rounded text-gray-600 hover:text-red-400 transition-opacity">✕</button>
              </div>

              <p class="text-sm font-medium text-white mb-1.5 line-clamp-2">{{ tache.titre }}</p>
              <p v-if="tache.description" class="text-xs text-gray-500 line-clamp-2 mb-2">{{ tache.description }}</p>

              <!-- Footer tâche -->
              <div class="flex items-center justify-between mt-1 pt-2 border-t border-gray-800/50">
                <div class="flex items-center gap-1.5">
                  <span v-if="tache.dateLimite" class="text-xs"
                    :class="isOverdue(tache.dateLimite) ? 'text-red-400' : 'text-gray-600'">
                    📅 {{ formatDate(tache.dateLimite) }}
                  </span>
                </div>
                <div class="flex items-center gap-2">
                  <span v-if="commentCounts[tache.id]" class="text-xs text-gray-600">
                    💬 {{ commentCounts[tache.id] }}
                  </span>
                  <div v-if="tache.assigne"
                    class="w-5 h-5 rounded-full flex items-center justify-center text-white text-xs font-bold"
                    :style="`background: ${avatarColor(tache.assigne.pseudo)}`"
                    :title="tache.assigne.pseudo">
                    {{ tache.assigne.pseudo?.[0]?.toUpperCase() }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty drop zone -->
            <div v-if="col.taches.length === 0 && dragTacheId"
              class="h-16 rounded-lg border-2 border-dashed border-gray-700/50 flex items-center justify-center text-xs text-gray-600">
              Déposer ici
            </div>
          </div>

          <!-- Ajouter tâche -->
          <div class="mt-1.5">
            <div v-if="addingToCol === col.id" class="bg-[#161b27] border border-gray-700 rounded-lg p-2 space-y-2">
              <input v-model="quickTitle" class="input-sm w-full"
                placeholder="Titre de la tâche..."
                @keyup.enter="quickAddTask(col.id)"
                @keyup.escape="addingToCol = null"
                ref="quickInput" />
              <div class="flex gap-1">
                <button @click="quickAddTask(col.id)" class="btn btn-primary btn-xs flex-1">Ajouter</button>
                <button @click="addingToCol = null"    class="btn btn-secondary btn-xs">✕</button>
              </div>
            </div>
            <button v-else
              @click="startQuickAdd(col.id)"
              class="w-full flex items-center gap-1.5 px-3 py-2 text-xs text-gray-600 hover:text-gray-400 hover:bg-gray-800/40 rounded-lg transition-all">
              <span class="text-base leading-none">+</span> Ajouter une tâche
            </button>
          </div>
        </div>

        <!-- Ajouter colonne -->
        <div class="w-64 shrink-0">
          <Transition name="slide-up">
            <div v-if="showAddCol" class="bg-[#161b27] border border-gray-700 rounded-xl p-3 space-y-2">
              <input v-model="newColTitre" class="input-sm w-full" placeholder="Nom de la colonne"
                @keyup.enter="createColonne" @keyup.escape="showAddCol = false" autofocus />
              <div class="flex gap-2 flex-wrap">
                <button v-for="c in COL_COLORS" :key="c" @click="newColCouleur = c"
                  class="w-5 h-5 rounded-full border-2 transition-all"
                  :style="`background:${c}; border-color: ${newColCouleur===c ? 'white':'transparent'}`"></button>
              </div>
              <div class="flex gap-1">
                <button @click="createColonne" class="btn btn-primary btn-xs flex-1">Créer</button>
                <button @click="showAddCol = false" class="btn btn-secondary btn-xs">✕</button>
              </div>
            </div>
            <button v-else @click="showAddCol = true"
              class="w-full flex items-center justify-center gap-2 py-3 rounded-xl border-2 border-dashed border-gray-700/50 text-gray-600 hover:text-brand-400 hover:border-brand-700/50 text-sm transition-all">
              + Ajouter une colonne
            </button>
          </Transition>
        </div>
      </div>
    </div>
    <div v-else class="flex-1 flex items-center justify-center">
      <div class="flex flex-col items-center gap-3 text-gray-500">
        <svg class="w-8 h-8 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
        <span class="text-sm">Chargement du tableau...</span>
      </div>
    </div>

    <!-- ══ Modal Tâche ══════════════════════════════════════ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="taskModal.show" class="modal-bg" @click.self="closeTaskModal">
          <div class="modal max-w-lg">
            <!-- Header -->
            <div class="flex items-start justify-between mb-4">
              <div class="flex-1 pr-4">
                <input v-if="taskModal.editing" v-model="taskModal.form.titre"
                  class="input text-base font-bold" @blur="saveTaskField('titre')" />
                <h2 v-else @click="taskModal.editing = true"
                  class="text-lg font-bold text-white cursor-text hover:text-brand-300 transition-colors">
                  {{ taskModal.tache?.titre }}
                </h2>
                <p class="text-xs text-gray-500 mt-0.5">
                  dans <span class="text-gray-300">{{ getColonneTitre(taskModal.tache?.colonne?.id) }}</span>
                </p>
              </div>
              <button @click="closeTaskModal" class="btn-ghost btn-sm p-1.5 rounded-lg shrink-0">✕</button>
            </div>

            <div class="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
              <!-- Métadonnées -->
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="label">Priorité</label>
                  <select v-model="taskModal.form.priorite" @change="saveTaskField('priorite')" class="select text-xs">
                    <option v-for="(v,k) in prioriteConfig" :key="k" :value="k">{{ v.icon }} {{ v.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">Colonne</label>
                  <select v-model="taskModal.form.idColonne" @change="saveTaskField('idColonne')" class="select text-xs">
                    <option v-for="c in colonnes" :key="c.id" :value="c.id">{{ c.titre }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">Assigné à</label>
                  <select v-model="taskModal.form.idAssigne" @change="saveTaskField('idAssigne')" class="select text-xs">
                    <option value="0">— Personne —</option>
                    <option v-for="m in membres" :key="m.utilisateur?.id" :value="m.utilisateur?.id">
                      {{ m.utilisateur?.pseudo }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="label">Date limite</label>
                  <input v-model="taskModal.form.dateLimite" type="date" class="input text-xs"
                    @change="saveTaskField('dateLimite')" />
                </div>
              </div>

              <!-- Description -->
              <div>
                <label class="label">Description</label>
                <textarea v-model="taskModal.form.description"
                  class="input h-20 resize-none text-sm"
                  placeholder="Ajouter une description..."
                  @blur="saveTaskField('description')"></textarea>
              </div>

              <div class="divider"></div>

              <!-- Commentaires -->
              <div>
                <label class="label">💬 Commentaires ({{ commentaires.length }})</label>
                <div class="space-y-2 max-h-36 overflow-y-auto mb-3">
                  <div v-if="commentaires.length === 0" class="text-xs text-gray-600 text-center py-3">
                    Aucun commentaire
                  </div>
                  <div v-for="c in commentaires" :key="c.id"
                    class="bg-gray-900/50 rounded-lg p-3 border border-gray-800/50">
                    <div class="flex items-center justify-between mb-1.5">
                      <div class="flex items-center gap-1.5">
                        <div class="w-5 h-5 rounded-full flex items-center justify-center text-white text-xs font-bold"
                          :style="`background: ${avatarColor(c.auteurPseudo)}`">
                          {{ c.auteurPseudo?.[0]?.toUpperCase() }}
                        </div>
                        <span class="text-xs font-medium text-gray-300">{{ c.auteurPseudo }}</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-gray-600">{{ formatDateTime(c.createdAt) }}</span>
                        <button v-if="c.auteurPseudo === auth.user?.pseudo || auth.isAdmin"
                          @click="deleteComment(c.id)"
                          class="text-gray-600 hover:text-red-400 text-xs transition-colors">✕</button>
                      </div>
                    </div>
                    <p class="text-sm text-gray-300 whitespace-pre-wrap">{{ c.contenu }}</p>
                  </div>
                </div>

                <!-- Nouveau commentaire -->
                <div class="flex gap-2">
                  <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0"
                    :style="`background: ${avatarColor(auth.user?.pseudo)}`">
                    {{ auth.user?.pseudo?.[0]?.toUpperCase() }}
                  </div>
                  <div class="flex-1">
                    <textarea v-model="newComment" rows="2"
                      class="input text-sm resize-none"
                      placeholder="Laisser un commentaire..."
                      @keydown.ctrl.enter="addComment"
                      @keydown.meta.enter="addComment"></textarea>
                    <div class="flex justify-between items-center mt-1.5">
                      <span class="text-xs text-gray-600">Ctrl+Entrée pour envoyer</span>
                      <button @click="addComment" :disabled="!newComment.trim()"
                        class="btn btn-primary btn-xs">Commenter</button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="divider"></div>
              <!-- Danger zone -->
              <div class="flex justify-end">
                <button @click="deleteTaskFromModal"
                  class="btn btn-danger btn-sm text-xs">🗑 Supprimer cette tâche</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ══ Modal Inviter ══════════════════════════════════ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showInvite" class="modal-bg" @click.self="showInvite = false">
          <div class="modal max-w-sm">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-bold text-white">👤 Inviter un membre</h3>
              <button @click="showInvite = false" class="btn-ghost btn-sm p-1.5 rounded-lg">✕</button>
            </div>

            <!-- Membres actuels -->
            <div class="mb-4">
              <p class="label">Membres actuels</p>
              <div class="space-y-2">
                <div v-for="m in membres" :key="m.id"
                  class="flex items-center justify-between py-1.5 px-2 rounded-lg hover:bg-gray-800/40">
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold"
                      :style="`background: ${avatarColor(m.utilisateur?.pseudo)}`">
                      {{ m.utilisateur?.pseudo?.[0]?.toUpperCase() }}
                    </div>
                    <span class="text-sm text-white">{{ m.utilisateur?.pseudo }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="badge-blue text-xs">{{ m.role }}</span>
                    <button v-if="m.role !== 'OWNER' && m.utilisateur?.id !== auth.user?.id"
                      @click="retirerMembre(m)"
                      class="text-xs text-gray-600 hover:text-red-400">✕</button>
                  </div>
                </div>
              </div>
            </div>

            <div class="divider mb-4"></div>

            <div class="space-y-3">
              <div v-if="inviteMsg" :class="inviteOk ? 'alert-success' : 'alert-error'">{{ inviteMsg }}</div>
              <div>
                <label class="label">Pseudo</label>
                <input v-model="invitePseudo" class="input" placeholder="pseudo_utilisateur"
                  @keyup.enter="inviterMembre" />
              </div>
              <div>
                <label class="label">Rôle</label>
                <select v-model="inviteRole" class="select">
                  <option value="EDITOR">Éditeur (peut modifier)</option>
                  <option value="VIEWER">Lecteur (consultation uniquement)</option>
                </select>
              </div>
              <div class="flex gap-2">
                <button @click="inviterMembre" class="btn btn-primary flex-1">Inviter</button>
                <button @click="showInvite = false" class="btn btn-secondary">Fermer</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ══ Modal Historique ═════════════════════════════════ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showHistorique" class="modal-bg" @click.self="showHistorique = false">
          <div class="modal max-w-md">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-bold text-white">📜 Historique</h3>
              <button @click="showHistorique = false" class="btn-ghost btn-sm p-1.5 rounded-lg">✕</button>
            </div>
            <div class="space-y-2 max-h-80 overflow-y-auto">
              <div v-if="historique.length === 0" class="text-center py-8 text-gray-500 text-sm">
                Aucune action enregistrée
              </div>
              <div v-for="h in historique" :key="h.id"
                class="flex gap-3 py-2 border-b border-gray-800/50">
                <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0"
                  :style="`background: ${avatarColor(h.user?.pseudo)}`">
                  {{ h.user?.pseudo?.[0]?.toUpperCase() ?? '?' }}
                </div>
                <div class="flex-1">
                  <p class="text-xs text-gray-300">{{ h.description }}</p>
                  <div class="flex items-center gap-2 mt-0.5">
                    <span class="text-xs text-gray-600">{{ h.user?.pseudo }}</span>
                    <span class="text-gray-700">·</span>
                    <span class="text-xs text-gray-600">{{ formatDateTime(h.createdAt) }}</span>
                  </div>
                </div>
                <span class="badge-gray text-xs shrink-0">{{ h.action }}</span>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ══ Modal Édition colonne ════════════════════════════ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="editColModal.show" class="modal-bg" @click.self="editColModal.show = false">
          <div class="modal max-w-xs">
            <h3 class="font-bold text-white mb-4">Modifier la colonne</h3>
            <div class="space-y-3">
              <div>
                <label class="label">Titre</label>
                <input v-model="editColModal.titre" class="input" @keyup.enter="saveColonne" />
              </div>
              <div>
                <label class="label">Couleur</label>
                <div class="flex gap-2 flex-wrap">
                  <button v-for="c in COL_COLORS" :key="c" @click="editColModal.couleur = c"
                    class="w-6 h-6 rounded-full border-2 transition-all"
                    :style="`background:${c}; border-color:${editColModal.couleur===c?'white':'transparent'}`"></button>
                </div>
              </div>
              <div class="flex gap-2 pt-1">
                <button @click="saveColonne" class="btn btn-primary flex-1">Enregistrer</button>
                <button @click="editColModal.show = false" class="btn btn-secondary">Annuler</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, reactive, nextTick, onMounted, provide } from 'vue'
import { useRoute } from 'vue-router'
import { tableauxAPI, colonnesAPI, tachesAPI, commentairesAPI, prioriteConfig } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth  = useAuthStore()
const boardId = computed(() => parseInt(route.params.id))

// ── State ─────────────────────────────────────────────────────
const tableau       = ref(null)
const colonnes      = ref([])
const membres       = ref([])
const boardLoading  = ref(true)
const commentaires  = ref([])
const commentCounts = ref({})
const historique    = ref([])
const newComment    = ref('')

// Drag & Drop
const dragTacheId   = ref(null)
const dragFromColId = ref(null)
const dragOverCol   = ref(null)

// Quick add
const addingToCol = ref(null)
const quickTitle  = ref('')
const quickInput  = ref(null)

// Colonnes
const showAddCol    = ref(false)
const newColTitre   = ref('')
const newColCouleur = ref('#6b7280')

// Modals
const showInvite     = ref(false)
const showHistorique = ref(false)
const invitePseudo   = ref('')
const inviteRole     = ref('EDITOR')
const inviteMsg      = ref('')
const inviteOk       = ref(false)

// Task modal
const taskModal = reactive({
  show: false, tache: null, editing: false,
  form: { titre:'', description:'', priorite:'NORMALE', idColonne:null, idAssigne:0, dateLimite:'' }
})

// Edit colonne modal
const editColModal = reactive({ show:false, id:null, titre:'', couleur:'' })

// Search
const searchQ       = ref('')
const searchResults = ref([])
const searchDone    = ref(false)

// Col colors
const COL_COLORS = ['#6b7280','#3b82f6','#8b5cf6','#ec4899','#10b981','#f59e0b','#ef4444']

// Provide board title to NavBar
const boardTitle = ref('')
provide('boardTitle', boardTitle)

// ── Helpers ───────────────────────────────────────────────────
const AVATAR_COLORS = ['#3b82f6','#8b5cf6','#ec4899','#10b981','#f59e0b','#6366f1','#14b8a6']
function avatarColor(s = '') { return AVATAR_COLORS[s.charCodeAt(0) % AVATAR_COLORS.length] }
function formatDate(d)     { if (!d) return ''; return new Date(d).toLocaleDateString('fr-FR') }
function formatDateTime(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('fr-FR', { day:'2-digit', month:'short', hour:'2-digit', minute:'2-digit' })
}
function isOverdue(d) { return d && new Date(d) < new Date() }
function getColonneTitre(colId) { return colonnes.value.find(c => c.id === colId)?.titre ?? '' }

// ── Load ─────────────────────────────────────────────────────
async function loadBoard() {
  const [tabRes, colRes] = await Promise.all([
    tableauxAPI.getById(boardId.value),
    colonnesAPI.getByTableau(boardId.value),
  ])
  tableau.value  = tabRes.data
  boardTitle.value = tabRes.data?.titre ?? ''
  membres.value  = tabRes.data?.membres ?? []

  const cols = colRes.data ?? []
  // Charger tâches pour chaque colonne en parallèle
  await Promise.all(cols.map(async c => {
    const r = await tachesAPI.getByColonne(c.id)
    c.taches = r.data ?? []
    // Compter commentaires
    await Promise.all(c.taches.map(async t => {
      const cr = await commentairesAPI.count(t.id)
      if (cr.data) commentCounts.value[t.id] = cr.data
    }))
  }))
  colonnes.value  = cols
  boardLoading.value = false
}

// ── Colonnes ─────────────────────────────────────────────────
async function createColonne() {
  if (!newColTitre.value.trim()) return
  await colonnesAPI.create(boardId.value, { titre: newColTitre.value, couleur: newColCouleur.value })
  newColTitre.value = ''; showAddCol.value = false
  await loadBoard()
}

function openEditCol(col) {
  editColModal.show = true; editColModal.id = col.id
  editColModal.titre = col.titre; editColModal.couleur = col.couleur
}

async function saveColonne() {
  await colonnesAPI.update(editColModal.id, { titre: editColModal.titre, couleur: editColModal.couleur })
  editColModal.show = false
  await loadBoard()
}

async function deleteColonne(col) {
  if (!confirm(`Supprimer "${col.titre}" et toutes ses tâches ?`)) return
  await colonnesAPI.delete(col.id)
  await loadBoard()
}

// ── Tâches ────────────────────────────────────────────────────
function startQuickAdd(colId) {
  addingToCol.value = colId; quickTitle.value = ''
  nextTick(() => quickInput.value?.focus())
}

async function quickAddTask(colId) {
  if (!quickTitle.value.trim()) return
  await tachesAPI.create({ titre: quickTitle.value, idColonne: colId })
  addingToCol.value = null; quickTitle.value = ''
  await loadBoard()
}

async function deleteTask(tache) {
  if (!confirm(`Supprimer "${tache.titre}" ?`)) return
  await tachesAPI.delete(tache.id)
  await loadBoard()
}

async function deleteTaskFromModal() {
  if (!confirm(`Supprimer "${taskModal.tache?.titre}" ?`)) return
  await tachesAPI.delete(taskModal.tache.id)
  closeTaskModal()
  await loadBoard()
}

// ── Drag & Drop ───────────────────────────────────────────────
function onDragStart(e, tache, colId) {
  dragTacheId.value   = tache.id
  dragFromColId.value = colId
  e.dataTransfer.effectAllowed = 'move'
}

async function onDrop(e, toColId) {
  e.preventDefault()
  dragOverCol.value = null
  if (!dragTacheId.value || dragFromColId.value === toColId) { dragTacheId.value = null; return }
  const col   = colonnes.value.find(c => c.id === toColId)
  const newPos = col ? col.taches.length : 0
  await tachesAPI.deplacer(dragTacheId.value, toColId, newPos)
  dragTacheId.value = null
  await loadBoard()
}

// ── Task modal ────────────────────────────────────────────────
async function openTaskModal(tache) {
  taskModal.tache   = tache
  taskModal.show    = true
  taskModal.editing = false
  taskModal.form    = {
    titre:       tache.titre,
    description: tache.description ?? '',
    priorite:    tache.priorite,
    idColonne:   tache.colonne?.id,
    idAssigne:   tache.assigne?.id ?? 0,
    dateLimite:  tache.dateLimite ?? '',
  }
  const res = await commentairesAPI.getByTache(tache.id)
  commentaires.value = res.data ?? []
}

function closeTaskModal() { taskModal.show = false; taskModal.tache = null; commentaires.value = [] }

async function saveTaskField(field) {
  if (!taskModal.tache) return
  await tachesAPI.update(taskModal.tache.id, { [field]: taskModal.form[field] })
  await loadBoard()
  // Mettre à jour la tâche dans le modal
  const updated = colonnes.value.flatMap(c => c.taches).find(t => t.id === taskModal.tache.id)
  if (updated) taskModal.tache = updated
}

// ── Commentaires ──────────────────────────────────────────────
async function addComment() {
  if (!newComment.value.trim() || !taskModal.tache) return
  await commentairesAPI.create(taskModal.tache.id, {
    contenu: newComment.value.trim(), auteurPseudo: auth.user?.pseudo
  })
  newComment.value = ''
  const res = await commentairesAPI.getByTache(taskModal.tache.id)
  commentaires.value = res.data ?? []
  commentCounts.value[taskModal.tache.id] = commentaires.value.length
}

async function deleteComment(id) {
  await commentairesAPI.delete(id)
  const res = await commentairesAPI.getByTache(taskModal.tache.id)
  commentaires.value = res.data ?? []
  commentCounts.value[taskModal.tache.id] = commentaires.value.length
}

// ── Inviter ───────────────────────────────────────────────────
async function inviterMembre() {
  if (!invitePseudo.value.trim()) return
  const res = await tableauxAPI.inviter(boardId.value, { pseudo: invitePseudo.value, role: inviteRole.value })
  inviteOk.value  = res.success !== false
  inviteMsg.value = res.message || (inviteOk.value ? '✅ Invitation envoyée !' : 'Erreur')
  if (inviteOk.value) { invitePseudo.value = ''; await loadBoard() }
}

async function retirerMembre(m) {
  if (!confirm(`Retirer ${m.utilisateur?.pseudo} ?`)) return
  await tableauxAPI.retirerMembre(boardId.value, m.utilisateur?.id)
  await loadBoard()
}

// ── Historique ────────────────────────────────────────────────
async function openHistorique() {
  showHistorique.value = true
  if (historique.value.length === 0) {
    const res = await tachesAPI.historique(boardId.value)
    historique.value = res.data ?? []
  }
}

// ── Recherche ─────────────────────────────────────────────────
let searchTimer = null
async function doSearch() {
  clearTimeout(searchTimer)
  if (!searchQ.value.trim()) { searchResults.value = []; searchDone.value = false; return }
  searchTimer = setTimeout(async () => {
    const res = await tachesAPI.search(boardId.value, searchQ.value)
    searchResults.value = res.data ?? []
    searchDone.value = true
  }, 300)
}

onMounted(async () => {
  await loadBoard()
  // Charger l'historique en arrière-plan
  tachesAPI.historique(boardId.value).then(r => historique.value = r.data ?? [])
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: all .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-up-enter-active, .slide-up-leave-active { transition: all .15s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
