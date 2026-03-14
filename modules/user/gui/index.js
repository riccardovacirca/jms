import 'bootstrap/dist/css/bootstrap.min.css';

function renderRoute(container) {
  if (!window.location.hash.startsWith('#/user')) { return; }
  import('./profile.js').then(() => {
    container.innerHTML = '<user-profile-page></user-profile-page>';
  });
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
