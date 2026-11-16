// Direct app.html links on desktop always enter the device preview.
(() => {
  if (
    window.self === window.top &&
    window.matchMedia('(min-width: 601px)').matches
  ) {
    const target = new URL('index.html', window.location.href);
    target.search = window.location.search;
    window.location.replace(target.href);
  }
})();
