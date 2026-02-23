import 'bootstrap/dist/css/bootstrap.min.css';
import './home.css';
import { auth, logout, checkAuth } from '../store.js';

class HomeLayout extends HTMLElement {
  connectedCallback() {
    this._render();
    auth.subscribe(() => this._render());
  }

  _render() {
    const { isAuthenticated, user } = auth.state;

    if (isAuthenticated && user?.must_change_password) {
      window.location.href = '/auth/changepass.html';
      return;
    }

    this.innerHTML = `
      <div class="min-vh-100 bg-light">
        <header class="d-flex align-items-center px-4 py-3 bg-white border-bottom">
          <span class="fw-bold fs-5">App</span>
          <div class="ms-auto d-flex align-items-center gap-2">
            ${isAuthenticated
              ? `<span class="text-muted small">${user?.username}</span>
                 <button class="btn btn-sm btn-outline-danger" id="btn-logout">Esci</button>`
              : `<button class="btn btn-sm btn-outline-primary" id="btn-login">Accedi</button>`
            }
          </div>
        </header>
        <main class="container py-5">
          <div class="text-center py-5">
            <h1 class="display-5">Benvenuto</h1>
            <p class="text-muted">Seleziona un modulo per iniziare.</p>
          </div>
        </main>
      </div>
    `;
    this.querySelector('#btn-logout')?.addEventListener('click', logout);
    this.querySelector('#btn-login')?.addEventListener('click', () => {
      window.location.href = '/auth/login.html';
    });
  }
}

customElements.define('home-layout', HomeLayout);
checkAuth();
