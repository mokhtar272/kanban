// URL du serveur Deno
const DENO = import.meta.env.VITE_API_URL ?? 'http://localhost:8000'

const getToken = () => localStorage.getItem('kanban_token') || ''

// ── HTTP helper ───────────────────────────────────────────────
async function req(method, path, body = null, params = null, extraHeaders = {}) {
  let url = DENO + path
  if (params) {
    const p = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== ''))
    )
    const qs = p.toString()
    if (qs) url += '?' + qs
  }
  const opts = {
    method,
    headers: {
      'Content-Type':  'application/json',
      'Authorization': `Bearer ${getToken()}`,
      ...extraHeaders
    }
  }
  if (body !== null) opts.body = JSON.stringify(body)
  try {
    const res  = await fetch(url, opts)
    const text = await res.text()
    if (!text) return { success: res.ok, data: null }
    try {
      const json = JSON.parse(text)
      // Normalize : si le serveur renvoie success:false, on enrichit le message
      if (!json.success && !json.message) json.message = `Erreur ${res.status}`
      return json
    } catch {
      return { success: false, message: `Réponse inattendue du serveur (${res.status})` }
    }
  } catch (e) {
    return { success: false, message: 'Impossible de contacter le serveur. Vérifiez votre connexion.' }
  }
}

// ── Auth ──────────────────────────────────────────────────────
export const authAPI = {
  register: (d) => req('POST', '/auth/register', d),
  login:    (d) => req('POST', '/auth/login',    d),
}

// ── Tableaux ──────────────────────────────────────────────────
export const tableauxAPI = {
  getMes:  (userId) => req('GET',    `/api/tableaux/mes/${userId}`),
  getById: (id)     => req('GET',    `/api/tableaux/${id}`),
  create:  (data)   => req('POST',   '/api/tableaux', data),
  update:  (id, d)  => req('PUT',    `/api/tableaux/${id}`, d),
  delete:  (id)     => req('DELETE', `/api/tableaux/${id}`),
  inviter: (id, d)  => req('POST',   `/api/tableaux/${id}/inviter`, d),
  retirerMembre: (id, uid) => req('DELETE', `/api/tableaux/${id}/membres/${uid}`),
}

// ── Colonnes ──────────────────────────────────────────────────
export const colonnesAPI = {
  getByTableau: (tId, boardId)       => req('GET',    `/api/colonnes/tableau/${tId}`, null, null, boardHdr(boardId)),
  create:       (tId, data, boardId) => req('POST',   '/api/colonnes', data, { tableauId: tId }, boardHdr(boardId)),
  update:       (id, data, boardId)  => req('PUT',    `/api/colonnes/${id}`, data, null, boardHdr(boardId)),
  delete:       (id, boardId)        => req('DELETE', `/api/colonnes/${id}`, null, null, boardHdr(boardId)),
}

// ── Tâches ────────────────────────────────────────────────────
export const tachesAPI = {
  getByColonne: (cId)            => req('GET',    `/api/taches/colonne/${cId}`),
  getById:      (id)             => req('GET',    `/api/taches/${id}`),
  create:       (data, boardId)  => req('POST',   '/api/taches', data, null, boardHdr(boardId)),
  update:       (id, data, boardId) => req('PUT', `/api/taches/${id}`, data, null, boardHdr(boardId)),
  delete:       (id, boardId)    => req('DELETE', `/api/taches/${id}`, null, null, boardHdr(boardId)),

  /** Recherche plein texte */
  search: (tId, q) =>
    req('GET', '/api/taches/search', null, { tableauId: tId, q }),

  /** Filtre par priorité et/ou assigné */
  filter: (tId, priorite, assigneId) =>
    req('GET', '/api/taches/filter', null, {
      tableauId: tId,
      priorite:  priorite  || undefined,
      assigneId: assigneId || undefined,
    }),

  historique: (tId) =>
    req('GET', `/api/taches/historique/${tId}`),

  deplacer: (id, colonneId, pos, boardId) =>
    req('PUT', `/api/taches/${id}`, { idColonne: colonneId, position: pos }, null, boardHdr(boardId)),
}

// ── Commentaires ──────────────────────────────────────────────
export const commentairesAPI = {
  getByTache: (tId)       => req('GET',    `/api/commentaires/tache/${tId}`),
  count:      (tId)       => req('GET',    `/api/commentaires/tache/${tId}/count`),
  /** body : { contenu, auteurPseudo, piecesJointes: [{nom, type, taille, data}] } */
  create:     (tId, data) => req('POST',   '/api/commentaires', data, { tacheId: tId }),
  delete:     (id)        => req('DELETE', `/api/commentaires/${id}`),
}

// ── Admin ─────────────────────────────────────────────────────
export const adminAPI = {
  stats:        ()   => req('GET',    '/api/admin/stats'),
  utilisateurs: ()   => req('GET',    '/api/admin/utilisateurs'),
  deleteUser:   (id) => req('DELETE', `/api/admin/utilisateurs/${id}`),
}

// ── WebSocket ─────────────────────────────────────────────────
const WS_BASE = DENO.replace(/^http/, 'ws')

export function createBoardWS(boardId, onUpdate) {
  const token = getToken()
  if (!token) return null
  const ws = new WebSocket(`${WS_BASE}/ws/board/${boardId}?token=${token}`)

  let pingInterval = null

  ws.onopen = () => {
    // Ping toutes les 25s pour garder la connexion active
    pingInterval = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN)
        ws.send(JSON.stringify({ type: 'PING' }))
    }, 25_000)
  }

  ws.onmessage = (evt) => {
    try {
      const msg = JSON.parse(evt.data)
      if (msg.type === 'BOARD_UPDATED') onUpdate(msg)
    } catch { /* ignore */ }
  }

  ws.onclose  = () => { if (pingInterval) clearInterval(pingInterval) }
  ws.onerror  = () => { if (pingInterval) clearInterval(pingInterval) }

  return ws
}

// ── Helpers ───────────────────────────────────────────────────
function boardHdr(boardId) {
  return {}
}

export const prioriteConfig = {
  BASSE:   { label: 'Basse',   class: 'badge-gray',   icon: '▽' },
  NORMALE: { label: 'Normale', class: 'badge-blue',   icon: '◇' },
  HAUTE:   { label: 'Haute',   class: 'badge-orange', icon: '△' },
  URGENTE: { label: 'Urgente', class: 'badge-red',    icon: '▲' },
}
