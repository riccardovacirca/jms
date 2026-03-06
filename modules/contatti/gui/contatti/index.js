import 'bootstrap/dist/css/bootstrap.min.css';
import './contatti.css';
import './component.js';
import './liste-component.js';
import ContattiModuleComponent from './module-component.js';

export default {
  mount(container) {
    container.appendChild(new ContattiModuleComponent());
  }
};
