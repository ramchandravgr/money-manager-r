(function () {
  const API = '/mmr/web/v1/api';
  let sessionToken = sessionStorage.getItem('mmr_web_session') || '';
  let metaAccounts = [];
  let metaCategories = [];
  let chartLibsLoaded = false;
  let approvalSelection = new Set();
  let txnViewMode = 'split';
  let txnItemsCache = [];
  let txnSelectedId = null;
  let txnFlatIds = [];
  let txnBulkSelection = new Set();
  let activeTab = 'approvals';
  const TXN_PRESETS_KEY = 'mmr_web_txn_presets';
  let mergeState = { approvalIds: [], selectedTxnId: null, ledger: [], suggestedIds: new Set() };

  const mainEl = document.querySelector('main');
  const loginPanel = document.getElementById('login-panel');
  const loginForm = document.getElementById('login-form');
  const appPanel = document.getElementById('app-panel');
  const loginError = document.getElementById('login-error');
  const statusLine = document.getElementById('status-line');
  const logoutBtn = document.getElementById('logout-btn');
  const txnDialog = document.getElementById('txn-dialog');
  const mergeDialog = document.getElementById('merge-dialog');
  const smsDialog = document.getElementById('sms-dialog');

  const ACTION_ICONS = {
    view: '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 5C7 5 2.73 8.11 1 12.5c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5C21.27 8.11 17 5 12 5zm0 12.5a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"/></svg>',
    save: '<svg viewBox="0 0 24 24" aria-hidden="true" class="icon-stroke"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M17 21v-8H7v8M7 3v5h8"/></svg>',
    merge: '<svg viewBox="0 0 24 24" aria-hidden="true" class="icon-stroke"><path fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round" d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round" d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>',
    approve: '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z"/></svg>',
    reject: '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M18.3 5.71a1 1 0 0 0-1.41 0L12 10.59 7.11 5.7A1 1 0 0 0 5.7 7.11L10.59 12l-4.89 4.89a1 1 0 1 0 1.41 1.41L12 13.41l4.89 4.89a1 1 0 0 0 1.41-1.41L13.41 12l4.89-4.89a1 1 0 0 0 0-1.4z"/></svg>',
  };

  function iconActionButton(className, label, iconKey, variant) {
    const v = variant ? ` icon-action-btn--${variant}` : '';
    return `<button type="button" class="icon-action-btn${v} ${className}" title="${escapeHtml(label)}" aria-label="${escapeHtml(label)}">${ACTION_ICONS[iconKey]}</button>`;
  }

  function escapeHtml(s) {
    return String(s || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  async function api(path, options = {}) {
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    if (sessionToken) headers['X-MMR-Web-Session'] = sessionToken;
    const res = await fetch(API + path, Object.assign({}, options, { headers }));
    const text = await res.text();
    let data = {};
    try { data = text ? JSON.parse(text) : {}; } catch (_) {}
    if (!res.ok) throw Object.assign(new Error(data.error || res.statusText), { status: res.status, data });
    return data;
  }

  function formatMoney(n) {
    return '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  function formatDate(ms) {
    if (!ms) return '';
    return new Date(ms).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  function typePill(type) {
    const t = (type || 'EXPENSE').toLowerCase();
    return `<span class="type-pill ${t}">${escapeHtml(type)}</span>`;
  }

  function categoryTypeForTxn(type) {
    if (type === 'INCOME') return 'INCOME';
    if (type === 'TRANSFER') return 'TRANSFER';
    return 'EXPENSE';
  }

  function categoriesForType(type) {
    const catType = categoryTypeForTxn(type);
    return metaCategories.filter((c) => c.type === catType || (catType === 'EXPENSE' && c.type === 'EXPENSE'));
  }

  function subcategoriesForCategory(categoryId) {
    const cat = metaCategories.find((c) => c.id === categoryId);
    return cat && cat.subcategories ? cat.subcategories : [];
  }

  function accountOptions(selectedId, excludeId) {
    return metaAccounts
      .filter((a) => a.id !== excludeId)
      .map((a) => `<option value="${a.id}" ${a.id === selectedId ? 'selected' : ''}>${escapeHtml(a.name)}</option>`)
      .join('');
  }

  function categoryOptions(type, selectedId) {
    const cats = categoriesForType(type);
    return '<option value="">—</option>' + cats.map((c) =>
      `<option value="${c.id}" ${c.id === selectedId ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
  }

  function subcategoryOptions(categoryId, selectedId) {
    const subs = subcategoriesForCategory(categoryId);
    return '<option value="">—</option>' + subs.map((s) =>
      `<option value="${s.id}" ${s.id === selectedId ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('');
  }

  async function refreshMeta() {
    metaAccounts = [];
    metaCategories = [];
    await ensureMeta();
  }

  async function addCategory(type) {
    const name = prompt('New category name');
    if (!name || !name.trim()) return;
    const created = await api('/meta/categories', {
      method: 'POST',
      body: JSON.stringify({ name: name.trim(), type: categoryTypeForTxn(type) }),
    });
    await refreshMeta();
    return created.id;
  }

  async function addSubcategory(categoryId) {
    if (!categoryId) {
      alert('Choose a category first.');
      return;
    }
    const name = prompt('New subcategory name');
    if (!name || !name.trim()) return;
    const created = await api('/meta/subcategories', {
      method: 'POST',
      body: JSON.stringify({ categoryId, name: name.trim() }),
    });
    await refreshMeta();
    return created.id;
  }

  function setLoginVisible(visible) {
    loginPanel.classList.toggle('hidden', !visible);
    mainEl.classList.toggle('login-view', visible);
    if (visible) {
      appPanel.classList.add('hidden');
      logoutBtn.classList.add('hidden');
      requestAnimationFrame(() => document.getElementById('password')?.focus());
    }
  }

  function handleAuthError(e) {
    if (e.status === 401) {
      sessionToken = '';
      sessionStorage.removeItem('mmr_web_session');
      setLoginVisible(true);
      return true;
    }
    return false;
  }

  async function ensureMeta() {
    if (metaAccounts.length && metaCategories.length) return;
    const [accounts, categories] = await Promise.all([
      api('/meta/accounts'),
      api('/meta/categories'),
    ]);
    metaAccounts = accounts.items || [];
    metaCategories = categories.items || [];
  }

  function showTab(name) {
    activeTab = name;
    document.querySelectorAll('.tab-panel').forEach((el) => el.classList.add('hidden'));
    document.getElementById('tab-' + name).classList.remove('hidden');
    document.querySelectorAll('.tabs button').forEach((btn) => {
      btn.classList.toggle('active', btn.dataset.tab === name);
    });
    if (name === 'approvals') loadApprovals();
    if (name === 'transactions') loadTransactions();
    if (name === 'stats') loadStats();
  }

  async function boot() {
    try {
      const status = await fetch(API + '/status').then((r) => r.json());
      if (!status.enabled) {
        statusLine.textContent = 'Web Manage is off on the phone. Turn it on in Settings.';
        return;
      }
      statusLine.textContent = 'Web Manage is active on your phone.';
      if (sessionToken) {
        await ensureMeta();
        refreshTxnPresetDropdown();
        setLoginVisible(false);
        appPanel.classList.remove('hidden');
        logoutBtn.classList.remove('hidden');
        showTab('approvals');
      } else {
        setLoginVisible(true);
      }
    } catch (_) {
      statusLine.textContent = 'Could not reach the phone. Check Wi‑Fi and the URL from Settings.';
    }
  }

  async function submitLogin() {
    loginError.textContent = '';
    const passwordInput = document.getElementById('password');
    const password = passwordInput.value.trim();
    const loginBtn = document.getElementById('login-btn');
    loginBtn.disabled = true;
    try {
      const data = await api('/auth/login', { method: 'POST', body: JSON.stringify({ password }) });
      sessionToken = data.token;
      sessionStorage.setItem('mmr_web_session', sessionToken);
      await ensureMeta();
      refreshTxnPresetDropdown();
      setLoginVisible(false);
      appPanel.classList.remove('hidden');
      logoutBtn.classList.remove('hidden');
      showTab('approvals');
    } catch (e) {
      const code = e.data?.error;
      const messages = {
        invalid_password: 'Wrong password. Tap Update password on the phone if you changed it recently.',
        password_not_set: 'No Web password on the phone. Open Web Manage in Settings and tap Set password.',
        password_required: 'Enter your Web Manage password.',
        invalid_body: 'Sign-in request was empty. Reload the page and try again.',
        web_manage_disabled: 'Web Manage is off on the phone. Turn it on in Settings.',
      };
      loginError.textContent = messages[code] || 'Sign-in failed. Use http (not https) and the exact URL from Settings.';
      passwordInput.focus();
    } finally {
      loginBtn.disabled = false;
    }
  }

  loginForm.addEventListener('submit', (ev) => {
    ev.preventDefault();
    submitLogin();
  });

  logoutBtn.addEventListener('click', () => {
    sessionToken = '';
    sessionStorage.removeItem('mmr_web_session');
    appPanel.classList.add('hidden');
    setLoginVisible(true);
  });

  document.querySelectorAll('.tabs button').forEach((btn) => {
    btn.addEventListener('click', () => showTab(btn.dataset.tab));
  });

  document.querySelectorAll('#txn-toolbar .view-toggle').forEach((btn) => {
    btn.addEventListener('click', () => {
      txnViewMode = btn.dataset.txnView;
      document.querySelectorAll('#txn-toolbar .view-toggle').forEach((b) => {
        b.classList.toggle('active', b === btn);
      });
      renderTransactionsView();
    });
  });
  document.getElementById('txn-period').addEventListener('change', () => {
    const custom = document.getElementById('txn-period').value === 'custom';
    document.getElementById('txn-custom-dates').classList.toggle('hidden', !custom);
  });
  document.getElementById('txn-apply-filters').addEventListener('click', () => loadTransactions());
  document.getElementById('txn-filter-q').addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter') loadTransactions();
  });
  document.getElementById('txn-preset-save').addEventListener('click', saveTxnPreset);
  document.getElementById('txn-preset-delete').addEventListener('click', deleteTxnPreset);
  document.getElementById('txn-preset-load').addEventListener('change', (ev) => {
    if (ev.target.value) applyTxnPreset(ev.target.value);
  });
  document.getElementById('txn-select-all').addEventListener('change', (ev) => {
    const checked = ev.target.checked;
    txnBulkSelection = checked ? new Set(txnFlatIds) : new Set();
    renderTransactionsView();
    updateTxnBulkToolbar();
  });
  document.getElementById('txn-bulk-delete').addEventListener('click', bulkDeleteSelectedTxns);
  document.getElementById('txn-export-excel').addEventListener('click', exportTransactionsExcel);
  document.addEventListener('keydown', onTxnListKeydown);

  document.getElementById('stats-mode').addEventListener('change', () => {
    const custom = document.getElementById('stats-mode').value === 'custom';
    document.getElementById('stats-custom-range').classList.toggle('hidden', !custom);
    if (!custom) loadStats();
  });
  document.getElementById('stats-refresh').addEventListener('click', () => loadStats());
  document.querySelectorAll('#stats-ledger .view-toggle').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#stats-ledger .view-toggle').forEach((b) => {
        b.classList.toggle('active', b === btn);
      });
      loadStats();
    });
  });

  function readApprovalFields(tile) {
    const type = tile.querySelector('.ap-type').value;
    const isTransfer = type === 'TRANSFER';
    return {
      amount: Number(tile.querySelector('.ap-amount').value),
      type,
      accountId: Number(tile.querySelector('.ap-account').value),
      relatedAccountId: isTransfer ? Number(tile.querySelector('.ap-related-account').value) : undefined,
      categoryId: isTransfer ? undefined : (Number(tile.querySelector('.ap-category').value) || undefined),
      subcategoryId: isTransfer ? undefined : (Number(tile.querySelector('.ap-subcategory').value) || undefined),
      note: tile.querySelector('.ap-note').value,
      dateTimeMillis: (() => {
        const raw = tile.querySelector('.ap-date')?.value;
        if (!raw) return undefined;
        const ms = Date.parse(raw);
        return Number.isFinite(ms) ? ms : undefined;
      })(),
    };
  }

  function approvalTileHtml(item) {
    const type = item.type || 'EXPENSE';
    const isTransfer = type === 'TRANSFER';
    const selected = approvalSelection.has(item.id);
    const dateVal = item.dateTimeMillis
      ? new Date(item.dateTimeMillis).toISOString().slice(0, 16)
      : '';
    return `
      <article class="approval-tile ${selected ? 'selected' : ''}" data-id="${item.id}">
        <div class="tile-head">
          <div class="tile-head-main">
            <div class="tile-amount">${formatMoney(item.amount)}</div>
            <div class="tile-meta">${formatDate(item.dateTimeMillis)}</div>
          </div>
          <div class="tile-head-aside">
            <div class="tile-actions tile-actions--icons">
              ${iconActionButton('ap-view-sms', 'View SMS', 'view')}
              ${iconActionButton('ap-save', 'Save changes', 'save')}
              ${iconActionButton('ap-merge-one', 'Link to transaction', 'merge')}
              ${iconActionButton('approve', 'Approve', 'approve', 'approve')}
              ${iconActionButton('reject', 'Reject', 'reject', 'reject')}
            </div>
            <div class="tile-type-row">${typePill(type)}</div>
          </div>
        </div>
        <div class="tile-meta">${escapeHtml(item.sender || '')}${item.merchant ? ' · ' + escapeHtml(item.merchant) : ''}</div>
        <div class="sms-excerpt">${escapeHtml(item.smsBody || '')}</div>
        <label class="inline-check ap-merge-select"><input type="checkbox" class="ap-select" ${selected ? 'checked' : ''} /> Select for merge</label>
        <div class="tile-fields">
          <label>Amount <input type="number" step="0.01" class="ap-amount" value="${item.amount}" /></label>
          <label>Date <input type="datetime-local" class="ap-date" value="${dateVal}" /></label>
          <label>Type
            <select class="ap-type">
              <option value="EXPENSE" ${type === 'EXPENSE' ? 'selected' : ''}>Expense</option>
              <option value="INCOME" ${type === 'INCOME' ? 'selected' : ''}>Income</option>
              <option value="TRANSFER" ${type === 'TRANSFER' ? 'selected' : ''}>Transfer</option>
            </select>
          </label>
          <label class="ap-from-label">${isTransfer ? 'From account' : 'Account'}
            <select class="ap-account">${accountOptions(item.accountId)}</select>
          </label>
          <label class="ap-to-wrap ${isTransfer ? '' : 'hidden'}">To account
            <select class="ap-related-account">${accountOptions(null, item.accountId)}</select>
          </label>
          <div class="ap-cat-block ${isTransfer ? 'hidden' : ''}">
            <label>Category <select class="ap-category">${categoryOptions(type, item.categoryId)}</select></label>
            <div class="inline-add"><button type="button" class="secondary ap-add-cat">+ Category</button></div>
            <label>Subcategory <select class="ap-subcategory">${subcategoryOptions(item.categoryId)}</select></label>
            <div class="inline-add"><button type="button" class="secondary ap-add-sub">+ Subcategory</button></div>
          </div>
          <label>Note <input type="text" class="ap-note" value="${String(item.note || '').replace(/"/g, '&quot;')}" /></label>
        </div>
      </article>`;
  }

  function wireApprovalTile(tile) {
    const id = Number(tile.dataset.id);
    tile.querySelector('.ap-type').addEventListener('change', (ev) => {
      const t = ev.target.value;
      const isTransfer = t === 'TRANSFER';
      tile.querySelector('.ap-to-wrap').classList.toggle('hidden', !isTransfer);
      tile.querySelector('.ap-cat-block').classList.toggle('hidden', isTransfer);
      tile.querySelector('.ap-from-label').textContent = isTransfer ? 'From account' : 'Account';
      tile.querySelector('.ap-category').innerHTML = categoryOptions(t, Number(tile.querySelector('.ap-category').value) || null);
      tile.querySelector('.ap-subcategory').innerHTML = subcategoryOptions(Number(tile.querySelector('.ap-category').value));
      tile.querySelector('.tile-type-row').innerHTML = typePill(t);
    });
    tile.querySelector('.ap-category').addEventListener('change', (ev) => {
      const catId = Number(ev.target.value);
      tile.querySelector('.ap-subcategory').innerHTML = subcategoryOptions(catId);
    });
    tile.querySelector('.ap-add-cat').addEventListener('click', async () => {
      const type = tile.querySelector('.ap-type').value;
      try {
        const newId = await addCategory(type);
        tile.querySelector('.ap-category').innerHTML = categoryOptions(type, newId);
        tile.querySelector('.ap-subcategory').innerHTML = subcategoryOptions(newId);
      } catch (e) { if (!handleAuthError(e)) alert('Could not add category.'); }
    });
    tile.querySelector('.ap-add-sub').addEventListener('click', async () => {
      const catId = Number(tile.querySelector('.ap-category').value);
      try {
        const newId = await addSubcategory(catId);
        tile.querySelector('.ap-subcategory').innerHTML = subcategoryOptions(catId, newId);
      } catch (e) { if (!handleAuthError(e)) alert('Could not add subcategory.'); }
    });
    tile.querySelector('.ap-select').addEventListener('change', (ev) => {
      if (ev.target.checked) approvalSelection.add(id);
      else approvalSelection.delete(id);
      tile.classList.toggle('selected', ev.target.checked);
      updateApprovalToolbar();
    });
    tile.querySelector('.ap-view-sms').addEventListener('click', () => {
      const body = tile.querySelector('.sms-excerpt')?.textContent || '';
      const metaLine = tile.querySelector('.tile-meta')?.textContent || '';
      const sender = metaLine.split('·')[0].trim();
      document.getElementById('sms-dialog-meta').textContent = sender ? `From: ${sender}` : '';
      document.getElementById('sms-dialog-body').textContent = body;
      smsDialog.showModal();
    });
    tile.querySelector('.ap-save').addEventListener('click', async () => {
      const body = readApprovalFields(tile);
      await api('/approvals/' + id, { method: 'POST', body: JSON.stringify(body) });
      loadApprovals();
    });
    tile.querySelector('.approve').addEventListener('click', async () => {
      const body = readApprovalFields(tile);
      await api('/approvals/' + id, { method: 'POST', body: JSON.stringify(body) });
      await api('/approvals/' + id + '/approve', { method: 'POST', body: JSON.stringify(body) });
      approvalSelection.delete(id);
      loadApprovals();
    });
    tile.querySelector('.reject').addEventListener('click', async () => {
      await api('/approvals/' + id + '/reject', { method: 'POST', body: '{}' });
      approvalSelection.delete(id);
      loadApprovals();
    });
    tile.querySelector('.ap-merge-one').addEventListener('click', () => {
      openMergeDialog([id]);
    });
  }

  function updateApprovalToolbar() {
    const bar = document.getElementById('approval-toolbar');
    if (!bar) return;
    const count = approvalSelection.size;
    bar.querySelector('.sel-count').textContent = count ? count + ' selected' : 'None selected';
    bar.querySelector('.link-selected').disabled = count === 0;
  }

  async function loadApprovals() {
    const panel = document.getElementById('tab-approvals');
    panel.innerHTML = '<p>Loading…</p>';
    try {
      await ensureMeta();
      const data = await api('/approvals/pending');
      if (!data.items || !data.items.length) {
        approvalSelection.clear();
        panel.innerHTML = '<p class="meta">No pending approvals.</p>';
        return;
      }
      const ids = new Set(data.items.map((i) => i.id));
      approvalSelection = new Set([...approvalSelection].filter((id) => ids.has(id)));
      panel.innerHTML = `
        <div class="approval-content">
          <div class="list-toolbar toolbar-panel approval-toolbar" id="approval-toolbar">
            <span class="toolbar-label sel-count">${approvalSelection.size ? approvalSelection.size + ' selected' : 'None selected'}</span>
            <button type="button" class="secondary link-selected" ${approvalSelection.size ? '' : 'disabled'}>Link selected to transaction</button>
            <button type="button" class="secondary" id="clear-approval-sel">Clear selection</button>
          </div>
          <div class="tile-grid" id="approval-grid"></div>
        </div>`;
      const grid = document.getElementById('approval-grid');
      grid.innerHTML = data.items.map(approvalTileHtml).join('');
      grid.querySelectorAll('.approval-tile').forEach(wireApprovalTile);
      panel.querySelector('.link-selected').addEventListener('click', () => {
        if (!approvalSelection.size) return;
        openMergeDialog([...approvalSelection]);
      });
      panel.querySelector('#clear-approval-sel').addEventListener('click', () => {
        approvalSelection.clear();
        loadApprovals();
      });
      updateApprovalToolbar();
    } catch (e) {
      if (handleAuthError(e)) return;
      panel.innerHTML = '<p class="error">Failed to load approvals.</p>';
    }
  }

  function renderMergePickList(container, items, filterText) {
    const q = (filterText || '').toLowerCase();
    const filtered = items.filter((t) => {
      if (!q) return true;
      const hay = [t.note, t.categoryName, t.accountName, t.type, t.amount, t.id].join(' ').toLowerCase();
      return hay.includes(q);
    });
    container.innerHTML = filtered.map((t) => {
      const suggested = mergeState.suggestedIds.has(t.id);
      const selected = mergeState.selectedTxnId === t.id;
      const line = `${formatMoney(t.amount)} · ${t.type} · ${t.categoryName || '—'} · ${formatDate(t.dateTimeMillis)}`;
      const sub = `${t.accountName || ''}${t.note ? ' — ' + t.note : ''}`;
      return `<button type="button" class="merge-pick ${selected ? 'selected' : ''} ${suggested ? 'suggested' : ''}" data-txn-id="${t.id}">
        <div>${escapeHtml(line)}</div>
        <div class="tile-meta">${escapeHtml(sub)}</div>
      </button>`;
    }).join('') || '<p class="meta" style="padding:0.5rem">No matches.</p>';
    container.querySelectorAll('.merge-pick').forEach((btn) => {
      btn.addEventListener('click', () => {
        mergeState.selectedTxnId = Number(btn.dataset.txnId);
        document.getElementById('merge-confirm').disabled = false;
        mergeDialog.querySelectorAll('.merge-pick').forEach((el) => {
          el.classList.toggle('selected', Number(el.dataset.txnId) === mergeState.selectedTxnId);
        });
      });
    });
  }

  async function openMergeDialog(approvalIds) {
    mergeState = { approvalIds, selectedTxnId: null, ledger: [], suggestedIds: new Set() };
    document.getElementById('merge-summary').textContent =
      approvalIds.length > 1
        ? `Link ${approvalIds.length} SMS approvals into one ledger transaction.`
        : 'Link this SMS approval to an existing transaction (amounts will be combined).';
    document.getElementById('merge-search').value = '';
    document.getElementById('merge-use-ledger').checked = false;
    document.getElementById('merge-confirm').disabled = true;
    const primaryId = approvalIds[0];
    const [ledger, suggested] = await Promise.all([
      api('/transactions?limit=100'),
      api('/approvals/' + primaryId + '/merge-candidates').catch(() => ({ items: [] })),
    ]);
    mergeState.ledger = ledger.items || [];
    (suggested.items || []).forEach((c) => mergeState.suggestedIds.add(c.transactionId));
    const suggestedWrap = document.getElementById('merge-suggested');
    if (mergeState.suggestedIds.size) {
      suggestedWrap.classList.remove('hidden');
      renderMergePickList(
        document.getElementById('merge-suggested-list'),
        mergeState.ledger.filter((t) => mergeState.suggestedIds.has(t.id)),
        '',
      );
    } else {
      suggestedWrap.classList.add('hidden');
    }
    const catSelect = document.getElementById('merge-category');
    catSelect.innerHTML = categoryOptions('EXPENSE');
    document.getElementById('merge-subcategory').innerHTML = '<option value="">—</option>';
    catSelect.onchange = () => {
      document.getElementById('merge-subcategory').innerHTML =
        subcategoryOptions(Number(catSelect.value));
    };
    renderMergePickList(document.getElementById('merge-txn-list'), mergeState.ledger, '');
    mergeDialog.showModal();
  }

  document.getElementById('merge-search').addEventListener('input', (ev) => {
    renderMergePickList(document.getElementById('merge-txn-list'), mergeState.ledger, ev.target.value);
  });
  document.getElementById('merge-cancel').addEventListener('click', () => mergeDialog.close());
  document.getElementById('sms-dialog-close').addEventListener('click', () => smsDialog.close());
  document.getElementById('merge-form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    if (!mergeState.selectedTxnId) return;
    const body = {
      approvalIds: mergeState.approvalIds,
      transactionId: mergeState.selectedTxnId,
      categoryId: Number(document.getElementById('merge-category').value) || undefined,
      subcategoryId: Number(document.getElementById('merge-subcategory').value) || undefined,
      useLedgerCategory: document.getElementById('merge-use-ledger').checked,
    };
    await api('/approvals/merge', { method: 'POST', body: JSON.stringify(body) });
    mergeState.approvalIds.forEach((id) => approvalSelection.delete(id));
    mergeDialog.close();
    loadApprovals();
  });

  const TXN_FORM_DIALOG = {
    id: 'txn-id', amount: 'txn-amount', datetime: null, type: 'txn-type', account: 'txn-account',
    related: 'txn-related-account', category: 'txn-category', subcategory: 'txn-subcategory',
    note: 'txn-note', toWrap: 'txn-to-wrap', catBlock: 'txn-cat-block',
    addCat: 'txn-add-cat', addSub: 'txn-add-sub',
  };
  const TXN_FORM_DETAIL = {
    id: 'txn-detail-id', amount: 'txn-detail-amount', datetime: 'txn-detail-datetime', type: 'txn-detail-type',
    account: 'txn-detail-account', related: 'txn-detail-related', category: 'txn-detail-category',
    subcategory: 'txn-detail-subcategory', note: 'txn-detail-note', toWrap: 'txn-detail-to-wrap',
    catBlock: 'txn-detail-cat-block', addCat: 'txn-detail-add-cat', addSub: 'txn-detail-add-sub',
  };

  function syncTxnTypeUi(type, form = TXN_FORM_DIALOG) {
    const isTransfer = type === 'TRANSFER';
    document.getElementById(form.toWrap).classList.toggle('hidden', !isTransfer);
    document.getElementById(form.catBlock).classList.toggle('hidden', isTransfer);
  }

  function fillTxnFormInto(form, item) {
    document.getElementById(form.id).value = item.id;
    document.getElementById(form.amount).value = item.amount;
    document.getElementById(form.type).value = item.type;
    document.getElementById(form.note).value = item.note || '';
    if (form.datetime) {
      const dt = item.dateTimeMillis ? new Date(item.dateTimeMillis).toISOString().slice(0, 16) : '';
      document.getElementById(form.datetime).value = dt;
    }
    syncTxnTypeUi(item.type, form);
    document.getElementById(form.account).innerHTML = accountOptions(item.accountId);
    document.getElementById(form.related).innerHTML = accountOptions(item.relatedAccountId, item.accountId);
    document.getElementById(form.category).innerHTML = categoryOptions(item.type, item.categoryId);
    document.getElementById(form.subcategory).innerHTML = subcategoryOptions(item.categoryId, item.subcategoryId);
  }

  function readTxnFormBody(form) {
    const type = document.getElementById(form.type).value;
    const body = {
      amount: Number(document.getElementById(form.amount).value),
      type,
      accountId: Number(document.getElementById(form.account).value),
      categoryId: type === 'TRANSFER' ? undefined : (Number(document.getElementById(form.category).value) || undefined),
      subcategoryId: type === 'TRANSFER' ? undefined : (Number(document.getElementById(form.subcategory).value) || undefined),
      relatedAccountId: type === 'TRANSFER' ? Number(document.getElementById(form.related).value) : undefined,
      note: document.getElementById(form.note).value,
    };
    if (form.datetime) {
      const raw = document.getElementById(form.datetime).value;
      if (raw) {
        const ms = Date.parse(raw);
        if (Number.isFinite(ms)) body.dateTimeMillis = ms;
      }
    }
    return body;
  }

  function wireTxnForm(form) {
    document.getElementById(form.type).addEventListener('change', (ev) => {
      syncTxnTypeUi(ev.target.value, form);
      document.getElementById(form.category).innerHTML = categoryOptions(ev.target.value);
      document.getElementById(form.subcategory).innerHTML = '<option value="">—</option>';
    });
    document.getElementById(form.category).addEventListener('change', (ev) => {
      document.getElementById(form.subcategory).innerHTML = subcategoryOptions(Number(ev.target.value));
    });
    document.getElementById(form.addCat).addEventListener('click', async () => {
      const type = document.getElementById(form.type).value;
      try {
        const newId = await addCategory(type);
        document.getElementById(form.category).innerHTML = categoryOptions(type, newId);
        document.getElementById(form.subcategory).innerHTML = subcategoryOptions(newId);
      } catch (e) { if (!handleAuthError(e)) alert('Could not add category.'); }
    });
    document.getElementById(form.addSub).addEventListener('click', async () => {
      const catId = Number(document.getElementById(form.category).value);
      try {
        const newId = await addSubcategory(catId);
        document.getElementById(form.subcategory).innerHTML = subcategoryOptions(catId, newId);
      } catch (e) { if (!handleAuthError(e)) alert('Could not add subcategory.'); }
    });
  }
  wireTxnForm(TXN_FORM_DIALOG);
  wireTxnForm(TXN_FORM_DETAIL);

  function fillTxnForm(item) { fillTxnFormInto(TXN_FORM_DIALOG, item); }

  async function openTxnDialog(id) {
    await ensureMeta();
    const item = await api('/transactions/' + id);
    fillTxnForm(item);
    txnDialog.showModal();
  }

  document.getElementById('txn-cancel').addEventListener('click', () => txnDialog.close());
  document.getElementById('txn-form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const id = document.getElementById('txn-id').value;
    const body = readTxnFormBody(TXN_FORM_DIALOG);
    await api('/transactions/' + id, { method: 'PUT', body: JSON.stringify(body) });
    txnDialog.close();
    loadTransactions();
  });
  document.getElementById('txn-delete').addEventListener('click', async () => {
    const id = document.getElementById('txn-id').value;
    if (!confirm('Delete this transaction on the phone?')) return;
    await api('/transactions/' + id, { method: 'DELETE' });
    txnDialog.close();
    loadTransactions();
  });

  document.getElementById('txn-detail-form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const id = document.getElementById('txn-detail-id').value;
    const body = readTxnFormBody(TXN_FORM_DETAIL);
    await api('/transactions/' + id, { method: 'PUT', body: JSON.stringify(body) });
    loadTransactions();
  });
  document.getElementById('txn-detail-delete').addEventListener('click', async () => {
    const id = document.getElementById('txn-detail-id').value;
    if (!id || !confirm('Delete this transaction on the phone?')) return;
    await api('/transactions/' + id, { method: 'DELETE' });
    txnSelectedId = null;
    loadTransactions();
  });

  function txnFilterQueryString() {
    const period = document.getElementById('txn-period').value;
    const now = Date.now();
    let start;
    let end = now;
    if (period === 'custom') {
      const sd = document.getElementById('txn-filter-start').value;
      const ed = document.getElementById('txn-filter-end').value;
      if (!sd || !ed) return null;
      start = new Date(sd + 'T00:00:00').getTime();
      end = new Date(ed + 'T23:59:59.999').getTime();
      if (!Number.isFinite(start) || !Number.isFinite(end) || start > end) return null;
    } else {
      const days = Number(period) || 30;
      start = now - days * 24 * 60 * 60 * 1000;
    }
    const params = new URLSearchParams({ limit: '500', start: String(start), end: String(end) });
    const type = document.getElementById('txn-filter-type').value;
    const accountId = document.getElementById('txn-filter-account').value;
    const categoryId = document.getElementById('txn-filter-category').value;
    const q = document.getElementById('txn-filter-q').value.trim();
    if (type) params.set('type', type);
    if (accountId) params.set('accountId', accountId);
    if (categoryId) params.set('categoryId', categoryId);
    if (q) params.set('q', q);
    return params.toString();
  }

  function sortTxnItems(items) {
    const sort = document.getElementById('txn-filter-sort').value;
    const copy = items.slice();
    copy.sort((a, b) => {
      if (sort === 'date-asc') return a.dateTimeMillis - b.dateTimeMillis;
      if (sort === 'amount-desc') return b.amount - a.amount;
      if (sort === 'amount-asc') return a.amount - b.amount;
      return b.dateTimeMillis - a.dateTimeMillis;
    });
    return copy;
  }

  function groupTxnsByDate(items) {
    const groups = new Map();
    items.forEach((item) => {
      const key = new Date(item.dateTimeMillis).toLocaleDateString('en-IN', {
        weekday: 'short', year: 'numeric', month: 'short', day: 'numeric',
      });
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(item);
    });
    return groups;
  }

  function txnSubtitle(item) {
    const cat = item.categoryName || (item.type === 'TRANSFER' ? 'Transfer' : 'Uncategorized');
    const sub = item.subcategoryName ? ' / ' + item.subcategoryName : '';
    const acct = item.type === 'TRANSFER' && item.relatedAccountName
      ? (item.accountName || '') + ' → ' + item.relatedAccountName
      : (item.accountName || '');
    return escapeHtml(cat + sub) + (acct ? ' · ' + escapeHtml(acct) : '');
  }

  function readTxnFilterState() {
    return {
      period: document.getElementById('txn-period').value,
      start: document.getElementById('txn-filter-start').value,
      end: document.getElementById('txn-filter-end').value,
      type: document.getElementById('txn-filter-type').value,
      accountId: document.getElementById('txn-filter-account').value,
      categoryId: document.getElementById('txn-filter-category').value,
      q: document.getElementById('txn-filter-q').value,
      sort: document.getElementById('txn-filter-sort').value,
    };
  }

  function applyTxnFilterState(state) {
    document.getElementById('txn-period').value = state.period || '30';
    document.getElementById('txn-filter-start').value = state.start || '';
    document.getElementById('txn-filter-end').value = state.end || '';
    document.getElementById('txn-filter-type').value = state.type || '';
    document.getElementById('txn-filter-account').value = state.accountId || '';
    document.getElementById('txn-filter-category').value = state.categoryId || '';
    document.getElementById('txn-filter-q').value = state.q || '';
    document.getElementById('txn-filter-sort').value = state.sort || 'date-desc';
    const custom = state.period === 'custom';
    document.getElementById('txn-custom-dates').classList.toggle('hidden', !custom);
  }

  function loadTxnPresets() {
    try {
      return JSON.parse(localStorage.getItem(TXN_PRESETS_KEY) || '[]');
    } catch (_) {
      return [];
    }
  }

  function saveTxnPresets(presets) {
    localStorage.setItem(TXN_PRESETS_KEY, JSON.stringify(presets));
    refreshTxnPresetDropdown();
  }

  function refreshTxnPresetDropdown() {
    const sel = document.getElementById('txn-preset-load');
    const presets = loadTxnPresets();
    sel.innerHTML = '<option value="">Saved presets…</option>' +
      presets.map((p) => `<option value="${escapeHtml(p.id)}">${escapeHtml(p.name)}</option>`).join('');
  }

  function saveTxnPreset() {
    const name = prompt('Name for this filter preset');
    if (!name || !name.trim()) return;
    const presets = loadTxnPresets();
    const id = 'p_' + Date.now();
    presets.push({ id, name: name.trim(), filters: readTxnFilterState() });
    saveTxnPresets(presets);
    document.getElementById('txn-preset-load').value = id;
  }

  function applyTxnPreset(id) {
    const preset = loadTxnPresets().find((p) => p.id === id);
    if (!preset) return;
    applyTxnFilterState(preset.filters);
    loadTransactions();
  }

  function deleteTxnPreset() {
    const id = document.getElementById('txn-preset-load').value;
    if (!id) {
      alert('Choose a preset to delete.');
      return;
    }
    if (!confirm('Delete this preset?')) return;
    saveTxnPresets(loadTxnPresets().filter((p) => p.id !== id));
  }

  function updateTxnBulkToolbar() {
    const n = txnBulkSelection.size;
    document.getElementById('txn-bulk-delete').disabled = n === 0;
    document.getElementById('txn-bulk-count').textContent = n ? n + ' selected' : '';
    const allSelected = txnFlatIds.length > 0 && n === txnFlatIds.length;
    document.getElementById('txn-select-all').checked = allSelected;
  }

  async function bulkDeleteSelectedTxns() {
    const ids = [...txnBulkSelection];
    if (!ids.length) return;
    if (!confirm('Delete ' + ids.length + ' transaction(s) on the phone?')) return;
    const result = await api('/transactions/bulk-delete', {
      method: 'POST',
      body: JSON.stringify({ ids }),
    });
    txnBulkSelection.clear();
    if (result.failed) {
      alert('Deleted ' + result.deleted + '; ' + result.failed + ' failed.');
    }
    loadTransactions();
  }

  async function exportTransactionsExcel() {
    const qs = txnFilterQueryString();
    if (!qs) {
      alert('Choose a valid date range first.');
      return;
    }
    const headers = {};
    if (sessionToken) headers['X-MMR-Web-Session'] = sessionToken;
    const res = await fetch(API + '/transactions/export?' + qs, { headers });
    if (!res.ok) {
      alert('Export failed.');
      return;
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'mmr-transactions.xlsx';
    a.click();
    URL.revokeObjectURL(url);
  }

  function onTxnListKeydown(e) {
    if (activeTab !== 'transactions' || appPanel.classList.contains('hidden')) return;
    if (txnViewMode !== 'split') return;
    if (e.target.matches('input, textarea, select, button')) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      moveTxnSelection(1);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      moveTxnSelection(-1);
    }
  }

  function moveTxnSelection(delta) {
    if (!txnFlatIds.length) return;
    let idx = txnFlatIds.indexOf(txnSelectedId);
    if (idx < 0) idx = 0;
    else idx = Math.max(0, Math.min(txnFlatIds.length - 1, idx + delta));
    selectTxnDetail(txnFlatIds[idx]);
  }

  function renderTxnSplitList(items) {
    const groups = groupTxnsByDate(items);
    let html = '';
    groups.forEach((rows, dateLabel) => {
      html += `<div class="txn-date-group"><div class="txn-date-header">${escapeHtml(dateLabel)}</div>`;
      rows.forEach((item) => {
        const selected = txnSelectedId === item.id;
        const bulkChecked = txnBulkSelection.has(item.id);
        html += `<button type="button" class="txn-compact-row ${selected ? 'selected' : ''}" data-txn-id="${item.id}">
          <div class="row-top">
            <label class="inline-check txn-bulk-check" onclick="event.stopPropagation()">
              <input type="checkbox" class="txn-bulk" data-txn-id="${item.id}" ${bulkChecked ? 'checked' : ''} />
            </label>
            <span>${typePill(item.type)}</span>
            <span class="row-amt">${formatMoney(item.amount)}</span>
          </div>
          <div class="row-sub">${txnSubtitle(item)}</div>
          ${item.note ? `<div class="row-sub">${escapeHtml(item.note)}</div>` : ''}
        </button>`;
      });
      html += '</div>';
    });
    return html || '<p class="meta" style="padding:0.75rem">No transactions match filters.</p>';
  }

  function selectTxnDetail(id) {
    txnSelectedId = id;
    const item = txnItemsCache.find((t) => t.id === id);
    if (!item) return;
    document.getElementById('txn-detail-empty').classList.add('hidden');
    document.getElementById('txn-detail-form').classList.remove('hidden');
    fillTxnFormInto(TXN_FORM_DETAIL, item);
    document.querySelectorAll('.txn-compact-row').forEach((row) => {
      row.classList.toggle('selected', Number(row.dataset.txnId) === id);
    });
    const row = document.querySelector('.txn-compact-row[data-txn-id="' + id + '"]');
    row?.scrollIntoView({ block: 'nearest' });
  }

  function wireTxnSplitList() {
    document.querySelectorAll('.txn-compact-row').forEach((row) => {
      row.addEventListener('click', () => selectTxnDetail(Number(row.dataset.txnId)));
    });
    document.querySelectorAll('.txn-bulk').forEach((box) => {
      box.addEventListener('change', (ev) => {
        const id = Number(ev.target.dataset.txnId);
        if (ev.target.checked) txnBulkSelection.add(id);
        else txnBulkSelection.delete(id);
        updateTxnBulkToolbar();
      });
    });
  }

  function wireTxnBulkCheckboxes(host) {
    host.querySelectorAll('.txn-bulk').forEach((box) => {
      box.addEventListener('change', (ev) => {
        const id = Number(ev.target.dataset.txnId);
        if (ev.target.checked) txnBulkSelection.add(id);
        else txnBulkSelection.delete(id);
        updateTxnBulkToolbar();
      });
    });
  }

  function renderTxnCards(items) {
    return `<div class="tile-grid">${items.map((item) => {
      const transferLine = item.type === 'TRANSFER' && item.relatedAccountName
        ? `${escapeHtml(item.accountName)} → ${escapeHtml(item.relatedAccountName)}`
        : escapeHtml(item.accountName || '');
      const bulkChecked = txnBulkSelection.has(item.id);
      return `
        <article class="txn-card" data-txn-id="${item.id}">
          <label class="inline-check"><input type="checkbox" class="txn-bulk" data-txn-id="${item.id}" ${bulkChecked ? 'checked' : ''} /> Select</label>
          <div class="tile-head">
            <div class="tile-amount">${formatMoney(item.amount)}</div>
            ${typePill(item.type)}
          </div>
          <div class="tile-meta">${formatDate(item.dateTimeMillis)}</div>
          <div class="tile-meta">${escapeHtml(item.categoryName || 'Uncategorized')}${item.subcategoryName ? ' / ' + escapeHtml(item.subcategoryName) : ''}</div>
          <div class="tile-meta">${transferLine}</div>
          ${item.note ? `<div class="tile-meta">${escapeHtml(item.note)}</div>` : ''}
          <div class="tile-actions">
            <button type="button" class="secondary txn-edit">Edit</button>
          </div>
        </article>`;
    }).join('')}</div>`;
  }

  function renderTxnTable(items, options = {}) {
    const showBulk = options.showBulk !== false;
    const showEdit = options.showEdit !== false;
    const rows = items.map((item) => {
      const acct = item.type === 'TRANSFER' && item.relatedAccountName
        ? `${escapeHtml(item.accountName)} → ${escapeHtml(item.relatedAccountName)}`
        : escapeHtml(item.accountName || '');
      const bulkChecked = txnBulkSelection.has(item.id);
      const bulkCell = showBulk
        ? `<td><input type="checkbox" class="txn-bulk" data-txn-id="${item.id}" ${bulkChecked ? 'checked' : ''} /></td>`
        : '';
      const editCell = showEdit
        ? `<td><button type="button" class="secondary txn-edit toolbar-btn">Edit</button></td>`
        : '';
      return `<tr data-txn-id="${item.id}">
        ${bulkCell}
        <td>${formatDate(item.dateTimeMillis)}</td>
        <td>${typePill(item.type)}</td>
        <td class="num">${formatMoney(item.amount)}</td>
        <td>${escapeHtml(item.categoryName || '—')}${item.subcategoryName ? ' / ' + escapeHtml(item.subcategoryName) : ''}</td>
        <td>${acct}</td>
        <td>${escapeHtml(item.note || '')}</td>
        ${editCell}
      </tr>`;
    }).join('');
    const bulkHead = showBulk ? '<th></th>' : '';
    const editHead = showEdit ? '<th></th>' : '';
    return `<div class="txn-table-wrap"><table class="txn-table">
      <thead><tr>${bulkHead}<th>Date</th><th>Type</th><th>Amount</th><th>Category</th><th>Account</th><th>Note</th>${editHead}</tr></thead>
      <tbody>${rows}</tbody></table></div>`;
  }

  function wireTxnEditButtons(host) {
    host.querySelectorAll('.txn-edit').forEach((btn) => {
      btn.addEventListener('click', (ev) => {
        const row = ev.target.closest('[data-txn-id]');
        const id = row.dataset.txnId;
        openTxnDialog(id).catch((e) => { if (!handleAuthError(e)) alert('Could not open transaction.'); });
      });
    });
  }

  async function populateTxnFilterDropdowns() {
    await ensureMeta();
    const acctSel = document.getElementById('txn-filter-account');
    const prevAcct = acctSel.value;
    acctSel.innerHTML = '<option value="">All</option>' +
      metaAccounts.map((a) => `<option value="${a.id}">${escapeHtml(a.name)}</option>`).join('');
    if (prevAcct) acctSel.value = prevAcct;
    const catSel = document.getElementById('txn-filter-category');
    const prevCat = catSel.value;
    catSel.innerHTML = '<option value="">All</option>' +
      metaCategories.map((c) => `<option value="${c.id}">${escapeHtml(c.name)} (${c.type})</option>`).join('');
    if (prevCat) catSel.value = prevCat;
  }

  function renderTxnFilterSummary(data, shown) {
    document.getElementById('txn-filter-summary').innerHTML = `
      <div class="stats-summary-card">
        <div class="label">Showing</div>
        <div class="value">${shown} / ${data.total || 0}</div>
      </div>
      <div class="stats-summary-card income">
        <div class="label">Income (filtered)</div>
        <div class="value">${formatMoney(data.totalIncome || 0)}</div>
      </div>
      <div class="stats-summary-card expense">
        <div class="label">Expense (filtered)</div>
        <div class="value">${formatMoney(data.totalExpense || 0)}</div>
      </div>
      <div class="stats-summary-card">
        <div class="label">Net</div>
        <div class="value">${formatMoney((data.totalIncome || 0) - (data.totalExpense || 0))}</div>
      </div>`;
  }

  function renderTransactionsView() {
    const splitLayout = document.getElementById('txn-split-layout');
    const altHost = document.getElementById('txn-alt-host');
    const items = sortTxnItems(txnItemsCache);
    txnFlatIds = items.map((t) => t.id);
    updateTxnBulkToolbar();
    if (txnViewMode === 'split') {
      splitLayout.classList.remove('hidden');
      altHost.classList.add('hidden');
      const listPane = document.getElementById('txn-list-pane');
      listPane.setAttribute('tabindex', '0');
      listPane.innerHTML = renderTxnSplitList(items);
      wireTxnSplitList();
      if (txnSelectedId && items.some((t) => t.id === txnSelectedId)) {
        selectTxnDetail(txnSelectedId);
      } else if (items.length) {
        selectTxnDetail(items[0].id);
      } else {
        txnSelectedId = null;
        document.getElementById('txn-detail-empty').classList.remove('hidden');
        document.getElementById('txn-detail-form').classList.add('hidden');
      }
      return;
    }
    splitLayout.classList.add('hidden');
    altHost.classList.remove('hidden');
    if (!items.length) {
      altHost.innerHTML = '<p class="meta">No transactions match filters.</p>';
      return;
    }
    altHost.innerHTML = txnViewMode === 'table' ? renderTxnTable(items) : renderTxnCards(items);
    wireTxnEditButtons(altHost);
    wireTxnBulkCheckboxes(altHost);
  }

  async function loadTransactions() {
    const qs = txnFilterQueryString();
    if (!qs) {
      document.getElementById('txn-list-pane').innerHTML = '<p class="error">Choose a valid custom date range.</p>';
      return;
    }
    document.getElementById('txn-list-pane').innerHTML = '<p class="meta" style="padding:0.75rem">Loading…</p>';
    try {
      await populateTxnFilterDropdowns();
      refreshTxnPresetDropdown();
      const data = await api('/transactions?' + qs);
      txnItemsCache = data.items || [];
      const visibleIds = new Set(txnItemsCache.map((t) => t.id));
      txnBulkSelection = new Set([...txnBulkSelection].filter((id) => visibleIds.has(id)));
      renderTxnFilterSummary(data, txnItemsCache.length);
      renderTransactionsView();
    } catch (e) {
      if (handleAuthError(e)) return;
      document.getElementById('txn-list-pane').innerHTML = '<p class="error">Failed to load transactions.</p>';
    }
  }

  function loadScript(src) {
    return new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = src;
      s.onload = resolve;
      s.onerror = reject;
      document.head.appendChild(s);
    });
  }

  async function ensureChartLibs() {
    if (chartLibsLoaded) return;
    await loadScript('/mmr/web/js/jquery-3.5.1.min.js');
    await loadScript('/mmr/web/chart/highcharts.js');
    await loadScript('/mmr/web/chart/no-data-to-display.js');
    chartLibsLoaded = true;
  }

  function drawPieChart(containerId, data) {
    const pieBackgroundColor = data.pieBackgroundColor || '#FFFFFF';
    const pieTextColor = data.pieTextColor || '#1A1A2E';
    const pieTextSize = data.pieTextSize || 12;
    const pieBorderColor = data.pieBorderColor || '#FFFFFF';
    const el = document.getElementById(containerId);
    el.innerHTML = '<div id="hc-inner" style="height:280px"></div>';
    window.jQuery('#hc-inner').highcharts({
      chart: { type: 'pie', backgroundColor: pieBackgroundColor, plotShadow: false },
      series: data.series,
      title: { text: '' },
      colors: data.colors,
      credits: { enabled: false },
      tooltip: { pointFormat: '{series.name}<b>{point.amount}</b>' },
      plotOptions: {
        pie: {
          borderColor: pieBorderColor,
          allowPointSelect: true,
          dataLabels: {
            enabled: true,
            format: '<b>{point.name}</b><br>{point.percentage:.1f} %',
            style: { fontWeight: 'normal', fontSize: pieTextSize, color: pieTextColor },
          },
        },
      },
      lang: { noData: data.nodata },
      noData: { style: { fontWeight: 'normal', fontSize: '15px', color: '#8d8d8d' } },
    });
  }

  function statsLedger() {
    const active = document.querySelector('#stats-ledger .view-toggle.active');
    return active?.dataset.ledger || 'expense';
  }

  function statsQueryString() {
    const mode = document.getElementById('stats-mode').value;
    const ledger = statsLedger();
    const params = new URLSearchParams({ mode, ledger });
    if (mode === 'custom') {
      const startDate = document.getElementById('stats-start-date').value;
      const endDate = document.getElementById('stats-end-date').value;
      if (!startDate || !endDate) {
        return null;
      }
      const start = new Date(startDate + 'T00:00:00').getTime();
      const end = new Date(endDate + 'T23:59:59.999').getTime();
      if (!Number.isFinite(start) || !Number.isFinite(end) || start > end) {
        return null;
      }
      params.set('start', String(start));
      params.set('end', String(end));
    }
    return params.toString();
  }

  function formatPeriodRange(startMs, endMs) {
    if (!startMs || !endMs) return '';
    const opts = { dateStyle: 'medium' };
    return new Date(startMs).toLocaleDateString('en-IN', opts) +
      ' – ' + new Date(endMs).toLocaleDateString('en-IN', opts);
  }

  async function loadStats() {
    const categoryList = document.getElementById('stats-category-list');
    const txnTableHost = document.getElementById('stats-txn-table');
    const qs = statsQueryString();
    if (!qs) {
      categoryList.innerHTML = '';
      txnTableHost.innerHTML = '<p class="error">Choose a valid custom date range.</p>';
      return;
    }
    categoryList.innerHTML = '';
    txnTableHost.innerHTML = '<p class="meta">Loading…</p>';
    try {
      const [spending, chartData] = await Promise.all([
        api('/stats/spending?' + qs),
        api('/stats/chart?' + qs),
      ]);
      const ledger = spending.ledger || chartData.ledger || statsLedger();
      document.getElementById('stats-period-label').textContent =
        'Period: ' + formatPeriodRange(spending.startMillis, spending.endMillis);
      document.getElementById('stats-summary').innerHTML = `
        <div class="stats-summary-card income">
          <div class="label">Income</div>
          <div class="value">${formatMoney(spending.totalIncome || 0)}</div>
        </div>
        <div class="stats-summary-card expense">
          <div class="label">Expense</div>
          <div class="value">${formatMoney(spending.totalExpense || 0)}</div>
        </div>
        <div class="stats-summary-card">
          <div class="label">Net</div>
          <div class="value">${formatMoney(spending.net || 0)}</div>
        </div>`;
      document.getElementById('stats-chart-title').textContent =
        ledger === 'income' ? 'Income by category' : 'Expenses by category';
      await ensureChartLibs();
      drawPieChart('stats-chart', chartData);
      const catRows = (spending.categories || []).map((c) =>
        `<div class="meta">${escapeHtml(c.categoryName || 'Other')}: ${formatMoney(c.amount)}</div>`).join('');
      const emptyCatLabel = ledger === 'income' ? 'No income in this period.' : 'No expenses in this period.';
      categoryList.innerHTML = catRows || `<p class="meta">${emptyCatLabel}</p>`;

      const txnType = ledger === 'income' ? 'INCOME' : 'EXPENSE';
      const txnQs = new URLSearchParams({
        start: String(spending.startMillis),
        end: String(spending.endMillis),
        type: txnType,
        limit: '500',
      });
      const txnData = await api('/transactions?' + txnQs.toString());
      const txnItems = sortTxnItems(txnData.items || []);
      document.getElementById('stats-txn-title').textContent =
        (ledger === 'income' ? 'Income' : 'Expense') +
        ` transactions (${txnItems.length}${txnData.total > txnItems.length ? ' of ' + txnData.total : ''})`;
      if (!txnItems.length) {
        txnTableHost.innerHTML = '<p class="meta">No matching transactions in this period.</p>';
      } else {
        txnTableHost.innerHTML = renderTxnTable(txnItems, { showBulk: false, showEdit: true });
        wireTxnEditButtons(txnTableHost);
      }
    } catch (e) {
      if (handleAuthError(e)) return;
      txnTableHost.innerHTML = '<p class="error">Failed to load spending.</p>';
    }
  }

  boot();
})();
