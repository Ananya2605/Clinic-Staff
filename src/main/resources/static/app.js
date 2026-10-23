'use strict';
const $ = s => document.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const inr = n => n == null ? 'Hidden' : '₹' + Number(n).toLocaleString('en-IN');
const label = s => ({ON_LEAVE: 'On leave'}[s] || s.charAt(0) + s.slice(1).toLowerCase());
const today = () => new Date().toISOString().slice(0, 10);
const tomorrow = () => new Date(Date.now() + 864e5).toISOString().slice(0, 10);

let auth = null, me = null, tab = 'dir', q = '', dept = 'All';
let emps = [], leaves = [], dash = null, audit = [];
try { auth = sessionStorage.getItem('auth'); } catch (e) {}

const canEdit = () => me && (me.role === 'ADMIN' || me.role === 'HR');
const isAdmin = () => me && me.role === 'ADMIN';

function toast(msg) {
  const t = $('#toast'); t.textContent = msg; t.style.display = 'block';
  clearTimeout(toast.h); toast.h = setTimeout(() => t.style.display = 'none', 3500);
}

async function api(path, method = 'GET', body) {
  const res = await fetch(path, {
    method,
    headers: { Authorization: auth || '', 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401) { if (me) logout(); throw new Error('Wrong username or password.'); }
  if (!res.ok) {
    let msg = 'Request failed (' + res.status + ').';
    try {
      const j = await res.json();
      if (j.errors && j.errors.length) msg = j.errors.map(e => (e.field ? e.field + ': ' : '') + e.defaultMessage).join('; ');
      else if (j.message) msg = j.message;
    } catch (e) {}
    throw new Error(msg);
  }
  return res.status === 204 ? null : res.json();
}

async function load() {
  emps = await api('/api/employees');
  leaves = await api('/api/leaves');
  if (canEdit()) { dash = await api('/api/dashboard'); audit = await api('/api/audit'); }
}
async function refresh() {
  try { await load(); } catch (e) { toast(e.message); }
  render();
}

async function login() {
  const u = $('#u').value.trim(), p = $('#p').value;
  if (!u || !p) { $('#lerr').textContent = 'Enter your username and password.'; return; }
  auth = 'Basic ' + btoa(u + ':' + p);
  try {
    me = await api('/api/me');
    try { sessionStorage.setItem('auth', auth); } catch (e) {}
    tab = canEdit() ? 'dash' : 'dir';
    await refresh();
  } catch (e) {
    auth = null; me = null;
    $('#lerr').textContent = e.message;
  }
}
function logout() {
  auth = null; me = null;
  try { sessionStorage.removeItem('auth'); } catch (e) {}
  render();
}
function theme() {
  const r = document.documentElement;
  const dark = r.dataset.theme === 'dark' || (!r.dataset.theme && matchMedia('(prefers-color-scheme:dark)').matches);
  r.dataset.theme = dark ? 'light' : 'dark';
  try { localStorage.setItem('theme', r.dataset.theme); } catch (e) {}
}
try { const t = localStorage.getItem('theme'); if (t) document.documentElement.dataset.theme = t; } catch (e) {}

function render() {
  const app = $('#app');
  if (!me) {
    app.innerHTML = `<div class="login"><h1>Clinic Staff Manager</h1><p>Sign in with your staff account.</p>
      <input id="u" placeholder="Username" autocomplete="username">
      <input id="p" type="password" placeholder="Password" autocomplete="current-password">
      <div class="err" id="lerr"></div>
      <button class="btn p" id="go">Sign in</button>
      <small>Demo accounts: admin / admin123, hr / hr123, viewer / viewer123</small></div>`;
    $('#go').onclick = login;
    $('#p').onkeydown = e => { if (e.key === 'Enter') login(); };
    return;
  }
  const tabs = [['dir', 'Directory'], ['org', 'Org chart'], ['leave', 'Leave']];
  if (canEdit()) tabs.unshift(['dash', 'Dashboard']), tabs.push(['audit', 'Audit trail']);
  const views = { dash: vDash, dir: vDir, org: vOrg, leave: vLeave, audit: vAudit };
  app.innerHTML = `<header><h1>Clinic Staff Manager</h1><nav>${tabs.map(t =>
    `<button class="${tab === t[0] ? 'on' : ''}" data-tab="${t[0]}">${t[1]}</button>`).join('')}</nav>
    <span><button class="btn" id="th">Dark mode</button> <button class="btn" id="lo">${esc(me.username)} (${me.role}) · Sign out</button></span></header>
    <main>${views[tab]()}</main>`;
  app.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => { tab = b.dataset.tab; render(); });
  $('#th').onclick = theme; $('#lo').onclick = logout;
  if (tab === 'dir') bindDir();
  if (tab === 'leave') bindLeave();
}

/* ---------- dashboard ---------- */
function vDash() {
  if (!dash) return '<p>Dashboard data is not available.</p>';
  const avgs = Object.entries(dash.averageSalaryByDepartment);
  const mx = Math.max(1, ...avgs.map(a => Number(a[1])));
  return `<div class="grid">
    <div class="stat"><b>${dash.totalStaff}</b>Staff</div>
    <div class="stat"><b>${dash.activeToday}</b>Active today</div>
    <div class="stat"><b>${dash.onLeaveToday}</b>On leave today</div>
    <div class="stat"><b>${dash.pendingLeaveRequests}</b>Leave requests waiting</div></div>
    <div class="panel"><h2>Average monthly salary by department</h2>
    ${avgs.map(([d, v]) => `<div class="bar"><span>${esc(d)}</span><i style="width:${Math.round(v / mx * 60)}%"></i><span>${inr(v)}</span></div>`).join('')}
    ${dash.salaryOutliers.length ? `<p>Salary outlier: ${dash.salaryOutliers.map(esc).join(', ')} earns more than twice the staff average.</p>` : ''}</div>`;
}

/* ---------- directory ---------- */
function rows() {
  const list = emps.filter(e => (dept === 'All' || e.department === dept) &&
    (e.name + e.title + e.email).toLowerCase().includes(q.toLowerCase()));
  if (!list.length) return '<tr><td colspan="6">No staff match. Clear the search or pick another department.</td></tr>';
  return list.map(e => `<tr><td>${esc(e.name)}<br><small>${esc(e.email)}</small></td><td>${esc(e.title)}</td>
    <td>${esc(e.department)}</td><td>${inr(e.salary)}</td>
    <td><span class="tag ${e.effectiveStatus}">${label(e.effectiveStatus)}</span></td>
    <td>${canEdit() ? `<button class="btn" data-edit="${e.id}">Edit</button>` : ''}
    ${isAdmin() ? `<button class="btn d" data-del="${e.id}">Delete</button>` : ''}</td></tr>`).join('');
}
function vDir() {
  const ds = ['All', ...new Set(emps.map(e => e.department))];
  return `<div class="tools"><input id="q" placeholder="Search name, title or email" value="${esc(q)}">
    <select id="dept">${ds.map(d => `<option${d === dept ? ' selected' : ''}>${esc(d)}</option>`).join('')}</select>
    ${canEdit() ? '<button class="btn p" id="add">Add employee</button>' : ''}</div>
    <div class="panel"><table><thead><tr><th>Name</th><th>Title</th><th>Department</th><th>Salary</th><th>Status</th><th></th></tr></thead>
    <tbody id="tb">${rows()}</tbody></table></div>`;
}
function bindTable() {
  document.querySelectorAll('[data-edit]').forEach(b => b.onclick = () => editDialog(+b.dataset.edit));
  document.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    const e = emps.find(x => x.id === +b.dataset.del);
    if (!confirm('Delete ' + e.name + '? This cannot be undone.')) return;
    try { await api('/api/employees/' + e.id, 'DELETE'); toast('Deleted ' + e.name); await refresh(); } catch (err) { toast(err.message); }
  });
}
function bindDir() {
  $('#q').oninput = e => { q = e.target.value; $('#tb').innerHTML = rows(); bindTable(); };
  $('#dept').onchange = e => { dept = e.target.value; render(); };
  if ($('#add')) $('#add').onclick = () => editDialog(null);
  bindTable();
}
function editDialog(id) {
  const e = id ? emps.find(x => x.id === id) : { name: '', email: '', department: '', title: '', salary: '', managerId: null, status: 'ACTIVE' };
  const f = (k, l, t = 'text') => `<label for="f_${k}">${l}</label><input id="f_${k}" type="${t}" value="${esc(e[k])}">`;
  $('#dlg').innerHTML = `<h2>${id ? 'Edit employee' : 'Add employee'}</h2>
    ${f('name', 'Full name')}${f('email', 'Email')}${f('department', 'Department')}${f('title', 'Job title')}${f('salary', 'Monthly salary (₹)', 'number')}
    <label for="f_managerId">Reports to</label><select id="f_managerId"><option value="">Nobody (top of chart)</option>
    ${emps.filter(m => m.id !== id).map(m => `<option value="${m.id}"${m.id === e.managerId ? ' selected' : ''}>${esc(m.name)}</option>`).join('')}</select>
    <label for="f_status">Status</label><select id="f_status">${['ACTIVE', 'INACTIVE'].map(s => `<option value="${s}"${s === e.status ? ' selected' : ''}>${label(s)}</option>`).join('')}</select>
    <div class="err" id="ferr"></div>
    <div class="tools" style="margin-top:12px"><button class="btn p" id="save">Save employee</button><button class="btn" id="cancel">Cancel</button></div>`;
  $('#cancel').onclick = () => $('#dlg').close();
  $('#save').onclick = async () => {
    const v = k => $('#f_' + k).value.trim();
    const body = { name: v('name'), email: v('email'), department: v('department'), title: v('title'),
      salary: v('salary') === '' ? null : Number(v('salary')), managerId: v('managerId') ? Number(v('managerId')) : null, status: v('status') };
    try {
      await api(id ? '/api/employees/' + id : '/api/employees', id ? 'PUT' : 'POST', body);
      $('#dlg').close(); toast('Saved ' + body.name); await refresh();
    } catch (err) { $('#ferr').textContent = err.message; }
  };
  $('#dlg').showModal();
}

