// URL du serveur Deno :
//   - En développement local : http://localhost:8000  (défini dans .env)
//   - En production déployée : http://info-tpsi.univ-brest.fr:10011  (défini dans .env.production)
const DENO = import.meta.env.VITE_API_URL ?? 'http://localhost:8000'

const getToken = () => localStorage.getItem('kanban_token') || ''

async function req(method, path, body = null, params = null) {
  let url = DENO + path
  if (params) {
    const p = new URLSearchParams(params)
    url += '?' + p.toString()
  }
  const opts = {
    method,
    headers: {
      'Content-Type':  'application/json',
      'Authorization': `Bearer ${getToken()}`
    }
  }
  if (body !== null) opts.body = JSON.stringify(body)
  try {
    const res  = await fetch(url, opts)
    const text = await res.text()
    if (!text) return { success: res.ok, data: null }
    const json = JSON.parse(text)
    return json
  } catch (e) {
    return { success: false, message: 'Erreur réseau : ' + e.message }
  }
}

// ─── Auth ─────────────────────────────────────────────────────
export const authAPI = {
  register: (d) => req('POST', '/auth/register', d),
  login:    (d) => req('POST', '/auth/login',    d),
}

// ─── Tableaux ─────────────────────────────────────────────────
export const tableauxAPI = {
  getMes:  (userId) => req('GET',    `/api/tableaux/mes/${userId}`),
  getById: (id)     => req('GET',    `/api/tableaux/${id}`),
  create:  (data)   => req('POST',   '/api/tableaux', data),
  update:  (id, d)  => req('PUT',    `/api/tableaux/${id}`, d),
  delete:  (id)     => req('DELETE', `/api/tableaux/${id}`),
  inviter: (id, d)  => req('POST',   `/api/tableaux/${id}/inviter`, d),
  retirerMembre: (id, uid) => req('DELETE', `/api/tableaux/${id}/membres/${uid}`),
}

// ─── Colonnes ─────────────────────────────────────────────────
export const colonnesAPI = {
  getByTableau: (tId)       => req('GET',    `/api/colonnes/tableau/${tId}`),
  create:       (tId, data) => req('POST',   '/api/colonnes', data, { tableauId: tId }),
  update:       (id, data)  => req('PUT',    `/api/colonnes/${id}`, data),
  delete:       (id)        => req('DELETE', `/api/colonnes/${id}`),
}

// ─── Tâches ───────────────────────────────────────────────────
export const tachesAPI = {
  getByColonne: (cId)      => req('GET',    `/api/taches/colonne/${cId}`),
  getById:      (id)       => req('GET',    `/api/taches/${id}`),
  create:       (data)     => req('POST',   '/api/taches', data),
  update:       (id, data) => req('PUT',    `/api/taches/${id}`, data),
  delete:       (id)       => req('DELETE', `/api/taches/${id}`),
  search:       (tId, q)   => req('GET',    '/api/taches/search', null, { tableauId: tId, q }),
  historique:   (tId)      => req('GET',    `/api/taches/historique/${tId}`),
  deplacer:     (id, colonneId, pos) =>
    req('PUT', `/api/taches/${id}`, { idColonne: colonneId, position: pos }),
}

// ─── Commentaires ─────────────────────────────────────────────
export const commentairesAPI = {
  getByTache: (tId)         => req('GET',    `/api/commentaires/tache/${tId}`),
  count:      (tId)         => req('GET',    `/api/commentaires/tache/${tId}/count`),
  create:     (tId, data)   => req('POST',   '/api/commentaires', data, { tacheId: tId }),
  delete:     (id)          => req('DELETE', `/api/commentaires/${id}`),
}

// ─── Admin ────────────────────────────────────────────────────
export const adminAPI = {
  stats:        ()   => req('GET',    '/api/admin/stats'),
  utilisateurs: ()   => req('GET',    '/api/admin/utilisateurs'),
  deleteUser:   (id) => req('DELETE', `/api/admin/utilisateurs/${id}`),
}

// ─── Helpers couleur pour priorité ───────────────────────────
export const prioriteConfig = {
  BASSE:   { label: 'Basse',   class: 'badge-gray',   icon: '▽' },
  NORMALE: { label: 'Normale', class: 'badge-blue',   icon: '◇' },
  HAUTE:   { label: 'Haute',   class: 'badge-orange', icon: '△' },
  URGENTE: { label: 'Urgente', class: 'badge-red',    icon: '▲' },
}
