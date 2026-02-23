import 'bootstrap/dist/css/bootstrap.min.css';
import './auth.css';

class ChangepassLayout extends HTMLElement {
  constructor() {
    super();
    this._loading = false;
    this._error   = null;
  }

  connectedCallback() {
    this._checkAccess();
  }

  async _checkAccess() {
    try {
      const res  = await fetch('/api/auth/session');
      const data = await res.json();
      if (data.err || !data.out) {
        window.location.href = '/auth/login.html';
        return;
      }
      if (!data.out.must_change_password) {
        window.location.href = '/home/main.html';
        return;
      }
    } catch (_) {
      window.location.href = '/auth/login.html';
      return;
    }
    this._render();
  }

  _render() {
    const cp = this.querySelector('#current_password')?.value  || '';
    const np = this.querySelector('#new_password')?.value      || '';
    const rp = this.querySelector('#confirm_password')?.value  || '';
    this.innerHTML = `
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          <h4 class="mb-1">Imposta nuova password</h4>
          <p class="text-muted small mb-4">La password temporanea deve essere modificata prima di continuare.</p>
          ${this._error ? `<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="current_password">Password attuale</label>
            <input id="current_password" type="password" class="form-control" ${this._loading ? 'disabled' : ''}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="new_password">Nuova password</label>
            <input id="new_password" type="password" class="form-control" ${this._loading ? 'disabled' : ''}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="confirm_password">Conferma nuova password</label>
            <input id="confirm_password" type="password" class="form-control" ${this._loading ? 'disabled' : ''}>
          </div>
          <button id="submit-change" class="btn btn-primary w-100" ${this._loading ? 'disabled' : ''}>
            ${this._loading ? 'Salvataggio in corso...' : 'Imposta password'}
          </button>
        </div>
      </div>
    `;
    this.querySelector('#current_password').value  = cp;
    this.querySelector('#new_password').value      = np;
    this.querySelector('#confirm_password').value  = rp;

    const submit = async () => {
      const currentPassword = this.querySelector('#current_password').value;
      const newPassword     = this.querySelector('#new_password').value;
      const confirmPassword = this.querySelector('#confirm_password').value;

      if (!currentPassword || !newPassword || !confirmPassword) {
        this._error = 'Tutti i campi sono obbligatori';
        this._render();
        return;
      }
      if (newPassword.length < 8) {
        this._error = 'La password deve contenere almeno 8 caratteri';
        this._render();
        return;
      }
      if (newPassword !== confirmPassword) {
        this._error = 'Le password non coincidono';
        this._render();
        return;
      }
      if (newPassword === currentPassword) {
        this._error = 'La nuova password deve essere diversa da quella temporanea';
        this._render();
        return;
      }

      this._loading = true;
      this._error   = null;
      this._render();
      try {
        const res = await fetch('/api/auth/change-password', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            current_password: currentPassword,
            new_password:     newPassword
          })
        });
        const data = await res.json();
        if (!res.ok || data.err) throw new Error(data.log || 'Errore durante il cambio password');
        window.location.href = '/home/main.html';
      } catch (e) {
        this._error   = e.message;
        this._loading = false;
        this._render();
      }
    };

    this.querySelector('#submit-change').addEventListener('click', submit);
    this.querySelectorAll('input').forEach(el =>
      el.addEventListener('keydown', e => {
        if (e.key === 'Enter') submit();
      })
    );
  }
}

customElements.define('changepass-layout', ChangepassLayout);
