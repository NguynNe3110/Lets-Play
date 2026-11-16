(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.KeoDomain = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  const HOUR = 3600000;
  const REACTIONS = ['HEART', 'LAUGH', 'WOW', 'FIRE', 'SAD'];
  const fail = (code, message) => { const e = new Error(message); e.code = code; throw e; };
  const check = (ok, code, message) => { if (!ok) fail(code, message); };
  const clean = (value, max, required = true) => {
    const text = String(value || '').normalize('NFC').trim();
    check((!required || text.length > 0) && Array.from(text).length <= max, 'VALIDATION', `Nội dung ${required ? 'cần có từ 1 đến' : 'tối đa'} ${max} ký tự.`);
    return text;
  };
  const now = s => Date.now() + (s.clockOffset || 0);
  const uid = (s, prefix) => `${prefix}_${++s.sequence}`;
  const pair = (a, b) => [a, b].sort().join(':');
  const user = (s, id) => s.users.find(x => x.id === id);
  const room = (s, id) => s.rooms.find(x => x.id === id);
  const moment = (s, id) => s.moments.find(x => x.id === id);
  const placement = (s, id) => s.placements.find(x => x.id === id);
  const active = (s, u) => user(s, u)?.status === 'ACTIVE';
  const blocked = (s, a, b) => s.blocks.some(x => (x.from === a && x.to === b) || (x.from === b && x.to === a));
  const friends = (s, a, b) => s.friends.includes(pair(a, b));
  const epoch = (s, a, b) => s.epochs[pair(a, b)] || 0;
  const member = (s, u, r) => r && r.lifecycle !== 'DELETED' && r.members.includes(u);
  const host = (s, u, r) => member(s, u, r) && r.hostId === u;
  function requireMember(s, u, r) { check(active(s, u) && member(s, u, r), 'NO_ACCESS', 'Bạn không còn quyền truy cập phòng này.'); }
  function requireHost(s, u, r) { requireMember(s, u, r); check(host(s, u, r), 'HOST_REQUIRED', 'Chỉ chủ phòng được thực hiện thao tác này.'); }
  function writable(r, time) {
    if (!r || r.lifecycle === 'DELETED') return { open: false, reason: 'Phòng không còn khả dụng', deadline: null };
    if (r.lifecycle === 'ARCHIVED') return { open: false, reason: 'Phòng đã được lưu trữ chung', deadline: null };
    if (r.lifecycle === 'ACTIVE') return { open: true, reason: '', deadline: null };
    if (r.policy.mode === 'OPEN') return { open: true, reason: '', deadline: null };
    if (r.policy.mode === 'VIEW_ONLY') return { open: false, reason: 'Chủ phòng đã đặt chế độ chỉ xem lại', deadline: null };
    const deadline = r.endedAt + r.policy.windowHours * HOUR;
    return { open: time < deadline, reason: time < deadline ? '' : 'Album đã hết thời gian đóng góp', deadline };
  }
  function requireWrite(s, u, r) {
    requireMember(s, u, r);
    const w = writable(r, now(s));
    check(w.open, w.deadline ? 'ALBUM_WINDOW_EXPIRED' : 'ALBUM_READ_ONLY', w.reason);
  }
  function visible(s, u, p) {
    if (!active(s, u) || !p || p.status !== 'ACTIVE') return false;
    const m = moment(s, p.momentId);
    if (!m || m.status !== 'PUBLISHED' || (u !== m.ownerId && blocked(s, u, m.ownerId))) return false;
    if (p.kind === 'ROOM') return member(s, u, room(s, p.roomId));
    return u === m.ownerId || p.grants.some(g => g.userId === u && g.epoch === epoch(s, u, m.ownerId) && friends(s, u, m.ownerId));
  }
  function ownMoments(s, u) { return s.moments.filter(m => m.ownerId === u && m.status === 'PUBLISHED').sort((a, b) => b.createdAt - a.createdAt); }
  function photos(s, u, roomId) { return s.placements.filter(p => (roomId ? p.kind === 'ROOM' && p.roomId === roomId : p.kind === 'PERSONAL') && visible(s, u, p)).sort((a, b) => b.createdAt - a.createdAt); }
  function canMessage(s, a, b) { return a !== b && active(s, a) && active(s, b) && !blocked(s, a, b) && (friends(s, a, b) || s.rooms.some(r => member(s, a, r) && member(s, b, r))); }
  function note(s, recipients, title, targetType, targetId) {
    [...new Set(recipients)].forEach(u => s.notifications.unshift({ id: uid(s, 'notice'), userId: u, title, targetType, targetId, createdAt: now(s), read: false }));
  }
  function command(s, u, key, payload, fn) {
    check(active(s, u), 'AUTH_REQUIRED', 'Tài khoản không còn hoạt động.');
    const id = `${u}:${key}`, fingerprint = JSON.stringify(payload), old = s.mutations[id];
    if (old) { check(old.fingerprint === fingerprint, 'IDEMPOTENCY_KEY_REUSED', 'Thao tác đã được dùng với nội dung khác.'); return old.result; }
    const result = fn(); s.mutations[id] = { fingerprint, result }; return result;
  }
  function audience(s, u, mode, selected = []) {
    check(['OWNER', 'ALL_FRIENDS_SNAPSHOT', 'SELECTED_FRIENDS'].includes(mode), 'VALIDATION', 'Chọn đối tượng chia sẻ.');
    const recipients = mode === 'OWNER' ? [] : mode === 'ALL_FRIENDS_SNAPSHOT' ? s.users.filter(v => v.id !== u && active(s, v.id) && friends(s, u, v.id) && !blocked(s, u, v.id)).map(v => v.id) : [...new Set(selected)];
    check(mode === 'OWNER' || (recipients.length > 0 && recipients.length <= 200), 'AUDIENCE_EMPTY', 'Hãy chọn ít nhất một người bạn, hoặc Chỉ mình tôi.');
    recipients.forEach(v => check(v !== u && active(s, v) && friends(s, u, v) && !blocked(s, u, v), 'AUDIENCE_CHANGED', 'Danh sách bạn bè đã thay đổi. Hãy chọn lại người nhận.'));
    return { mode, recipients: recipients.map(v => ({ userId: v, epoch: epoch(s, u, v) })), ownerId: u, expiresAt: now(s) + 600000 };
  }
  function validateDestinations(s, u, destinations, existingMoment) {
    check(destinations && (destinations.personal || destinations.roomId), 'NO_DESTINATION', 'Chọn ít nhất một nơi đăng ảnh.');
    if (destinations.roomId) requireWrite(s, u, room(s, destinations.roomId));
    if (destinations.personal) {
      const a = destinations.personal;
      check(['OWNER', 'ALL_FRIENDS_SNAPSHOT', 'SELECTED_FRIENDS'].includes(a.mode) && Array.isArray(a.recipients) && (a.mode === 'OWNER' ? a.recipients.length === 0 : a.recipients.length > 0 && a.recipients.length <= 200), 'VALIDATION', 'Đối tượng chia sẻ không hợp lệ.');
      check(a.ownerId === u && a.expiresAt > now(s), 'AUDIENCE_CHANGED', 'Hãy xác nhận lại người nhận trước khi đăng.');
      a.recipients.forEach(g => check(active(s, g.userId) && friends(s, u, g.userId) && !blocked(s, u, g.userId) && g.epoch === epoch(s, u, g.userId), 'AUDIENCE_CHANGED', 'Người nhận đã thay đổi. Ảnh chưa được đăng.'));
    }
    if (existingMoment) {
      const places = s.placements.filter(p => p.momentId === existingMoment.id && p.status === 'ACTIVE');
      check(!(destinations.personal && places.some(p => p.kind === 'PERSONAL')) && !(destinations.roomId && places.some(p => p.kind === 'ROOM')), 'PLACEMENT_ALREADY_EXISTS', 'Ảnh đã có nơi đăng này. Gỡ nơi cũ trước khi chọn lại.');
    }
  }
  function insertPlaces(s, u, m, destinations) {
    const places = [];
    if (destinations.personal) places.push({ id: uid(s, 'place'), momentId: m.id, kind: 'PERSONAL', audienceMode: destinations.personal.mode, grants: destinations.personal.recipients, status: 'ACTIVE', createdAt: now(s) });
    if (destinations.roomId) places.push({ id: uid(s, 'place'), momentId: m.id, kind: 'ROOM', roomId: destinations.roomId, roomNameAtPlacement: room(s, destinations.roomId).name, grants: [], status: 'ACTIVE', createdAt: now(s) });
    s.placements.push(...places);
    const recipients = s.users.filter(v => v.id !== u && places.some(p => visible(s, v.id, p)));
    recipients.forEach(v => { const p = places.find(p => p.kind === 'PERSONAL' && visible(s, v.id, p)) || places.find(p => visible(s, v.id, p)); note(s, [v.id], `${user(s, u).name} vừa chia sẻ một khoảnh khắc`, 'PHOTO', p.id); });
    return places;
  }
  function publish(s, u, input) {
    return command(s, u, input.key, input, () => {
      const caption = clean(input.caption, 280, false);
      validateDestinations(s, u, input.destinations);
      check(input.scene || /^data:image\/jpeg;base64,/.test(input.imageUri || ''), 'INVALID_IMAGE', 'Chụp ảnh trước khi đăng.');
      const m = { id: uid(s, 'moment'), ownerId: u, caption, scene: input.scene || 'forest', imageUri: input.imageUri || null, createdAt: now(s), status: 'PUBLISHED' };
      s.moments.push(m); const places = insertPlaces(s, u, m, input.destinations);
      return { momentId: m.id, placementIds: places.map(p => p.id) };
    });
  }
  function attach(s, u, input) {
    return command(s, u, input.key, input, () => {
      const m = moment(s, input.momentId); check(m && m.ownerId === u && m.status === 'PUBLISHED', 'NO_ACCESS', 'Chỉ được thêm ảnh do chính bạn sở hữu.');
      validateDestinations(s, u, input.destinations, m);
      return { momentId: m.id, placementIds: insertPlaces(s, u, m, input.destinations).map(p => p.id) };
    });
  }
  function removePlace(s, u, id, reason) {
    const p = placement(s, id), m = p && moment(s, p.momentId);
    check(m && (m.ownerId === u || (p.kind === 'ROOM' && host(s, u, room(s, p.roomId)) && visible(s, u, p))), 'NO_ACCESS', 'Bạn không có quyền gỡ ảnh này.');
    if (m.ownerId !== u) clean(reason, 500);
    p.status = 'REMOVED'; p.removedBy = u;
  }
  function deleteMoment(s, u, id) { const m = moment(s, id); check(m?.ownerId === u, 'NO_ACCESS', 'Chỉ tác giả được xóa ảnh ở mọi nơi.'); m.status = 'DELETED'; s.placements.filter(p => p.momentId === id).forEach(p => { p.status = 'REMOVED'; }); }
  function react(s, u, id, type) {
    const old = s.reactions.find(x => x.placementId === id && x.userId === u);
    if (old && old.type === type) { s.reactions = s.reactions.filter(x => x !== old); return; }
    const p = placement(s, id); check(visible(s, u, p), 'NO_ACCESS', 'Ảnh không còn khả dụng.');
    if (p.kind === 'ROOM') requireWrite(s, u, room(s, p.roomId));
    check(REACTIONS.includes(type), 'VALIDATION', 'Cảm xúc không hợp lệ.');
    if (old) old.type = type; else s.reactions.push({ placementId: id, userId: u, type });
  }
  function comment(s, u, input) {
    return command(s, u, input.key, input, () => {
      const text = clean(input.text, 1000);
      let r;
      if (input.kind === 'PHOTO') { const p = placement(s, input.targetId); check(p?.kind === 'ROOM' && visible(s, u, p), 'NO_ACCESS', 'Chỉ ảnh nhóm có bình luận chung.'); r = room(s, p.roomId); }
      else r = room(s, input.targetId);
      requireWrite(s, u, r);
      const c = { id: uid(s, 'comment'), kind: input.kind, targetId: input.targetId, authorId: u, text, createdAt: now(s), state: 'ACTIVE' }; s.comments.push(c);
      const recipients = input.kind === 'PHOTO' ? [moment(s, placement(s, input.targetId).momentId).ownerId].filter(v => v !== u && visible(s, v, placement(s, input.targetId))) : r.members.filter(v => v !== u && !blocked(s, u, v));
      note(s, recipients, `${user(s, u).name} đã bình luận ${input.kind === 'PHOTO' ? 'ảnh nhóm' : 'album'}`, input.kind === 'PHOTO' ? 'PHOTO' : 'ALBUM', input.targetId);
      return c.id;
    });
  }
  function comments(s, u, kind, targetId) {
    const allowed = kind === 'PHOTO' ? visible(s, u, placement(s, targetId)) : member(s, u, room(s, targetId));
    if (!allowed) return [];
    return s.comments.filter(c => c.kind === kind && c.targetId === targetId && c.state === 'ACTIVE' && active(s, c.authorId) && !blocked(s, u, c.authorId));
  }
  function deleteComment(s, u, id, reason) {
    const c = s.comments.find(x => x.id === id); check(c, 'NO_ACCESS', 'Bình luận không còn khả dụng.');
    const p = c.kind === 'PHOTO' && placement(s, c.targetId), r = room(s, p ? p.roomId : c.targetId);
    check(c.authorId === u || (host(s, u, r) && (p ? visible(s, u, p) : member(s, u, r))), 'NO_ACCESS', 'Bạn không có quyền xóa bình luận.');
    if (c.authorId !== u) clean(reason, 500); c.state = 'DELETED'; c.text = '';
  }
  function setPolicy(s, u, roomId, mode, windowHours) {
    const r = room(s, roomId); requireHost(s, u, r); check(r.lifecycle !== 'ARCHIVED', 'ROOM_ARCHIVED', 'Khôi phục phòng trước khi đổi thiết lập.');
    check(['OPEN', 'VIEW_ONLY', 'WINDOWED'].includes(mode) && (mode !== 'WINDOWED' || (Number.isInteger(windowHours) && windowHours >= 1 && windowHours <= 720)), 'VALIDATION', 'Thời hạn phải là số giờ nguyên từ 1 đến 720.');
    r.policy = { mode, windowHours: mode === 'WINDOWED' ? windowHours : null }; r.revision++;
    note(s, r.members.filter(v => v !== u), 'Chủ phòng đã thay đổi thiết lập album', 'ALBUM', r.id);
  }
  function lifecycle(s, u, roomId, action) {
    const r = room(s, roomId); requireHost(s, u, r);
    if (action === 'end') { check(r.planVersion > 0 && r.lifecycle === 'ACTIVE', 'INVALID_STATE', 'Chốt kế hoạch trước khi kết thúc kèo.'); r.lifecycle = 'ENDED'; r.endedAt = now(s); }
    else if (action === 'reopen') { check(r.lifecycle === 'ENDED', 'INVALID_STATE', 'Kèo chưa ở trạng thái kết thúc.'); r.history.push({ type: 'END', endedAt: r.endedAt }); r.lifecycle = 'ACTIVE'; r.endedAt = null; }
    else if (action === 'archive') { check(['ACTIVE', 'ENDED'].includes(r.lifecycle), 'INVALID_STATE', 'Không thể lưu trữ trạng thái này.'); r.previousLifecycle = r.lifecycle; r.lifecycle = 'ARCHIVED'; }
    else if (action === 'restore') { check(r.lifecycle === 'ARCHIVED', 'INVALID_STATE', 'Phòng chưa lưu trữ.'); r.lifecycle = r.previousLifecycle; }
    else if (action === 'delete') { r.lifecycle = 'DELETED'; s.placements.filter(p => p.roomId === r.id).forEach(p => { p.status = 'REMOVED'; }); }
    else fail('VALIDATION', 'Thao tác không hợp lệ.'); r.revision++;
  }
  function leave(s, u, roomId, removeUserId) {
    const r = room(s, roomId); requireMember(s, u, r); const target = removeUserId || u;
    if (removeUserId) requireHost(s, u, r);
    check(target !== r.hostId, 'HOST_TRANSFER_REQUIRED', 'Chuyển quyền chủ phòng trước khi rời kèo.'); r.members = r.members.filter(v => v !== target);
  }
  function changeRelation(s, u, v, action) {
    check(u !== v && user(s, v), 'VALIDATION', 'Người dùng không hợp lệ.');
    if (action === 'unfriend') { s.friends = s.friends.filter(x => x !== pair(u, v)); s.epochs[pair(u, v)] = epoch(s, u, v) + 1; }
    else if (action === 'block') { if (!s.blocks.some(x => x.from === u && x.to === v)) { s.blocks.push({ from: u, to: v }); s.epochs[pair(u, v)] = epoch(s, u, v) + 1; } }
    else if (action === 'unblock') s.blocks = s.blocks.filter(x => !(x.from === u && x.to === v));
    else if (action === 'request') { check(!blocked(s, u, v), 'NO_ACCESS', 'Không thể gửi lời mời.'); if (!s.friendRequests.some(x => x.from === u && x.to === v)) s.friendRequests.push({ from: u, to: v }); }
    else if (action === 'accept') { check(!blocked(s, u, v) && s.friendRequests.some(x => x.from === v && x.to === u), 'NO_ACCESS', 'Không có lời mời phù hợp.'); if (!friends(s, u, v)) s.friends.push(pair(u, v)); s.friendRequests = s.friendRequests.filter(x => !(x.from === v && x.to === u)); }
  }
  function conversation(s, a, b, create = false) {
    let c = s.conversations.find(x => x.pair === pair(a, b));
    if (!c && create) { check(canMessage(s, a, b), 'MESSAGING_NOT_ALLOWED', 'Bạn cần là bạn bè hoặc cùng một phòng để nhắn riêng.'); c = { id: uid(s, 'chat'), pair: pair(a, b), users: [a, b], read: { [a]: 0, [b]: 0 }, sequence: 0, updatedAt: now(s) }; s.conversations.push(c); }
    return c;
  }
  function send(s, u, input) {
    return command(s, u, input.key, input, () => {
      const text = clean(input.text, 2000); check(canMessage(s, u, input.peerId), 'MESSAGING_NOT_ALLOWED', 'Bạn không thể gửi tin nhắn mới cho người này.');
      if (input.referenceId) { const p = placement(s, input.referenceId); check(visible(s, u, p) && visible(s, input.peerId, p), 'REFERENCE_NOT_AVAILABLE', 'Ảnh không còn được cả hai xem. Hãy bỏ tham chiếu nếu muốn gửi riêng đoạn chữ.'); }
      const c = conversation(s, u, input.peerId, true); const m = { id: uid(s, 'message'), conversationId: c.id, sequence: ++c.sequence, senderId: u, text, referenceId: input.referenceId || null, state: 'SENT', createdAt: now(s) }; s.messages.push(m); c.updatedAt = m.createdAt; return m.id;
    });
  }
  function messages(s, u, id) { const c = s.conversations.find(x => x.id === id); check(c?.users.includes(u), 'NO_ACCESS', 'Hội thoại không khả dụng.'); return s.messages.filter(m => m.conversationId === id); }
  function markRead(s, u, id, seq) { const c = s.conversations.find(x => x.id === id); check(c?.users.includes(u) && seq <= c.sequence, 'NO_ACCESS', 'Không thể cập nhật trạng thái đọc.'); c.read[u] = Math.max(c.read[u] || 0, seq); }
  function recall(s, u, id) { const m = s.messages.find(x => x.id === id); check(m?.senderId === u, 'NO_ACCESS', 'Chỉ được thu hồi tin của mình.'); m.state = 'RECALLED'; m.text = ''; m.referenceId = null; }
  function unread(s, u, c) { return s.messages.filter(m => m.conversationId === c.id && m.senderId !== u && m.state === 'SENT' && m.sequence > (c.read[u] || 0)).length; }
  function createRoom(s, u, name, region) {
    const id = uid(s, 'room'); const r = baseRoom(id, clean(name, 100), u, [u], 'ideas'); r.region = clean(region, 120, false); r.date = ''; r.time = ''; r.code = `KE${String(s.sequence).padStart(4, '0')}`; s.rooms.unshift(r); return r;
  }
  function joinRoom(s, u, code) { const r = s.rooms.find(r => r.code === String(code).trim().toUpperCase() && r.lifecycle !== 'DELETED'); check(r && !r.inviteLocked, 'INVITE_INVALID', 'Mã phòng không đúng hoặc đã khóa.'); if (!r.members.includes(u)) r.members.push(u); return r; }
  function addIdea(s, u, roomId, input, editId) {
    const r = room(s, roomId); requireMember(s, u, r); check(r.lifecycle === 'ACTIVE', 'INVALID_STATE', 'Kèo đã kết thúc hoặc lưu trữ, đề xuất hiện chỉ đọc.');
    const data = { title: clean(input.title, 100), note: clean(input.note, 500, false), cost: input.cost == null ? null : Number(input.cost), duration: input.duration == null ? null : Number(input.duration) };
    check((data.cost === null || (Number.isInteger(data.cost) && data.cost >= 0)) && (data.duration === null || (Number.isInteger(data.duration) && data.duration > 0)), 'VALIDATION', 'Chi phí là số đồng nguyên; thời lượng là số phút dương.');
    if (editId) {
      const old = r.ideas.find(i => i.id === editId); check(old?.ownerId === u, 'NO_ACCESS', 'Chỉ sửa ý tưởng của chính bạn.');
      check(!(r.planVersion > 0 && r.order.includes(editId)), 'IN_CURRENT_PLAN', 'Ý này đang trong lịch chung. Hãy thêm đề xuất mới hoặc cập nhật lịch trước khi sửa.');
      Object.assign(old, data, { votes: {}, endorsements: [] });
      if (r.selected.includes(editId)) { r.ballots = {}; r.shortlistLocked = false; }
    }
    else r.ideas.push({ ...data, id: uid(s, 'idea'), ownerId: u, endorsements: [], votes: {}, completed: false });
    r.submitted = []; r.revision++;
  }
  function deleteIdea(s, u, rid, iid) {
    const r = room(s, rid); requireMember(s, u, r); const i = r.ideas.find(x => x.id === iid);
    check(i?.ownerId === u && r.lifecycle === 'ACTIVE', 'NO_ACCESS', 'Chỉ xóa ý của mình khi kèo còn hoạt động.');
    check(!(r.planVersion > 0 && r.order.includes(iid)), 'IN_CURRENT_PLAN', 'Ý này đang trong lịch chung, không thể xóa trực tiếp.');
    r.ideas = r.ideas.filter(x => x.id !== iid);
    if (r.selected.includes(iid)) { r.selected = r.selected.filter(id => id !== iid); r.ballots = {}; r.shortlistLocked = false; }
    r.submitted = []; r.revision++;
  }
  function vote(s, u, rid, iid, value) { const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle === 'ACTIVE' && [0, 1, 2].includes(value), 'INVALID_STATE', 'Chỉ bình chọn khi kèo còn hoạt động.'); const i = r.ideas.find(x => x.id === iid); check(i, 'NO_ACCESS', 'Ý tưởng không còn tồn tại.'); i.votes[u] = value; r.submitted = r.submitted.filter(v => v !== u); r.revision++; }
  function submitVotes(s, u, rid) { const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle === 'ACTIVE', 'INVALID_STATE', 'Kèo hiện chỉ đọc.'); check(r.ideas.length && r.ideas.every(i => i.votes[u] !== undefined), 'VOTES_INCOMPLETE', 'Chọn đủ tất cả ý tưởng trước khi đánh dấu đã xem hết.'); if (!r.submitted.includes(u)) r.submitted.push(u); }
  function setPhase(s, u, rid, phase) {
    const r = room(s, rid); requireHost(s, u, r); check(r.lifecycle === 'ACTIVE', 'INVALID_STATE', 'Mở lại kèo trước khi đổi kế hoạch.');
    check(['ideas', 'vote', 'order', 'plan'].includes(phase), 'INVALID_STATE', 'Trạng thái kế hoạch không hợp lệ.');
    // Legacy phase is a display hint, never a gate on contribution or a command to erase a plan.
    if (phase === 'plan') return publishPlan(s, u, rid, { order: ranked(r).map(x => x.id), time: r.time, reason: '' });
    r.phase = phase; r.revision++;
  }
  function selectIdea(s, u, rid, iid) { const r = room(s, rid); requireHost(s, u, r); check(r.lifecycle === 'ACTIVE' && !r.shortlistLocked, 'INVALID_STATE', 'Mở chỉnh danh sách trước khi thay hoạt động.'); check(r.ideas.some(i => i.id === iid), 'VALIDATION', 'Ý tưởng không tồn tại.'); r.selected = r.selected.includes(iid) ? r.selected.filter(x => x !== iid) : [...r.selected, iid]; r.ballots = {}; r.revision++; }
  function lockShortlist(s, u, rid) { const r = room(s, rid); requireHost(s, u, r); check(r.lifecycle === 'ACTIVE' && r.selected.length > 0, 'INVALID_STATE', 'Chọn ít nhất một hoạt động.'); r.shortlistLocked = !r.shortlistLocked; r.ballots = {}; r.revision++; }
  function submitOrder(s, u, rid, order) { const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle === 'ACTIVE' && r.shortlistLocked && order.length === r.selected.length && new Set(order).size === order.length && order.every(id => r.selected.includes(id)), 'INVALID_ORDER', 'Thứ tự cần đủ các hoạt động và không trùng.'); r.ballots[u] = [...order]; }
  function publishPlan(s, u, rid, input) {
    const r = room(s, rid); requireHost(s, u, r); const order = input.order;
    check(r.lifecycle === 'ACTIVE' && r.shortlistLocked && Array.isArray(order) && order.length > 0 && order.length === r.selected.length && new Set(order).size === order.length && order.every(id => r.selected.includes(id) && r.ideas.some(i => i.id === id)), 'INVALID_ORDER', 'Chốt đủ danh sách đã chọn, không trùng và khi kèo còn hoạt động.');
    const reason = clean(input.reason || '', 500, false), time = input.time || '';
    check(!time || /^([01]\d|2[0-3]):[0-5]\d$/.test(time), 'VALIDATION', 'Giờ bắt đầu không hợp lệ.');
    check(JSON.stringify(order) === JSON.stringify(ranked(r).map(x => x.id)) || reason.length > 0, 'REASON_REQUIRED', 'Ghi lý do nếu lịch khác thứ tự gợi ý.');
    if (r.planVersion > 0) r.history.push({ type: 'PLAN', version: r.planVersion, order: [...r.order], time: r.time, reason: r.planOverrideReason || '', items: JSON.parse(JSON.stringify(r.order.map(id => r.ideas.find(i => i.id === id)).filter(Boolean))) });
    r.ideas.forEach(i => { if (order.includes(i.id) && !r.order.includes(i.id)) { i.completed = false; delete i.completedBy; } });
    r.order = [...order]; r.time = time; r.planOverrideReason = reason; r.planVersion++; r.phase = 'plan'; r.revision++;
    note(s, r.members.filter(id => id !== u), `Lịch chung được cập nhật: phiên bản ${r.planVersion}`, 'ALBUM', r.id);
    return r;
  }
  function ranked(r) { return r.selected.map((id, original) => ({ id, original, score: Object.values(r.ballots).reduce((sum, list) => sum + list.length - 1 - list.indexOf(id), 0) })).sort((a, b) => b.score - a.score || a.original - b.original); }
  function allocations(amount, participants) { check(Number.isInteger(amount) && amount > 0 && amount <= 100000000 && participants.length, 'INVALID_AMOUNT', 'Nhập số tiền nguyên từ 1 đến 100.000.000 đồng và người tham gia.'); const ids = [...new Set(participants)].sort(); return Object.fromEntries(ids.map((id, index) => [id, Math.floor(amount / ids.length) + (index < amount % ids.length ? 1 : 0)])); }
  function validateExpense(r, input) {
    const title = clean(input.title, 160), amount = Number(input.amount), payers = input.payers, shares = input.shares;
    check(Number.isInteger(amount) && amount > 0 && amount <= 100000000, 'INVALID_AMOUNT', 'Số tiền phải là số đồng nguyên từ 1 đến 100.000.000.');
    for (const map of [payers, shares]) check(map && Object.keys(map).length && Object.entries(map).every(([u, n]) => r.members.includes(u) && Number.isInteger(n) && n >= 0) && Object.values(map).reduce((a, b) => a + b, 0) === amount, 'INVALID_SPLIT', 'Tổng người trả và tổng phần chia đều phải bằng tổng khoản chi.');
    return { title, amount, payers, shares, note: clean(input.note, 1000, false) };
  }
  function addExpense(s, u, rid, input) {
    const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle !== 'ARCHIVED', 'ROOM_ARCHIVED', 'Phòng lưu trữ chỉ đọc.'); const data = validateExpense(r, input);
    const required = Object.keys(data.payers).filter(v => data.payers[v] > 0); const e = { ...data, id: uid(s, 'expense'), createdBy: u, approvals: required.includes(u) ? [u] : [], required, state: required.every(v => v === u) ? 'APPROVED' : 'PENDING', createdAt: now(s), history: [] }; r.expenses.push(e); return e;
  }
  function approveExpense(s, u, rid, eid, approve) { const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle !== 'ARCHIVED', 'ROOM_ARCHIVED', 'Phòng lưu trữ chỉ đọc.'); const e = r.expenses.find(x => x.id === eid); if (approve && e?.state === 'APPROVED' && e.approvals.includes(u)) return; check(e?.required.includes(u) && e.state === 'PENDING', 'NO_ACCESS', 'Chỉ người trả liên quan được xác nhận khoản ghi hộ.'); if (!approve) { e.state = 'REJECTED'; return; } if (!e.approvals.includes(u)) e.approvals.push(u); if (e.required.every(v => e.approvals.includes(v))) e.state = 'APPROVED'; }
  function balances(r) {
    const ids = [...new Set([...r.members, ...r.expenses.flatMap(e => [...Object.keys(e.payers), ...Object.keys(e.shares)]), ...r.settlements.flatMap(t => [t.from, t.to])])]; const out = Object.fromEntries(ids.map(id => [id, 0]));
    r.expenses.filter(e => e.state === 'APPROVED').forEach(e => { Object.entries(e.payers).forEach(([id, n]) => { out[id] += n; }); Object.entries(e.shares).forEach(([id, n]) => { out[id] -= n; }); });
    r.settlements.filter(t => t.state === 'CONFIRMED').forEach(t => { out[t.from] += t.amount; out[t.to] -= t.amount; }); return out;
  }
  function debts(r) { const b = balances(r), debt = Object.entries(b).filter(([, n]) => n < 0).map(([id, n]) => ({ id, amount: -n })), credit = Object.entries(b).filter(([, n]) => n > 0).map(([id, n]) => ({ id, amount: n })), out = []; debt.forEach(d => credit.forEach(c => { const amount = Math.min(d.amount, c.amount); if (amount) { out.push({ from: d.id, to: c.id, amount }); d.amount -= amount; c.amount -= amount; } })); return out; }
  function settle(s, u, rid, to, amount) { const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle !== 'ARCHIVED' && to !== u && r.members.includes(to) && Number.isInteger(amount) && amount > 0, 'INVALID_AMOUNT', 'Chọn người nhận khác bạn và số tiền nguyên dương.'); check(!r.settlements.some(t => t.state === 'PENDING' && t.from === u && t.to === to && t.amount === amount), 'DUPLICATE', 'Đã có khoản giống hệt đang chờ xác nhận.'); const t = { id: uid(s, 'transfer'), from: u, to, amount, state: 'PENDING', createdAt: now(s) }; r.settlements.push(t); return t; }
  function settlementAction(s, u, rid, id, action) {
    const r = room(s, rid); requireMember(s, u, r); check(r.lifecycle !== 'ARCHIVED' || action === 'cancel', 'ROOM_ARCHIVED', 'Phòng lưu trữ chỉ đọc.'); const t = r.settlements.find(x => x.id === id); check(t, 'NO_ACCESS', 'Khoản thanh toán không tồn tại.');
    if (['confirm', 'reject'].includes(action)) { check(t.to === u, 'NO_ACCESS', 'Chỉ người nhận được xác nhận hoặc từ chối.'); if (t.state === 'CONFIRMED' && action === 'confirm') return; check(t.state === 'PENDING', 'INVALID_STATE', 'Khoản này không còn chờ.'); t.state = action === 'confirm' ? 'CONFIRMED' : 'REJECTED'; }
    else if (action === 'cancel') { check(t.from === u && t.state === 'PENDING', 'NO_ACCESS', 'Chỉ người gửi hủy khoản đang chờ.'); t.state = 'CANCELLED'; }
    else if (action === 'reverse-request') { check([t.from, t.to].includes(u) && t.state === 'CONFIRMED', 'NO_ACCESS', 'Chỉ hai bên được yêu cầu đảo.'); t.reverseRequestedBy = u; }
    else if (action === 'reverse-confirm') { check([t.from, t.to].includes(u) && t.reverseRequestedBy && t.reverseRequestedBy !== u && t.state === 'CONFIRMED', 'NO_ACCESS', 'Bên còn lại cần xác nhận yêu cầu đảo.'); t.state = 'REVERSED'; }
  }
  function editExpense(s, u, rid, eid, input) {
    const r = room(s, rid); requireMember(s, u, r); const e = r.expenses.find(x => x.id === eid); check(e?.createdBy === u && e.state === 'APPROVED' && r.lifecycle !== 'ARCHIVED', 'NO_ACCESS', 'Chỉ người tạo sửa khoản của mình trong phòng đang mở.'); const data = validateExpense(r, input);
    const affected = [...new Set([...Object.keys(e.payers), ...Object.keys(e.shares), ...Object.keys(data.payers), ...Object.keys(data.shares)])];
    if (r.settlements.some(t => ['CONFIRMED', 'REVERSED'].includes(t.state)) || Object.keys(data.payers).some(v => v !== u && data.payers[v] > 0)) e.adjustment = { data, required: affected, approvals: affected.includes(u) ? [u] : [] };
    else { e.history.push({ ...e, history: undefined, adjustment: undefined }); Object.assign(e, data); }
  }
  function approveAdjustment(s, u, rid, eid) { const r = room(s, rid); requireMember(s, u, r); const e = r.expenses.find(x => x.id === eid), a = e?.adjustment; check(a?.required.includes(u), 'NO_ACCESS', 'Bạn không thuộc các bên cần xác nhận điều chỉnh.'); if (!a.approvals.includes(u)) a.approvals.push(u); if (a.required.every(v => a.approvals.includes(v))) { e.history.push({ ...e, history: undefined, adjustment: undefined }); Object.assign(e, a.data); delete e.adjustment; } }
  function baseRoom(id, name, hostId, members, phase) { return { id, name, code: id === 'dalat' ? 'DALAT26' : id === 'dinner' ? 'TOINAY' : id === 'sea' ? 'BIEN26' : 'CHILL26', hostId, members, phase, lifecycle: 'ACTIVE', policy: { mode: 'OPEN', windowHours: null }, endedAt: null, revision: 1, region: 'TP. Hồ Chí Minh', date: '2026-11-14', time: '08:00', ideas: [], submitted: [], selected: [], order: [], ballots: {}, shortlistLocked: false, planVersion: 0, history: [], expenses: [], settlements: [], notes: [], availability: [], availabilityVotes: {}, slotComments: [], attendance: {}, rides: {}, preferences: {}, calendarLinks: {} }; }
  function seed() {
    const time = Date.now(); const s = { version: 1, sequence: 1000, currentUser: 'an', clockOffset: 0, online: true, users: [{ id: 'an', name: 'An', color: '#DCE9DA', status: 'ACTIVE' }, { id: 'binh', name: 'Bình', color: '#F6DED0', status: 'ACTIVE' }, { id: 'chi', name: 'Chi', color: '#E5DFF0', status: 'ACTIVE' }, { id: 'dung', name: 'Dũng', color: '#DCE8F0', status: 'ACTIVE' }, { id: 'linh', name: 'Linh', color: '#F4E5B9', status: 'ACTIVE' }, { id: 'nam', name: 'Nam', color: '#DFE3D8', status: 'ACTIVE' }], friends: ['an:binh', 'an:dung', 'an:linh', 'binh:chi', 'binh:linh'], epochs: {}, blocks: [], friendRequests: [], rooms: [], moments: [], placements: [], reactions: [], comments: [], conversations: [], messages: [], notifications: [], reports: [], mutations: {}, draft: null };
    const r = baseRoom('dalat', 'Đà Lạt, mình đi thôi', 'an', ['an', 'binh', 'chi', 'linh'], 'plan'); r.region = 'Đà Lạt'; r.planVersion = 1; r.policy = { mode: 'WINDOWED', windowHours: 72 };
    r.ideas = [{ id: 'coffee', title: 'Ngồi cà phê dưới tán thông', note: 'Một buổi sáng thật chậm, không cần dậy quá sớm.', cost: 75000, duration: 90, ownerId: 'chi', endorsements: [], votes: { an: 2, binh: 1, chi: 2, linh: 2 }, completed: true }, { id: 'lake', title: 'Dạo quanh hồ Tuyền Lâm', note: 'Mang theo máy ảnh, mình đi bộ một chút nhé.', cost: 0, duration: 60, ownerId: 'binh', endorsements: [], votes: { an: 2, binh: 2, chi: 1, linh: 2 }, completed: false }, { id: 'dinner', title: 'Lẩu gà lá é cho cả nhóm', note: 'Chi không ăn cay, nhớ gọi phần nước dùng riêng.', cost: 150000, duration: 90, ownerId: 'an', endorsements: [], votes: { an: 2, binh: 2, chi: 2, linh: 1 }, completed: false }]; r.selected = r.ideas.map(i => i.id); r.order = [...r.selected]; r.shortlistLocked = true; r.submitted = [...r.members];
    r.notes = [{ id: 'note1', ownerId: 'chi', text: 'Mình không ăn cay, cần về trước 22:00.', anonymous: true, handled: false }];
    r.expenses = [{ id: 'e1', title: 'Bữa trưa của cả nhóm', amount: 600000, payers: { an: 600000 }, shares: { an: 150000, binh: 150000, chi: 150000, linh: 150000 }, note: '', createdBy: 'an', state: 'APPROVED', required: ['an'], approvals: ['an'], history: [], createdAt: time - HOUR * 2 }, { id: 'e2', title: 'Cà phê sáng', amount: 280000, payers: { binh: 280000 }, shares: { an: 70000, binh: 70000, chi: 70000, linh: 70000 }, note: '', createdBy: 'binh', state: 'APPROVED', required: ['binh'], approvals: ['binh'], history: [], createdAt: time - HOUR }];
    r.availability = [{ id: 'slot1', startsAt: '2026-11-14T01:00:00Z' }, { id: 'slot2', startsAt: '2026-11-15T01:00:00Z' }]; r.availabilityVotes = { an: ['slot1'], binh: ['slot1', 'slot2'], chi: ['slot1'] };
    const d = baseRoom('dinner', 'Tối nay ăn gì nhỉ?', 'binh', ['an', 'binh', 'dung', 'nam'], 'vote'); d.time = '18:30'; d.ideas = [{ id: 'hotpot', title: 'Lẩu nấm gần nhà', note: 'Có cả phần chay.', cost: 180000, duration: 90, ownerId: 'dung', endorsements: [], votes: { binh: 2 }, completed: false }, { id: 'pizza', title: 'Pizza và một bộ phim', note: 'Nhẹ nhàng cho tối thứ Sáu.', cost: 160000, duration: 120, ownerId: 'an', endorsements: [], votes: { binh: 1 }, completed: false }];
    const sea = baseRoom('sea', 'Một ngày ở Vũng Tàu', 'an', ['an', 'binh', 'chi'], 'plan'); sea.region = 'Vũng Tàu'; sea.lifecycle = 'ENDED'; sea.endedAt = time - HOUR * 48; sea.policy = { mode: 'WINDOWED', windowHours: 72 }; sea.planVersion = 1;
    s.rooms = [r, d, sea, baseRoom('chill', 'Chủ nhật chậm lại', 'an', ['an', 'linh'], 'ideas')];
    const sample = (id, ownerId, caption, scene, personal, roomId, hours) => { const m = { id, ownerId, caption, scene, createdAt: time - hours * HOUR, status: 'PUBLISHED' }; s.moments.push(m); if (personal) s.placements.push({ id: `${id}_personal`, momentId: id, kind: 'PERSONAL', audienceMode: 'SELECTED_FRIENDS', grants: personal.map(userId => ({ userId, epoch: 0 })), createdAt: m.createdAt, status: 'ACTIVE' }); if (roomId) s.placements.push({ id: `${id}_room`, momentId: id, kind: 'ROOM', roomId, roomNameAtPlacement: room(s, roomId).name, grants: [], createdAt: m.createdAt, status: 'ACTIVE' }); };
    sample('m1', 'an', 'Cà phê ngon hơn khi đi cùng nhau.', 'cafe', ['binh', 'linh', 'dung'], 'dalat', 1);
    sample('m2', 'binh', 'Có những buổi sáng chỉ muốn ở lại đây.', 'forest', ['an', 'linh'], 'dalat', 2);
    sample('m3', 'chi', 'Bữa này để mình chọn nha.', 'food', null, 'dalat', 3);
    sample('m4', 'linh', 'Đi một ngày, nhớ thật lâu.', 'sea', ['an', 'binh'], 'sea', 48);
    sample('m5', 'dung', 'Thành phố lên đèn rồi.', 'city', ['an'], null, 5);
    sample('m6', 'an', 'Một chút yên tĩnh cho riêng mình.', 'window', [], null, 24);
    sample('m7', 'an', 'Tụi mình và bầu trời hôm ấy.', 'sunset', ['binh'], 'sea', 49);
    s.comments.push({ id: 'c1', kind: 'PHOTO', targetId: 'm1_room', authorId: 'chi', text: 'Lần sau mình lại quay lại quán này nhé!', state: 'ACTIVE', createdAt: time - 120000 }, { id: 'c2', kind: 'ALBUM', targetId: 'dalat', authorId: 'binh', text: 'Mọi người góp ảnh vào đây, cuối chuyến mình cùng xem lại nha.', state: 'ACTIVE', createdAt: time - HOUR });
    s.reactions = [{ placementId: 'm1_personal', userId: 'binh', type: 'HEART' }, { placementId: 'm1_room', userId: 'chi', type: 'FIRE' }, { placementId: 'm2_personal', userId: 'an', type: 'HEART' }];
    const chat = conversation(s, 'an', 'binh', true); chat.sequence = 2; chat.read.an = 1; s.messages.push({ id: 'msg1', conversationId: chat.id, sequence: 1, senderId: 'an', text: 'Hẹn mọi người ở quán lúc 8 giờ nhé!', referenceId: null, state: 'SENT', createdAt: time - HOUR }, { id: 'msg2', conversationId: chat.id, sequence: 2, senderId: 'binh', text: 'Ảnh này đẹp quá, gửi mình địa chỉ quán với.', referenceId: 'm1_personal', state: 'SENT', createdAt: time - 180000 });
    note(s, ['an'], 'Bình vừa chia sẻ một khoảnh khắc', 'PHOTO', 'm2_personal'); return s;
  }
  return { HOUR, REACTIONS, fail, check, clean, now, uid, pair, user, room, moment, placement, active, blocked, friends, epoch, member, host, requireMember, requireHost, writable, requireWrite, visible, ownMoments, photos, canMessage, audience, publish, attach, removePlace, deleteMoment, react, comment, comments, deleteComment, setPolicy, lifecycle, leave, changeRelation, conversation, send, messages, markRead, recall, unread, createRoom, joinRoom, addIdea, deleteIdea, vote, submitVotes, setPhase, selectIdea, lockShortlist, submitOrder, publishPlan, ranked, allocations, validateExpense, addExpense, approveExpense, balances, debts, settle, settlementAction, editExpense, approveAdjustment, note, seed };
});
