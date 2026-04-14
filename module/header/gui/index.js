import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import Header from './Header.js';

export default {
  async mount(container) {
    const component = new Header();
    try {
      const res = await fetch('/api/config');
      const data = await res.json();
      component['app-name'] = data.out?.app_title ?? 'App';
    } catch (_) {}
    container.appendChild(component);
  }
};
