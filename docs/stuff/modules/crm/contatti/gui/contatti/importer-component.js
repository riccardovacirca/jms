import { LitElement, html } from 'lit';

/**
 * <importer-wizard listaId="123">
 * Wizard 4 step: upload → mappa → valida → esegui
 * Emette evento 'done' al completamento o 'cancel' per tornare indietro.
 */
class ImporterWizard extends LitElement {

  static properties = {
    listaId:     {},
    _step:       { state: true },
    _loading:    { state: true },
    _error:      { state: true },
    _session:    { state: true },
    _campi:      { state: true },
    _mapping:    { state: true },
    _validation: { state: true },
    _result:     { state: true },
    _consenso:   { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.listaId     = null;
    this._step       = 1;
    this._loading    = false;
    this._error      = null;
    this._session    = null;
    this._campi      = [];
    this._mapping    = {};
    this._validation = null;
    this._result     = null;
    this._consenso   = false;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadCampi();
  }

  async _loadCampi() {
    try {
      const res  = await fetch('/api/import/campi');
      const data = await res.json();
      if (!data.err) this._campi = data.out;
    } catch (e) { /* silent */ }
  }

  // ── Step 1: upload file ────────────────────────────────────────────────────

  async _onFileChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;

    this._loading = true;
    this._error   = null;

    const form = new FormData();
    form.append('file', file);

    try {
      const res  = await fetch('/api/import/analyze', { method: 'POST', body: form });
      const data = await res.json();
      if (data.err) {
        this._error = data.log;
      } else {
        this._session = data.out;
        this._mapping = {};
        this._step    = 2;
      }
    } catch (e) {
      this._error = 'Errore di rete';
    } finally {
      this._loading = false;
    }
  }

  // ── Step 2: mappatura colonne ──────────────────────────────────────────────

  _setMapping(colFile, campoSys) {
    this._mapping = { ...this._mapping, [colFile]: campoSys };
  }

