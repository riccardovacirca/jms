import 'bootstrap/dist/css/bootstrap.min.css';
import './contatti.css';
import './Contatti.js';
import './Liste.js';
import ContattiModule from './ContattiModule.js';

export default {
  mount(container) {
    container.appendChild(new ContattiModule());
  }
};
