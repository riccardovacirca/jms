import 'bootstrap/dist/css/bootstrap.min.css';
import './auth.css';
import { authorized } from '../../store.js';

/**
 * Restituisce la sezione corrente dall'hash.
 * @returns {{ section: string|null, sub: string|null }}
 */
function parseSection() {
  const parts   = window.location.hash.replace(/^#\//, '').split('/');
  const section = parts[1] || null;
  const sub     = parts[2] || null;
  return { section, sub };
}

function renderRoute(container) {
  const { section, sub } = parseSection();

  if (section === null && authorized.get()) {
    window.location.hash = '/auth/account';
    return;
  }

  if (section === 'account' && sub === 'edit') {
    import('./account-edit.js').then(() => {
      container.innerHTML = '<auth-account-edit-page></auth-account-edit-page>';
    });
  } else if (section === 'account') {
    import('./account-list.js').then(() => {
      container.innerHTML = '<auth-account-list-page></auth-account-list-page>';
    });
  } else if (section === 'forgot') {
    import('./forgot.js').then(() => {
      container.innerHTML = '<auth-forgot-page></auth-forgot-page>';
    });
  } else if (section === 'reset') {
    import('./reset.js').then(() => {
      container.innerHTML = '<auth-reset-page></auth-reset-page>';
    });
  } else if (section === 'changepass') {
    import('./changepass.js').then(() => {
      container.innerHTML = '<auth-changepass-page></auth-changepass-page>';
    });
  } else {
    import('./login.js').then(() => {
      container.innerHTML = '<auth-login-page></auth-login-page>';
    });
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
