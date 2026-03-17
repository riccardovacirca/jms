import 'bootstrap/dist/css/bootstrap.min.css';
import './home.css';
import Home from './Home.js';

export default {
  mount(container) {
    const component = new Home();
    container.appendChild(component);
  }
};
