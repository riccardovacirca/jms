import 'bootstrap/dist/css/bootstrap.min.css'
import './index.css'

class AuthLayout extends HTMLElement {
  constructor() {
    super()
    this._loading = false
    this._error   = null
  }

  connectedCallback() { this._render() }

  _render() {
    const u = this.querySelector('#username')?.value || ''
    const p = this.querySelector('#password')?.value || ''
    this.innerHTML = `
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          <h4 class="mb-4">Accedi</h4>
          ${this._error ? `<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input id="username" class="form-control" ${this._loading ? 'disabled' : ''}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="password">Password</label>
            <input id="password" type="password" class="form-control" ${this._loading ? 'disabled' : ''}>
          </div>
          <button id="submit" class="btn btn-primary w-100" ${this._loading ? 'disabled' : ''}>
            ${this._loading ? 'Accesso in corso...' : 'Accedi'}
          </button>
        </div>
      </div>
    `
    this.querySelector('#username').value = u
    this.querySelector('#password').value = p

    const submit = async () => {
      this._loading = true; this._error = null; this._render()
      try {
        const res  = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: this.querySelector('#username').value,
            password: this.querySelector('#password').value
          })
        })
        const data = await res.json()
        if (!res.ok || data.err) throw new Error(data.log || 'Credenziali non valide')
        window.location.href = '/home'
      } catch (e) {
        this._error = e.message
        this._loading = false
        this._render()
      }
    }

    this.querySelector('#submit').addEventListener('click', submit)
    this.querySelectorAll('input').forEach(el =>
      el.addEventListener('keydown', e => { if (e.key === 'Enter') submit() })
    )
  }
}

customElements.define('auth-layout', AuthLayout)
