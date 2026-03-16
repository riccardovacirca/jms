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
      import('./Register.js').then(() => {
        container.innerHTML = '<user-register-page></user-register-page>';
      });
    } else if (sub === 'forgot-password') {
      import('./ForgotPassword.js').then(() => {
        container.innerHTML = '<user-forgot-password-page></user-forgot-password-page>';
      });
    } else if (sub === 'reset-password') {
      import('./ResetPassword.js').then(() => {
        container.innerHTML = '<user-reset-password-page></user-reset-password-page>';
      });
    } else if (sub === 'change-password') {
      import('./ChangePassword.js').then(() => {
        container.innerHTML = '<user-change-password-page></user-change-password-page>';
      });
    } else {
      import('./Login.js').then(() => {
        container.innerHTML = '<user-login-page></user-login-page>';
      });
    }
  } else if (section === 'account') {
    if (!authorized.get()) { window.location.hash = '/user/auth/login'; return; }
    import('./Account.js').then(() => {
      container.innerHTML = '<user-account-page></user-account-page>';
    });
  } else if (section === 'profile') {
    if (!authorized.get()) { window.location.hash = '/user/auth/login'; return; }
    import('./Profile.js').then(() => {
      container.innerHTML = '<user-profile-page></user-profile-page>';
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
