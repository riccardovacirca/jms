import 'bootstrap/dist/css/bootstrap.min.css';
import Header from './Header.js';

export default {
  mount(container) {
    const component = new Header();
    container.appendChild(component);
  }
};
