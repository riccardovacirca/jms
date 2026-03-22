import 'bootstrap/dist/css/bootstrap.min.css';
import { authorized } from '../../store.js';

/**
 * Parsa la sezione e la sotto-sezione dall'hash corrente.
 * @returns {{ section: string|null, sub: string|null }}
 */
function parseSection() {
  const parts   = window.location.hash.replace(/^#\//, '').split('/');
  const section = parts[1] ? parts[1].split('?')[0] : null;
  const sub     = parts[2] ? parts[2].split('?')[0] : null;
  return { section, sub };
}

function renderRoute(container) {
  const { section, sub } = parseSection();

  if (section === 'auth') {
    if (sub === 'register') {
      import('./auth/Register.js').then(() => {
        container.innerHTML = '<user-register></user-register>';
      });
    } else if (sub === 'forgot-password') {
      import('./auth/ForgotPassword.js').then(() => {
        container.innerHTML = '<user-forgot-password></user-forgot-password>';
      });
    } else if (sub === 'reset-password') {
      import('./auth/ResetPassword.js').then(() => {
        container.innerHTML = '<user-reset-password></user-reset-password>';
      });
    } else if (sub === 'change-password') {
      import('./auth/ChangePassword.js').then(() => {
        container.innerHTML = '<user-change-password></user-change-password>';
      });
    } else {
      import('./auth/Login.js').then(() => {
        container.innerHTML = '<user-login></user-login>';
      });
    }
  } else if (section === 'account') {
    if (!authorized.get()) { window.location.hash = '/user/auth/login'; return; }
    import('./account/Settings.js').then(() => {
      container.innerHTML = '<user-account></user-account>';
    });
  } else if (section === 'profile') {
    if (!authorized.get()) { window.location.hash = '/user/auth/login'; return; }
    import('./account/Profile.js').then(() => {
      container.innerHTML = '<user-profile></user-profile>';
    });
  } else {
    if (authorized.get()) {
      window.location.hash = '/user/account';
    } else {
      window.location.hash = '/user/auth/login';
    }
  }
}

let _handler = null;

export default {
  mount(container) {
    renderRoute(container);
    _handler = () => renderRoute(container);
    window.addEventListener('hashchange', _handler);
  },
  unmount() {
    if (_handler) {
      window.removeEventListener('hashchange', _handler);
      _handler = null;
    }
  }
};
