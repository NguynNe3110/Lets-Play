(() => {
  // A real phone opens the app directly so its safe-area and keyboard viewport are native.
  if (window.matchMedia('(max-width: 600px)').matches) {
    const target = new URL('app.html', window.location.href);
    target.search = window.location.search;
    if (target.searchParams.has('screen'))
      target.searchParams.set('catalog', '1');
    window.location.replace(target.href);
    return;
  }
  const frame = document.getElementById('phoneFrame');
  const wrap = document.getElementById('phoneWrap');
  const stage = document.getElementById('previewStage');
  const app = frame.querySelector('iframe');
  let width = 390,
    height = 844;
  const fit = () => {
    const mobile = window.matchMedia('(max-width: 600px)').matches;
    if (mobile) return;
    const outerWidth = width + 24,
      outerHeight = height + 24;
    const scale = Math.max(
      0.2,
      Math.min(
        1,
        (stage.clientWidth - 48) / outerWidth,
        (stage.clientHeight - 48) / outerHeight,
      ),
    );
    Object.assign(frame.style, {
      width: `${outerWidth}px`,
      height: `${outerHeight}px`,
      transform: `scale(${scale})`,
    });
    Object.assign(wrap.style, {
      width: `${outerWidth * scale}px`,
      height: `${outerHeight * scale}px`,
    });
    Object.assign(app.style, { width: `${width}px`, height: `${height}px` });
  };
  document.querySelectorAll('[data-width]').forEach((button) => {
    button.addEventListener('click', () => {
      width = Number(button.dataset.width);
      height = Number(button.dataset.height);
      document.querySelectorAll('[data-width]').forEach((item) => {
        item.classList.toggle('active', item === button);
        item.setAttribute('aria-pressed', String(item === button));
      });
      fit();
    });
  });
  window.addEventListener('resize', fit);
  const screens = window.KeoScreens || [];
  const list = document.getElementById('screenList');
  const search = document.getElementById('screenSearch');
  const free = document.getElementById('freePreview');
  let selectedScreen =
    new URL(window.location.href).searchParams.get('screen') || '';
  const plain = (value) =>
    value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd');
  const renderList = () => {
    const query = plain(search.value.trim());
    const filtered = screens.filter((screen) =>
      plain(screen.title + ' ' + screen.group).includes(query),
    );
    const groups = [...new Set(filtered.map((screen) => screen.group))];
    list.innerHTML = groups
      .map(
        (group) =>
          `<details open><summary>${group}</summary>${filtered
            .filter((screen) => screen.group === group)
            .map(
              (screen) =>
                `<button type="button" data-screen="${screen.id}" class="${screen.id === selectedScreen ? 'active' : ''}" ${screen.id === selectedScreen ? 'aria-current="page"' : ''}><span>${screen.title}</span><i aria-hidden="true">›</i></button>`,
            )
            .join('')}</details>`,
      )
      .join('');
    document.getElementById('screenCount').textContent = query
      ? `${filtered.length}/${screens.length}`
      : screens.length;
    document.getElementById('screenEmpty').hidden = filtered.length > 0;
    free.classList.toggle('active', !selectedScreen);
  };
  const selectScreen = (id, updateHistory = true) => {
    const screen = screens.find((item) => item.id === id);
    selectedScreen = screen ? screen.id : '';
    const source = new URL('app.html', window.location.href);
    source.searchParams.set('preview', '1');
    if (screen) {
      source.searchParams.set('catalog', '1');
      source.searchParams.set('screen', screen.id);
    }
    app.src = source.href;
    app.title = screen
      ? screen.title
      : 'Prototype ứng dụng Kèo trên điện thoại';
    if (updateHistory) {
      const address = new URL(window.location.href);
      address.search = screen ? `?screen=${encodeURIComponent(screen.id)}` : '';
      window.history.replaceState(null, '', address.href);
    }
    renderList();
  };
  list.addEventListener('click', (event) => {
    const button = event.target.closest('[data-screen]');
    if (button) selectScreen(button.dataset.screen);
  });
  search.addEventListener('input', renderList);
  free.addEventListener('click', () => selectScreen(''));
  selectScreen(selectedScreen, false);
  fit();
})();
