import 'bootstrap/dist/css/bootstrap.min.css';
import HeaderComponent from './component.js';

export default {
  mount(container) {
    const component = new HeaderComponent();
    container.appendChild(component);
  }
};
