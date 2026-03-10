import 'bootstrap/dist/css/bootstrap.min.css';

export default {
  mount(container) {
    import('./account.js').then(() => {
      container.innerHTML = '<account-users-page></account-users-page>';
    });
  }
};
