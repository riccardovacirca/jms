import { currentModule } from '../store.js'

// Menu — aggiungere una voce per ogni modulo privato
const menuItems = [
  // { id: 'dashboard', label: 'Dashboard', icon: '📊' },
]

class SidebarLayout extends HTMLElement {
  connectedCallback() {
    this._render()
    currentModule.subscribe(() => this._render())
  }

  _render() {
    const current = currentModule.state.name
    const items = menuItems.map(({ id, label, icon }) => `
      <li>
        <button data-module="${id}" style="
          background:${current === id ? '#343a40' : 'none'};
          color:${current === id ? '#fff' : '#adb5bd'};
          border:none;border-radius:4px;padding:.375rem .75rem;
          width:100%;text-align:left;cursor:pointer">
          <span style="margin-right:.5rem">${icon}</span>${label}
        </button>
      </li>
    `).join('')

    this.innerHTML = `
      <nav style="width:220px;min-height:100vh;background:#212529;color:#fff;display:flex;flex-direction:column;padding:.5rem">
        <div style="padding:.75rem .5rem;font-weight:bold;font-size:1.1rem">App</div>
        <ul class="nav flex-column gap-1">${items}</ul>
      </nav>
    `
    this.querySelectorAll('[data-module]').forEach(btn =>
      btn.addEventListener('click', () => currentModule.navigate(btn.dataset.module))
    )
  }
}

customElements.define('sidebar-layout', SidebarLayout)
