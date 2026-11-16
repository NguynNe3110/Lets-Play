const assert = require('node:assert/strict');
const D = require('../dist/domain.js');
let passed = 0, failed = 0;
function test(name, fn) { try { fn(); passed++; console.log(`PASS ${name}`); } catch (e) { failed++; console.error(`FAIL ${name}\n${e.stack}`); } }
const code = (fn, value) => assert.throws(fn, e => e.code === value);
const payload = (s, extra = {}) => ({ key: 'intent-1', scene: 'cafe', caption: 'Kỷ niệm', destinations: { personal: D.audience(s, 'an', 'SELECTED_FRIENDS', ['binh']), roomId: 'dalat' }, ...extra });
test('publish creates one original, two independent contexts, and deduplicated notices', () => {
  const s = D.seed(), before = s.moments.length, notices = s.notifications.length;
  const result = D.publish(s, 'an', payload(s));
  assert.equal(s.moments.length, before + 1); assert.equal(result.placementIds.length, 2);
  assert.equal(s.notifications.length - notices, 3);
  assert.equal(D.ownMoments(s, 'an').filter(x => x.id === result.momentId).length, 1);
});
test('all destinations fail atomically if the room closes', () => {
  const s = D.seed(); D.lifecycle(s, 'an', 'dalat', 'end'); D.setPolicy(s, 'an', 'dalat', 'VIEW_ONLY', null);
  const before = JSON.stringify({ moments: s.moments, placements: s.placements, notifications: s.notifications });
  code(() => D.publish(s, 'an', payload(s)), 'ALBUM_READ_ONLY');
  assert.equal(JSON.stringify({ moments: s.moments, placements: s.placements, notifications: s.notifications }), before);
});
test('retry after a lost response returns original IDs and does not fan out again', () => {
  const s = D.seed(), input = payload(s), first = D.publish(s, 'an', input), n = s.notifications.length;
  assert.deepEqual(D.publish(s, 'an', input), first); assert.equal(s.notifications.length, n);
  code(() => D.publish(s, 'an', { ...input, caption: 'Khác' }), 'IDEMPOTENCY_KEY_REUSED');
});
test('room-only photo never enters a friend personal feed', () => {
  const s = D.seed(), result = D.publish(s, 'an', payload(s, { destinations: { roomId: 'dalat' } }));
  assert(!D.photos(s, 'binh').some(p => p.momentId === result.momentId));
  assert(D.photos(s, 'binh', 'dalat').some(p => p.momentId === result.momentId));
});
test('membership and friendship grant different access', () => {
  const s = D.seed(); assert(!D.visible(s, 'chi', D.placement(s, 'm1_personal'))); assert(D.visible(s, 'chi', D.placement(s, 'm1_room')));
  assert(!D.visible(s, 'dung', D.placement(s, 'm1_room')));
});
test('joining grants existing album history but does not create friendship', () => {
  const s = D.seed(); D.joinRoom(s, 'dung', 'DALAT26'); assert(D.visible(s, 'dung', D.placement(s, 'm3_room'))); assert(!D.friends(s, 'dung', 'chi'));
});
test('owner leaving keeps original rights without group discussion rights', () => {
  const s = D.seed(); s.rooms[0].hostId = 'binh'; D.leave(s, 'an', 'dalat');
  assert(D.ownMoments(s, 'an').some(m => m.id === 'm1')); assert(!D.visible(s, 'an', D.placement(s, 'm1_room')));
  assert(D.visible(s, 'chi', D.placement(s, 'm1_room'))); D.deleteMoment(s, 'an', 'm1'); assert(!D.visible(s, 'chi', D.placement(s, 'm1_room')));
});
test('unfriend then immediate re-friend never resurrects old grants', () => {
  const s = D.seed(); D.changeRelation(s, 'an', 'binh', 'unfriend'); D.changeRelation(s, 'an', 'binh', 'request'); D.changeRelation(s, 'binh', 'an', 'accept');
  assert(D.friends(s, 'an', 'binh')); assert(!D.visible(s, 'binh', D.placement(s, 'm1_personal'))); assert(D.visible(s, 'binh', D.placement(s, 'm1_room')));
});
test('audience snapshot invalidates when epoch changes even after re-friending', () => {
  const s = D.seed(), input = payload(s); D.changeRelation(s, 'an', 'binh', 'unfriend'); D.changeRelation(s, 'an', 'binh', 'request'); D.changeRelation(s, 'binh', 'an', 'accept');
  code(() => D.publish(s, 'an', input), 'AUDIENCE_CHANGED');
});
test('block filters group content without removing it for third parties', () => {
  const s = D.seed(); D.changeRelation(s, 'an', 'binh', 'block'); assert(!D.visible(s, 'binh', D.placement(s, 'm1_room'))); assert(D.visible(s, 'chi', D.placement(s, 'm1_room')));
  D.changeRelation(s, 'an', 'binh', 'unblock'); assert(D.visible(s, 'binh', D.placement(s, 'm1_room'))); assert(!D.visible(s, 'binh', D.placement(s, 'm1_personal')));
});
test('policy has exact inclusive deadline boundary', () => {
  const s = D.seed(), r = s.rooms[0]; r.lifecycle = 'ENDED'; r.endedAt = 1000000; r.policy = { mode: 'WINDOWED', windowHours: 72 }; const deadline = r.endedAt + 72 * D.HOUR;
  assert(D.writable(r, deadline - 1).open); assert(!D.writable(r, deadline).open); assert(!D.writable(r, deadline + 1).open);
});
test('all three modes differ after end and do not close an active room', () => {
  const s = D.seed(), r = s.rooms[0]; D.setPolicy(s, 'an', r.id, 'VIEW_ONLY', null); assert(D.writable(r, Date.now()).open);
  D.lifecycle(s, 'an', r.id, 'end'); assert(!D.writable(r, Date.now()).open);
  D.setPolicy(s, 'an', r.id, 'OPEN', null); assert(D.writable(r, Date.now() + 10000 * D.HOUR).open);
  D.setPolicy(s, 'an', r.id, 'WINDOWED', 1); assert(!D.writable(r, r.endedAt + D.HOUR).open);
});
test('restore preserves endedAt and expired deadline', () => {
  const s = D.seed(), r = s.rooms.find(r => r.id === 'sea'); const endedAt = r.endedAt;
  s.clockOffset = 96 * D.HOUR; D.lifecycle(s, 'an', r.id, 'archive'); D.lifecycle(s, 'an', r.id, 'restore'); assert.equal(r.endedAt, endedAt); assert(!D.writable(r, D.now(s)).open);
});
test('members cannot edit policy and limits reject fractions', () => {
  const s = D.seed(); code(() => D.setPolicy(s, 'chi', 'dalat', 'OPEN', null), 'HOST_REQUIRED');
  for (const h of [0, 721, 1.5]) code(() => D.setPolicy(s, 'an', 'dalat', 'WINDOWED', h), 'VALIDATION');
});
test('closed album still permits removing own reaction and own comment', () => {
  const s = D.seed(); D.lifecycle(s, 'an', 'dalat', 'end'); D.setPolicy(s, 'an', 'dalat', 'VIEW_ONLY', null);
  code(() => D.react(s, 'chi', 'm1_room', 'HEART'), 'ALBUM_READ_ONLY'); D.react(s, 'chi', 'm1_room', 'FIRE'); assert(!s.reactions.some(r => r.placementId === 'm1_room' && r.userId === 'chi'));
  D.deleteComment(s, 'chi', 'c1'); assert.equal(s.comments.find(c => c.id === 'c1').state, 'DELETED');
});
test('reactions remain isolated between contexts', () => {
  const s = D.seed(); D.react(s, 'binh', 'm1_personal', 'WOW'); assert(s.reactions.some(r => r.placementId === 'm1_personal' && r.type === 'WOW')); assert(!s.reactions.some(r => r.placementId === 'm1_room' && r.type === 'WOW'));
});
test('album comment and photo comment remain separate, personal has no public comment', () => {
  const s = D.seed(); D.comment(s, 'an', { key: 'c-new', kind: 'ALBUM', targetId: 'dalat', text: 'Cả nhóm ơi' });
  assert.equal(D.comments(s, 'an', 'ALBUM', 'dalat').length, 2); assert.equal(D.comments(s, 'an', 'PHOTO', 'm1_room').length, 1);
  code(() => D.comment(s, 'an', { key: 'bad', kind: 'PHOTO', targetId: 'm1_personal', text: 'Không đúng nơi' }), 'NO_ACCESS');
});
test('host removes a group placement but cannot delete another owner original', () => {
  const s = D.seed(); D.removePlace(s, 'an', 'm2_room', 'Gỡ trong nhóm'); assert(D.moment(s, 'm2').status === 'PUBLISHED'); assert(D.visible(s, 'an', D.placement(s, 'm2_personal')));
  code(() => D.deleteMoment(s, 'an', 'm2'), 'NO_ACCESS');
});
test('reattaching creates a new context without resurrecting old comments', () => {
  const s = D.seed(); D.removePlace(s, 'an', 'm1_room'); const result = D.attach(s, 'an', { key: 'attach-1', momentId: 'm1', destinations: { roomId: 'dalat' } });
  assert.notEqual(result.placementIds[0], 'm1_room'); assert.equal(D.comments(s, 'chi', 'PHOTO', result.placementIds[0]).length, 0);
});
test('cannot repost another user moment into own group', () => {
  const s = D.seed(); code(() => D.attach(s, 'an', { key: 'not-mine', momentId: 'm2', destinations: { roomId: 'dalat' } }), 'NO_ACCESS');
});
test('private reply does not create a group comment or notification', () => {
  const s = D.seed(), n = s.comments.length, notifications = s.notifications.length; D.send(s, 'chi', { key: 'dm-new', peerId: 'an', referenceId: 'm1_room', text: 'Nhắn riêng nè' }); assert.equal(s.comments.length, n); assert.equal(s.notifications.length, notifications);
});
test('reference cannot bypass access after photo author leaves group', () => {
  const s = D.seed(); D.send(s, 'binh', { key: 'dm-old', peerId: 'an', referenceId: 'm1_room', text: 'Trước lúc rời' }); s.rooms[0].hostId = 'binh'; D.leave(s, 'an', 'dalat');
  code(() => D.send(s, 'binh', { key: 'dm-after', peerId: 'an', referenceId: 'm1_room', text: 'Sau khi rời' }), 'REFERENCE_NOT_AVAILABLE');
});
test('private conversation unique pair, message idempotency and unread watermark', () => {
  const s = D.seed(), input = { key: 'dm-repeat', peerId: 'an', text: 'Xin chào' }; const id = D.send(s, 'binh', input); assert.equal(D.send(s, 'binh', input), id);
  const c = D.conversation(s, 'an', 'binh'); assert.equal(s.conversations.filter(c => c.pair === 'an:binh').length, 1);
  D.markRead(s, 'an', c.id, c.sequence); D.markRead(s, 'an', c.id, 1); assert.equal(c.read.an, c.sequence); assert.equal(D.unread(s, 'an', c), 0);
  code(() => D.messages(s, 'chi', c.id), 'NO_ACCESS');
});
test('block stops new messages but does not erase text history; own recall still works', () => {
  const s = D.seed(), c = D.conversation(s, 'an', 'binh'); D.changeRelation(s, 'an', 'binh', 'block'); code(() => D.send(s, 'binh', { key: 'blocked', peerId: 'an', text: 'Không được' }), 'MESSAGING_NOT_ALLOWED');
  assert(D.messages(s, 'an', c.id).some(m => m.text)); D.recall(s, 'an', 'msg1'); assert.equal(s.messages.find(m => m.id === 'msg1').state, 'RECALLED');
});
test('three-level vote is deterministic upsert, not a toggle', () => {
  const s = D.seed(); D.vote(s, 'an', 'dinner', 'hotpot', 2); D.vote(s, 'an', 'dinner', 'hotpot', 2); assert.equal(D.room(s, 'dinner').ideas[0].votes.an, 2);
  D.vote(s, 'an', 'dinner', 'hotpot', 0); assert.equal(D.room(s, 'dinner').ideas[0].votes.an, 0); code(() => D.submitVotes(s, 'an', 'dinner'), 'VOTES_INCOMPLETE'); D.vote(s, 'an', 'dinner', 'pizza', 1); D.submitVotes(s, 'an', 'dinner');
});
test('host cannot edit another author suggestion and member cannot close phase', () => {
  const s = D.seed(), r = D.room(s, 'chill'); D.addIdea(s, 'linh', r.id, { title: 'Đi bộ', note: '', cost: null, duration: null }); const i = r.ideas[0]; code(() => D.addIdea(s, 'an', r.id, { title: 'Sửa hộ' }, i.id), 'NO_ACCESS'); code(() => D.setPhase(s, 'linh', r.id, 'vote'), 'HOST_REQUIRED');
});
test('Borda tie keeps original shortlist order and rejects incomplete ballots', () => {
  const s = D.seed(), r = s.rooms[0]; D.setPhase(s, 'an', r.id, 'order'); D.submitOrder(s, 'an', r.id, ['coffee', 'lake', 'dinner']); D.submitOrder(s, 'binh', r.id, ['dinner', 'lake', 'coffee']); assert.deepEqual(D.ranked(r).map(x => x.id), ['coffee', 'lake', 'dinner']); code(() => D.submitOrder(s, 'chi', r.id, ['coffee']), 'INVALID_ORDER');
});
test('members add proposals and vote in every active phase without erasing the trip', () => {
  for (const phase of ['ideas', 'vote', 'order', 'plan']) {
    const s = D.seed(), r = D.room(s, 'dalat'); r.phase = phase;
    const order = [...r.order], votes = { ...r.ideas[0].votes }, version = r.planVersion;
    D.addIdea(s, 'binh', r.id, { title: 'Ghé ăn kem trên đường đi', note: '', cost: 35000, duration: 30 });
    const added = r.ideas.at(-1); D.vote(s, 'chi', r.id, added.id, 2); D.vote(s, 'chi', r.id, added.id, 1);
    assert.equal(added.votes.chi, 1); assert.deepEqual(r.order, order); assert.deepEqual(r.ideas[0].votes, votes);
    assert.equal(r.planVersion, version); assert.equal(r.ideas[0].completed, true); assert.equal(r.history.length, 0);
  }
});
test('draft shortlist and Borda ballots do not replace the confirmed itinerary', () => {
  const s = D.seed(), r = D.room(s, 'dalat'), original = [...r.order];
  D.addIdea(s, 'binh', r.id, { title: 'Ăn kem', duration: 30 }); const added = r.ideas.at(-1);
  D.lockShortlist(s, 'an', r.id); D.selectIdea(s, 'an', r.id, added.id); D.lockShortlist(s, 'an', r.id);
  D.submitOrder(s, 'binh', r.id, [...r.selected]);
  assert.deepEqual(r.order, original); assert.equal(r.planVersion, 1); assert.equal(r.ideas[0].completed, true);
  D.publishPlan(s, 'an', r.id, { order: [...r.selected], time: '09:00', reason: '' });
  assert.equal(r.planVersion, 2); assert(r.order.includes(added.id)); assert.equal(r.ideas[0].completed, true);
  assert.equal(added.completed, false); assert.deepEqual(r.history[0].order, original); assert.equal(r.history[0].time, '08:00');
  assert.equal(r.history[0].items[0].completed, true); assert.equal(r.ideas[0].votes.an, 2);
});
test('history freezes old content and completion while new versions are edited', () => {
  const s = D.seed(), r = D.room(s, 'dalat'); D.publishPlan(s, 'an', r.id, { order: [...r.selected], time: '09:00', reason: '' });
  const title = r.history[0].items[0].title; r.ideas[0].title = 'Không được đổi snapshot'; r.ideas[0].completed = false;
  assert.equal(r.history[0].items[0].title, title); assert.equal(r.history[0].items[0].completed, true);
});
test('editing only an unscheduled own proposal clears its votes and invalidates its shortlist ballots', () => {
  const s = D.seed(), r = D.room(s, 'dalat'); D.addIdea(s, 'binh', r.id, { title: 'Ăn kem' }); const added = r.ideas.at(-1);
  D.vote(s, 'an', r.id, added.id, 2); D.lockShortlist(s, 'an', r.id); D.selectIdea(s, 'an', r.id, added.id); D.lockShortlist(s, 'an', r.id); D.submitOrder(s, 'an', r.id, [...r.selected]);
  D.addIdea(s, 'binh', r.id, { title: 'Đổi sang ăn bánh' }, added.id);
  assert.deepEqual(added.votes, {}); assert.deepEqual(r.ballots, {}); assert.equal(r.shortlistLocked, false);
  assert.equal(r.ideas[0].votes.an, 2); assert.equal(r.order.length, 3); assert.equal(r.ideas[0].completed, true);
  code(() => D.addIdea(s, 'chi', r.id, { title: 'Đổi lịch lén' }, 'coffee'), 'IN_CURRENT_PLAN');
  code(() => D.deleteIdea(s, 'binh', r.id, 'lake'), 'IN_CURRENT_PLAN');
});
test('deleting an unscheduled candidate preserves all confirmed stops', () => {
  const s = D.seed(), r = D.room(s, 'dalat'); D.addIdea(s, 'binh', r.id, { title: 'Ăn kem' }); const added = r.ideas.at(-1);
  D.lockShortlist(s, 'an', r.id); D.selectIdea(s, 'an', r.id, added.id); D.deleteIdea(s, 'binh', r.id, added.id);
  assert(!r.selected.includes(added.id)); assert(!r.ideas.includes(added)); assert.deepEqual(r.order, ['coffee', 'lake', 'dinner']);
});
test('only a host may publish an exact valid shortlist, with a reason when overriding Borda', () => {
  const s = D.seed(), r = D.room(s, 'dalat'), version = r.planVersion;
  code(() => D.publishPlan(s, 'binh', r.id, { order: [...r.selected] }), 'HOST_REQUIRED');
  code(() => D.publishPlan(s, 'an', r.id, { order: ['coffee', 'coffee', 'lake'] }), 'INVALID_ORDER');
  code(() => D.publishPlan(s, 'an', r.id, { order: ['coffee'] }), 'INVALID_ORDER');
  code(() => D.publishPlan(s, 'an', r.id, { order: [...r.selected], time: '25:99' }), 'VALIDATION');
  code(() => D.publishPlan(s, 'an', r.id, { order: [...r.selected].reverse(), reason: '' }), 'REASON_REQUIRED');
  assert.equal(r.planVersion, version); assert.equal(r.history.length, 0);
  D.publishPlan(s, 'an', r.id, { order: [...r.selected].reverse(), reason: 'Đổi chặng cho kịp giờ ăn', time: '10:30' });
  assert.equal(r.planVersion, version + 1);
});
test('ended and archived rooms reject all new planning contributions', () => {
  const outside = D.seed();
  code(() => D.addIdea(outside, 'dung', 'dalat', { title: 'Người ngoài phòng' }), 'NO_ACCESS');
  code(() => D.vote(outside, 'dung', 'dalat', 'coffee', 2), 'NO_ACCESS');
  code(() => D.submitOrder(outside, 'dung', 'dalat', ['coffee', 'lake', 'dinner']), 'NO_ACCESS');
  for (const lifecycle of ['ENDED', 'ARCHIVED']) {
    const s = D.seed(), r = D.room(s, 'dalat'); r.lifecycle = lifecycle;
    code(() => D.addIdea(s, 'binh', r.id, { title: 'Ý mới' }), 'INVALID_STATE');
    code(() => D.vote(s, 'binh', r.id, 'coffee', 2), 'INVALID_STATE');
    code(() => D.submitVotes(s, 'an', r.id), 'INVALID_STATE');
    code(() => D.lockShortlist(s, 'an', r.id), 'INVALID_STATE');
    code(() => D.submitOrder(s, 'binh', r.id, [...r.selected]), 'INVALID_ORDER');
    code(() => D.publishPlan(s, 'an', r.id, { order: [...r.selected] }), 'INVALID_ORDER');
  }
});
test('legacy display phases no longer erase votes, completion, ballots or the existing plan', () => {
  const s = D.seed(), r = D.room(s, 'dalat'); D.submitOrder(s, 'binh', r.id, [...r.selected]);
  const before = JSON.stringify({ order: r.order, selected: r.selected, ideas: r.ideas, ballots: r.ballots });
  D.setPhase(s, 'an', r.id, 'ideas'); D.setPhase(s, 'an', r.id, 'vote'); D.setPhase(s, 'an', r.id, 'order');
  assert.equal(JSON.stringify({ order: r.order, selected: r.selected, ideas: r.ideas, ballots: r.ballots }), before);
});
test('integer split conserves money for many amounts and handles a payer outside consumers', () => {
  for (let amount = 1; amount < 500; amount++) { const parts = D.allocations(amount, ['chi', 'an', 'binh']); assert.equal(Object.values(parts).reduce((a, b) => a + b, 0), amount); assert(Math.max(...Object.values(parts)) - Math.min(...Object.values(parts)) <= 1); }
  assert.deepEqual(D.allocations(100, ['chi', 'binh', 'an']), { an: 34, binh: 33, chi: 33 });
  const s = D.seed(), r = D.room(s, 'chill'); D.addExpense(s, 'an', r.id, { title: 'Ứng hộ', amount: 100, payers: { an: 100 }, shares: { linh: 100 } }); assert.deepEqual(D.balances(r), { an: 100, linh: -100 });
});
test('recorded-on-behalf expense does not enter ledger before payer approval', () => {
  const s = D.seed(), r = s.rooms[0], before = D.balances(r), e = D.addExpense(s, 'an', r.id, { title: 'Ghi hộ Chi', amount: 100, payers: { chi: 100 }, shares: { an: 100 } });
  assert.deepEqual(D.balances(r), before); code(() => D.approveExpense(s, 'an', r.id, e.id, true), 'NO_ACCESS'); D.approveExpense(s, 'chi', r.id, e.id, true); D.approveExpense(s, 'chi', r.id, e.id, true); assert.equal(D.balances(r).an, before.an - 100);
});
test('multiple payers supported without weakening sum invariants', () => {
  const s = D.seed(), r = s.rooms[0]; const e = D.addExpense(s, 'an', r.id, { title: 'Cùng ứng', amount: 100, payers: { an: 40, binh: 60 }, shares: { chi: 100 } }); assert.equal(e.state, 'PENDING'); D.approveExpense(s, 'binh', r.id, e.id, true); assert.equal(e.state, 'APPROVED'); assert.equal(Object.values(D.balances(r)).reduce((a, b) => a + b, 0), 0);
  code(() => D.addExpense(s, 'an', r.id, { title: 'Sai tổng', amount: 100, payers: { an: 99 }, shares: { an: 100 } }), 'INVALID_SPLIT');
});
test('pending transfer does not alter balances and only actual recipient confirms once', () => {
  const s = D.seed(), r = s.rooms[0], before = D.balances(r), t = D.settle(s, 'chi', r.id, 'binh', 100);
  assert.deepEqual(D.balances(r), before); code(() => D.settlementAction(s, 'an', r.id, t.id, 'confirm'), 'NO_ACCESS'); D.settlementAction(s, 'binh', r.id, t.id, 'confirm'); D.settlementAction(s, 'binh', r.id, t.id, 'confirm'); assert.equal(D.balances(r).chi, before.chi + 100);
});
test('reversal requires the other participant and restores ledger without erasing history', () => {
  const s = D.seed(), r = s.rooms[0], before = D.balances(r), t = D.settle(s, 'chi', r.id, 'binh', 100); D.settlementAction(s, 'binh', r.id, t.id, 'confirm'); D.settlementAction(s, 'chi', r.id, t.id, 'reverse-request'); code(() => D.settlementAction(s, 'chi', r.id, t.id, 'reverse-confirm'), 'NO_ACCESS'); D.settlementAction(s, 'binh', r.id, t.id, 'reverse-confirm'); assert.deepEqual(D.balances(r), before); assert(r.settlements.includes(t));
});
test('adjustment after settlement waits for all affected parties', () => {
  const s = D.seed(), r = s.rooms[0], t = D.settle(s, 'chi', r.id, 'an', 100); D.settlementAction(s, 'an', r.id, t.id, 'confirm'); const before = D.balances(r);
  D.editExpense(s, 'an', r.id, 'e1', { title: 'Điều chỉnh', amount: 800000, payers: { an: 800000 }, shares: { an: 200000, binh: 200000, chi: 200000, linh: 200000 } }); assert.deepEqual(D.balances(r), before);
  for (const id of ['binh', 'chi', 'linh']) D.approveAdjustment(s, id, r.id, 'e1'); assert.equal(r.expenses[0].amount, 800000); assert.equal(r.expenses[0].history.length, 1); assert.equal(Object.values(D.balances(r)).reduce((a, b) => a + b, 0), 0);
});
test('deleting a room removes group context without deleting personal originals', () => {
  const s = D.seed(); D.lifecycle(s, 'an', 'dalat', 'delete'); assert(!D.visible(s, 'binh', D.placement(s, 'm1_room'))); assert(D.visible(s, 'binh', D.placement(s, 'm1_personal'))); assert.equal(D.moment(s, 'm1').status, 'PUBLISHED');
});
console.log(`\n${passed} passed, ${failed} failed`); if (failed) process.exitCode = 1;
