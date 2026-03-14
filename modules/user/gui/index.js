import 'bootstrap/dist/css/bootstrap.min.css';

/** Restituisce la sezione corrente dall'hash: 'list' | 'edit'. */
function parseSection() {
  const parts = window.location.hash.replace(/^#\//, '').split('/');
  return parts[1] === 'edit' ? 'edit' : 'list';
}

function renderRoute(container) {
  const section = parseSection();
  if (section === 'edit') {
    import('./edit.js').then(() => {
      container.innerHTML = '<user-edit-page></user-edit-page>';
    });
  } else {
    import('./list.js').then(() => {
      container.innerHTML = '<user-list-page></user-list-page>';
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
