/**
 * Modulo CTI — punto di ingresso frontend.
 * Monta il componente web della sessione chiamate nel container assegnato.
 */
import './Call.js';

/**
 * Monta il modulo CTI nel container indicato dal router.
 *
 * @param {HTMLElement} container elemento DOM in cui renderizzare il modulo
 */
function mount(container)
{
  container.innerHTML = '<cti-call></cti-call>';
}

export default { mount };
