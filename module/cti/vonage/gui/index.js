/**
 * Modulo CTI — punto di ingresso frontend.
 * Monta la CTI bar nel container assegnato.
 *
 * La barra è fissa in basso (position: fixed) e si sovrappone al contenuto di pagina.
 * Per integrare il CTI in un altro modulo (es. CRM), importare Call.js direttamente
 * e includere <cti-bar> nel template del modulo ospitante.
 */
import './Call.js';

/**
 * Monta il modulo CTI nel container indicato dal router.
 * Inserisce il custom element <cti-bar> che si posiziona fisso in basso.
 *
 * @param {HTMLElement} container elemento DOM in cui registrare il componente
 */
function mount(container)
{
  container.innerHTML = '<cti-bar></cti-bar>';
}

export default { mount };
