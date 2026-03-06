import 'bootstrap/dist/css/bootstrap.min.css';
import './rapporti.css';
import './component.js';
import RapportiModuleComponent from './module-component.js';

export default {
  mount(container) {
    container.appendChild(new RapportiModuleComponent());
  }
};
