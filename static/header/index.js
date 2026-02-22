import { auth, logout } from '../store.js';

class HeaderLayout extends HTMLElement {
  connectedCallback() {
    this._render();
    auth.subscribe(() => this._render());
  }

  _render() {
    const { user } = auth.state;
    this.innerHTML = `
      <header class="d-flex align-items-center px-3 bg-white border-bottom" style="height:56px">
        <div class="ms-auto d-flex align-items-center gap-2">
          ${user ? `
            <span class="text-muted small">${user.username}</span>
            <button id="logout" class="btn btn-sm btn-outline-danger">Esci</button>
          ` : ''}
        </div>
      </header>
    `;
    this.querySelector('#logout')?.addEventListener('click', logout);
  }
}

customElements.define('header-layout', HeaderLayout);