  async _saveMapping() {
    const mapped = Object.fromEntries(
      Object.entries(this._mapping).filter(([, v]) => v && v !== '')
    );
    if (Object.keys(mapped).length === 0) {
      this._error = 'Mappa almeno una colonna';
      return;
    }
    this._loading = true;
    this._error   = null;
    try {
      const res  = await fetch(`/api/import/${this._session.sessionId}/mapping`, {
        method:  'PUT',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ mapping: mapped })
      });
      const data = await res.json();
      if (data.err) {
        this._error = data.log;
      } else {
        await this._runValidation();
      }
    } catch (e) {
      this._error = 'Errore di rete';
    } finally {
      this._loading = false;
    }
  }

  // ── Step 3: validazione ────────────────────────────────────────────────────

  async _runValidation() {
    this._loading = true;
    this._error   = null;
    try {
      const res  = await fetch(`/api/import/${this._session.sessionId}/validate`);
      const data = await res.json();
      if (data.err) {
        this._error = data.log;
      } else {
        this._validation = data.out;
        this._step       = 3;
      }
    } catch (e) {
      this._error = 'Errore di rete';
    } finally {
      this._loading = false;
    }
  }

  // ── Step 4: esecuzione ────────────────────────────────────────────────────

  async _execute() {
    this._loading = true;
    this._error   = null;
    try {
      const res  = await fetch(`/api/import/${this._session.sessionId}/execute`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({
          listaId:  this.listaId ? Number(this.listaId) : null,
          consenso: this._consenso
        })
      });
      const data = await res.json();
      if (data.err) {
        this._error = data.log;
      } else {
        this._result = data.out;
        this._step   = 4;
      }
    } catch (e) {
      this._error = 'Errore di rete';
    } finally {
      this._loading = false;
    }
  }

  _cancel() {
    this.dispatchEvent(new CustomEvent('cancel', { bubbles: true }));
  }

  _done() {
    this.dispatchEvent(new CustomEvent('done', { bubbles: true }));
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  render() {
    return html`
      <div class="container py-4" style="max-width:720px">
        <div class="d-flex align-items-center gap-2 mb-4">
          <button class="btn btn-sm btn-outline-secondary" @click=${this._cancel}>← Torna</button>
          <h1 class="h4 mb-0">Importa contatti</h1>
        </div>

        ${this._renderStepper()}
        ${this._error ? html`<div class="alert alert-danger mt-3">${this._error}</div>` : ''}

        <div class="mt-4">
          ${this._step === 1 ? this._renderStep1() : ''}
          ${this._step === 2 ? this._renderStep2() : ''}
          ${this._step === 3 ? this._renderStep3() : ''}
          ${this._step === 4 ? this._renderStep4() : ''}
        </div>
      </div>`;
  }

  _renderStepper() {
    const steps = ['Upload', 'Mappa', 'Valida', 'Fine'];
    return html`
      <div class="d-flex gap-0">
        ${steps.map((s, i) => {
          const n   = i + 1;
          const cls = n < this._step ? 'bg-success text-white' : n === this._step ? 'bg-primary text-white' : 'bg-light text-muted';
          return html`
            <div class="d-flex align-items-center">
              <span class="badge rounded-pill px-3 py-2 ${cls}">${n}. ${s}</span>
              ${n < steps.length ? html`<span class="text-muted px-1">→</span>` : ''}
            </div>`;
        })}
      </div>`;
  }

  _renderStep1() {
    return html`
      <div class="card">
        <div class="card-body">
          <h5 class="card-title">Seleziona file Excel</h5>
          <p class="text-muted small">Formati supportati: .xls, .xlsx. La prima riga deve contenere le intestazioni.</p>
          ${this._loading
            ? html`<p class="text-muted">Analisi in corso...</p>`
            : html`<input type="file" class="form-control" accept=".xls,.xlsx"
                          @change=${this._onFileChange}>`}
        </div>
      </div>`;
  }

  _renderStep2() {
    const headers = this._session?.headers ?? [];
    const preview = this._session?.preview ?? [];
    return html`
      <div>
        <h5>Mappa le colonne</h5>
        <p class="text-muted small">Associa ogni colonna del file al campo corrispondente nel sistema.</p>

        <div class="table-responsive mb-3">
          <table class="table table-sm">
            <thead class="table-light">
              <tr>
                <th>Colonna file</th>
                <th>Esempio</th>
                <th>Campo sistema</th>
              </tr>
            </thead>
            <tbody>
              ${headers.map(h => html`
                <tr>
                  <td><strong>${h}</strong></td>
                  <td class="text-muted small">${preview[0]?.[h] ?? ''}</td>
                  <td>
                    <select class="form-select form-select-sm"
                            @change=${e => this._setMapping(h, e.target.value)}>
                      <option value="">— ignora —</option>
                      ${this._campi.map(c => html`
                        <option value="${c.key}" ?selected=${this._mapping[h] === c.key}>${c.label}</option>`)}
                    </select>
                  </td>
                </tr>`)}
            </tbody>
          </table>
        </div>

        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-outline-secondary" @click=${() => { this._step = 1; }}>← Indietro</button>
          <button class="btn btn-sm btn-primary ms-auto" ?disabled=${this._loading}
                  @click=${this._saveMapping}>
            ${this._loading ? 'Salvataggio...' : 'Avanti →'}
          </button>
        </div>
      </div>`;
  }

  _renderStep3() {
    const v    = this._validation;
    const rows = v?.rows ?? [];
    return html`
      <div>
        <h5>Risultato validazione</h5>

        <div class="d-flex gap-3 mb-3">
          <span class="badge bg-success fs-6">${v?.valid ?? 0} valide</span>
          <span class="badge bg-warning text-dark fs-6">${v?.warnings ?? 0} avvisi</span>
          <span class="badge bg-danger fs-6">${v?.errors ?? 0} errori</span>
        </div>

        ${v?.errors > 0 ? html`
          <div class="alert alert-warning small">
            Le righe con errori verranno saltate durante l'importazione.
          </div>` : ''}

        <div class="table-responsive mb-3" style="max-height:300px;overflow-y:auto">
          <table class="table table-sm table-hover">
            <thead class="table-light sticky-top">
              <tr>
                <th>#</th>
                <th>Stato</th>
                <th>Nome / Ragione Sociale</th>
                <th>Telefono</th>
                <th>Messaggio</th>
              </tr>
            </thead>
            <tbody>
              ${rows.map((r, i) => html`
                <tr>
                  <td class="text-muted">${i + 1}</td>
                  <td>
                    ${r.status === 'ok'      ? html`<span class="badge bg-success">ok</span>` : ''}
                    ${r.status === 'warning' ? html`<span class="badge bg-warning text-dark">avviso</span>` : ''}
                    ${r.status === 'error'   ? html`<span class="badge bg-danger">errore</span>` : ''}
                  </td>
                  <td>${r.data?.nome ?? ''} ${r.data?.cognome ?? ''} ${r.data?.ragione_sociale ?? ''}</td>
                  <td>${r.data?.telefono ?? ''}</td>
                  <td class="small text-muted">${[...r.errors, ...r.warnings].join('; ')}</td>
                </tr>`)}
            </tbody>
          </table>
        </div>

        <div class="mb-3">
          <div class="form-check">
            <input class="form-check-input" type="checkbox" id="chk-consenso-import"
                   .checked=${this._consenso}
                   @change=${e => { this._consenso = e.target.checked; }}>
            <label class="form-check-label" for="chk-consenso-import">Consenso marketing</label>
          </div>
        </div>

        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-outline-secondary" @click=${() => { this._step = 2; }}>← Indietro</button>
          <button class="btn btn-sm btn-success ms-auto" ?disabled=${this._loading || (v?.valid + v?.warnings) === 0}
                  @click=${this._execute}>
            ${this._loading ? 'Importazione...' : `Importa ${(v?.valid ?? 0) + (v?.warnings ?? 0)} contatti →`}
          </button>
        </div>
      </div>`;
  }

  _renderStep4() {
    return html`
      <div class="text-center py-4">
        <div class="mb-3 fs-1">✓</div>
        <h5>Importazione completata</h5>
        <div class="d-flex justify-content-center gap-3 my-3">
          <span class="badge bg-success fs-6">${this._result?.imported ?? 0} importati</span>
          <span class="badge bg-warning text-dark fs-6">${this._result?.warnings ?? 0} duplicati saltati</span>
          <span class="badge bg-secondary fs-6">${this._result?.skipped ?? 0} righe senza identificatore</span>
        </div>
        <button class="btn btn-primary" @click=${this._done}>Chiudi</button>
      </div>`;
  }
}

customElements.define('importer-wizard', ImporterWizard);
export default ImporterWizard;
