// Executes render functions with a minimal DOM surface. This is not browser or visual QA.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const nodes = {};
for (const id of ['app', 'modal', 'toast']) nodes[id] = { innerHTML: '', textContent: '', dataset: {}, classList: { add() {}, remove() {} }, addEventListener() {}, focus() {}, open: false, close() { this.open = false; }, showModal() { this.open = true; } };
const document = { activeElement: null, getElementById: id => nodes[id] || null, addEventListener() {} };
const sandbox = { document, localStorage: { getItem: () => null, setItem() {} }, navigator: {}, console, Intl, Date, setTimeout, clearTimeout, URL, Blob, addEventListener() {}, scrollTo() {} };
sandbox.window = sandbox;
vm.createContext(sandbox);
for (const file of ['domain.js', 'scenes.js', 'app.js']) vm.runInContext(fs.readFileSync(path.join(__dirname, '../dist', file), 'utf8'), sandbox, { filename: file });
const app = sandbox.KeoPrototype, s = app.getState(), view = app.getView();
assert.equal(view.page, 'camera', 'Mở prototype vào Home chụp ảnh');
assert(nodes.app.innerHTML.includes('aria-label="Điều hướng chính"'));
assert(!nodes.app.innerHTML.includes('class="sidebar"'));
assert(!nodes.app.innerHTML.includes('class="demo-bar"'));
let checked = 0;
for (const user of s.users) {
  s.currentUser = user.id;
  for (const page of ['feed', 'rooms', 'library', 'camera', 'inbox']) {
    Object.assign(view, { page, captured: false, chatPeer: null }); app.render(); assert(nodes.app.innerHTML.includes('<main id="main"')); assert(!nodes.app.innerHTML.includes('undefined')); checked++;
  }
  for (const r of s.rooms.filter(r => r.members.includes(user.id))) for (const tab of ['plan', 'schedule', 'money', 'album', 'members']) {
    Object.assign(view, { page: 'room', roomId: r.id, roomTab: tab }); app.render(); assert(!nodes.app.innerHTML.includes('undefined'), `${user.id}/${r.id}/${tab}`); checked++;
  }
}
s.currentUser = 'an';
s.draft = { userId: 'an', createdAt: Date.now(), scene: 'cafe', caption: 'Chú thích <script>alert(1)</script>', roomId: 'dalat', personalEnabled: true, audienceMode: 'SELECTED_FRIENDS', selectedFriends: ['binh'] };
Object.assign(view, { page: 'camera', captured: true }); app.render(); assert(nodes.app.innerHTML.includes('&lt;script&gt;alert(1)&lt;/script&gt;')); assert(!nodes.app.innerHTML.includes('<script>alert(1)</script>')); checked++;
Object.assign(view, { page: 'inbox', chatPeer: 'binh' }); app.render(); assert(nodes.app.innerHTML.includes('Chỉ hai bạn đọc được')); checked++;
s.draft.unknownResult = true; s.draft.pendingInput = { key: 'already-committed' };
Object.assign(view, { page: 'camera', captured: true }); app.render(); assert(nodes.app.innerHTML.includes('readonly')); assert(nodes.app.innerHTML.includes('Kết quả lần đăng trước chưa được xác nhận')); checked++;
sandbox.KeoDomain.setPhase(s, 'an', 'dalat', 'order');
Object.assign(view, { page: 'room', roomId: 'dalat', roomTab: 'plan', planPanel: 'order' }); app.render(); assert(nodes.app.innerHTML.includes('Lịch cuối do chủ phòng sắp')); assert(!nodes.app.innerHTML.includes('undefined')); checked++;
sandbox.KeoDomain.setPhase(s, 'an', 'dalat', 'plan'); sandbox.KeoDomain.lifecycle(s, 'an', 'dalat', 'end'); sandbox.KeoDomain.setPolicy(s, 'an', 'dalat', 'VIEW_ONLY', null);
Object.assign(view, { page: 'room', roomTab: 'album' }); app.render(); assert(nodes.app.innerHTML.includes('Album chỉ xem lại')); checked++;
console.log(`PASS ${checked} render paths; no browser/layout/camera assertions made.`);
