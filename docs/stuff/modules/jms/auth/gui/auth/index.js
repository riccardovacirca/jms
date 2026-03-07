import 'bootstrap/dist/css/bootstrap.min.css';
import './auth.css';
import { authorized, user } from '../../store.js';
import { DEFAULT_ROUTE } from '../../config.js';
import LoginComponent from './login.js';
import ChangepassComponent from './changepass.js';

export default {
  mount(container) {
    // Il router ripristina la sessione all'avvio prima di caricare qualsiasi modulo.
    // Se l'utente arriva qui già autorizzato (es. digitando /#/auth a mano),
    // redirect diretto senza ulteriore network call.
    if (authorized.get()) {
      window.location.hash = DEFAULT_ROUTE;
      return;
    }

    const showLogin = () => {
      container.innerHTML = '';
      const login = new LoginComponent();
      login.addEventListener('must-change-password', () => {
        container.innerHTML = '';
        const changepass = new ChangepassComponent();
        changepass.addEventListener('password-changed', () => {
          authorized.set(false);
          user.set(null);
          showLogin();
        });
        container.appendChild(changepass);
      });
      container.appendChild(login);
    };

    showLogin();
  }
};
