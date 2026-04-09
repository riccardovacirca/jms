import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import Dashboard from './Dashboard.js';

export default {
  mount(container) {
    const component = new Dashboard();
    container.appendChild(component);
  }
};
