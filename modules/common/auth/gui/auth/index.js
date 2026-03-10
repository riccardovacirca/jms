import 'bootstrap/dist/css/bootstrap.min.css';
import './auth.css';
import { authorized, user } from '../../store.js';
import LoginComponent from './login.js';
import ChangepassComponent from './changepass.js';

export default {
  mount(container) {
    if (authorized.get()) {
      window.location.hash = '/account';
      return;
    }
    showLogin(container);
  }
};

function showLogin(container) {
  container.innerHTML = '';
  const login = new LoginComponent();
  login.addEventListener('must-change-password', () => {
    container.innerHTML = '';
    const changepass = new ChangepassComponent();
    changepass.addEventListener('password-changed', () => {
      authorized.set(false);
      user.set(null);
      showLogin(container);
    });
    container.appendChild(changepass);
  });
  container.appendChild(login);
}