/* ---------- org chart ---------- */
function vOrg() {
  const ids = new Set(emps.map(e => e.id)), seen = new Set();
  const build = pid => {
    const kids = emps.filter(e => pid === null ? (!e.managerId || !ids.has(e.managerId)) : e.managerId === pid && !seen.has(e.id));
    if (!kids.length) return '';
    return '<ul>' + kids.map(e => { seen.add(e.id);
      return `<li><span class="node">${esc(e.name)}<small>${esc(e.title)}</small></span>${build(e.id)}</li>`; }).join('') + '</ul>';
  };
  return `<div class="panel tree"><h2>Reporting structure</h2>${build(null) || '<p>No staff yet.</p>'}</div>`;
}

/* ---------- leave ---------- */
function vLeave() {
  const name = id => (emps.find(e => e.id === id) || {}).name || 'Removed';
  const form = canEdit() ? `<div class="panel"><h2>Request leave</h2><div class="tools">
    <select id="le" aria-label="Employee">${emps.map(e => `<option value="${e.id}">${esc(e.name)}</option>`).join('')}</select>
    <input type="date" id="lf" value="${today()}" aria-label="From"><input type="date" id="lt" value="${tomorrow()}" aria-label="To">
    <input id="lr" placeholder="Reason"><button class="btn p" id="req">Submit request</button></div><div class="err" id="lerr2"></div></div>` : '';
  const body = leaves.length ? leaves.slice().reverse().map(l => `<tr><td>${esc(name(l.employeeId))}</td><td>${l.fromDate}</td><td>${l.toDate}</td>
    <td>${esc(l.reason)}</td><td><span class="tag ${l.state}">${label(l.state)}</span></td>
    <td>${isAdmin() && l.state === 'PENDING' ? `<button class="btn p" data-ap="${l.id}">Approve</button> <button class="btn d" data-rj="${l.id}">Reject</button>` : ''}</td></tr>`).join('')
    : '<tr><td colspan="6">No leave requests yet.</td></tr>';
  return `${form}<div class="panel"><h2>Leave requests</h2><table><tr><th>Employee</th><th>From</th><th>To</th><th>Reason</th><th>State</th><th></th></tr>${body}</table>
    <p><small>Approved leave shows the employee as On leave from the start date and back to Active after the return date, automatically.</small></p></div>`;
}
function bindLeave() {
  if ($('#req')) $('#req').onclick = async () => {
    try {
      await api('/api/leaves', 'POST', { employeeId: +$('#le').value, fromDate: $('#lf').value, toDate: $('#lt').value, reason: $('#lr').value.trim() || 'Not given' });
      toast('Leave requested'); await refresh();
    } catch (e) { $('#lerr2').textContent = e.message; }
  };
  const decide = (attr, verb) => document.querySelectorAll(`[${attr}]`).forEach(b => b.onclick = async () => {
    try { await api(`/api/leaves/${b.getAttribute(attr)}/${verb}`, 'POST'); await refresh(); } catch (e) { toast(e.message); }
  });
  decide('data-ap', 'approve'); decide('data-rj', 'reject');
}

/* ---------- audit ---------- */
function vAudit() {
  return `<div class="panel"><h2>Audit trail</h2><div class="tl">${audit.slice(0, 80).map(a =>
    `<div>${esc(a.message)}<br><small>${esc(a.actor)} · ${new Date(a.at).toLocaleString()}</small></div>`).join('') || '<p>No changes recorded yet.</p>'}</div></div>`;
}

/* ---------- start ---------- */
(async function init() {
  if (auth) {
    try { me = await api('/api/me'); tab = canEdit() ? 'dash' : 'dir'; await load(); } catch (e) { me = null; auth = null; }
  }
  render();
})();
