import { LitElement, html } from 'lit';
import '../ImporterWizard.js';

/**
 * Wrapper per il wizard di importazione contatti.
 * Accetta il listaId come proprietà e ri-emette gli eventi cancel e done
 * verso il componente padre.
 */
class ImportaLista extends LitElement {

  static properties = {
    listaId: {}
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.listaId = null;
  }

  render() {
    return html`
      <importer-wizard
        .listaId=${this.listaId ?? null}
        @cancel=${() => this.dispatchEvent(new CustomEvent('cancel', { bubbles: true }))}
        @done=${()   => this.dispatchEvent(new CustomEvent('done',   { bubbles: true }))}>
      </importer-wizard>`;
  }
}

customElements.define('sales-importa-lista', ImportaLista);
