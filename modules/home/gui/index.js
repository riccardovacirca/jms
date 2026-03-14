import 'bootstrap/dist/css/bootstrap.min.css';
import './home.css';
import HomeComponent from './component.js';

export default {
  mount(container) {
    const component = new HomeComponent();
    container.appendChild(component);
  }
};
