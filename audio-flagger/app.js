'use strict';

/* ============================== state ============================== */

const COLORS = { red: '#ef4444', yellow: '#eab308', green: '#22c55e', blue: '#3b82f6' };
const COLOR_ORDER = ['red', 'yellow', 'green', 'blue'];

const $ = id => document.getElementById(id);
const els = {
  toolbar: $('toolbar'), openBtn: $('openBtn'), audioInput: $('audioInput'), fileLabel: $('fileLabel'),
  playBtn: $('playBtn'), restartBtn: $('restartBtn'), timeDisp: $('timeDisp'), speed: $('speed'), volume: $('volume'), follow: $('follow'),
  flagMs: $('flagMs'), flagId: $('flagId'), flagCount: $('flagCount'),
  importBtn: $('importBtn'), importInput: $('importInput'),
  exportCsvBtn: $('exportCsvBtn'), exportJsonBtn: $('exportJsonBtn'),
  zoomInBtn: $('zoomInBtn'), zoomOutBtn: $('zoomOutBtn'), fitBtn: $('fitBtn'),
  ruler: $('ruler'), waveWrap: $('waveWrap'), wave: $('wave'),
  flagLayer: $('flagLayer'), selectionBox: $('selectionBox'), playhead: $('playhead'), emptyHint: $('emptyHint'),
  sidebarBtn: $('sidebarBtn'), flagSidebar: $('flagSidebar'), sidebarFlagCount: $('sidebarFlagCount'), flagList: $('flagList'),
  player: $('player'),
};

let audioCtx = null;
let peaks = null;          // { min, max, bucket, sampleRate, count }
let duration = 0;          // seconds
let fileName = null;
let objectUrl = null;

let pxPerSec = 100;        // zoom
let viewStart = 0;         // seconds at left edge
let dirty = true;

let flags = [];            // { id, time (sec), color, el }
let selectedFlags = new Set();
let selectionAnchor = null;
let pendingPlaybackFlag = null;
let flagIdDirty = false;
let defaultColor = 'red';
let lastPulseT = 0;

let undoStack = [];
let saveTimer = null;

const waveCtx = els.wave.getContext('2d');
const rulerCtx = els.ruler.getContext('2d');
let waveW = 0, waveH = 0, rulerH = 0;

/* ============================== helpers ============================== */

const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

function uuid() {
  if (crypto.randomUUID) { try { return crypto.randomUUID(); } catch (e) { /* fall through */ } }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

function fmtTime(sec) {
  if (!isFinite(sec)) sec = 0;
  const ms = Math.round(sec * 1000);
  const m = Math.floor(ms / 60000);
  const s = Math.floor(ms / 1000) % 60;
  const f = ms % 1000;
  return `${m}:${String(s).padStart(2, '0')}.${String(f).padStart(3, '0')}`;
}

function fitPxPerSec() {
  return duration > 0 ? Math.max(1, waveW / duration) : 100;
}

function clampView() {
  const maxStart = Math.max(0, duration - waveW / pxPerSec);
  viewStart = clamp(viewStart, 0, maxStart);
}

function timeAtX(clientX) {
  const rect = els.waveWrap.getBoundingClientRect();
  return viewStart + (clientX - rect.left) / pxPerSec;
}

/* ============================== audio loading ============================== */

async function loadAudio(file) {
  els.fileLabel.textContent = `decoding ${file.name}…`;
  try {
    const buf = await file.arrayBuffer();
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const audioBuf = await audioCtx.decodeAudioData(buf);

    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = URL.createObjectURL(file);
    els.player.src = objectUrl;

    fileName = file.name;
    duration = audioBuf.duration;
    peaks = computePeaks(audioBuf);
    pxPerSec = fitPxPerSec();
    viewStart = 0;
    lastPulseT = 0;
    undoStack = [];

    setFlags(loadSavedFlags() || []);
    selectFlag(null);

    els.fileLabel.textContent = file.name;
    els.playBtn.disabled = false;
    els.restartBtn.disabled = false;
    els.playhead.hidden = false;
    els.emptyHint.hidden = true;
    dirty = true;
  } catch (err) {
    els.fileLabel.textContent = `failed to load: ${file.name}`;
    console.error(err);
    alert(`Could not decode "${file.name}" as audio.\n${err.message || err}`);
  }
}

function computePeaks(buffer) {
  const bucket = 256;
  const ch0 = buffer.getChannelData(0);
  const ch1 = buffer.numberOfChannels > 1 ? buffer.getChannelData(1) : null;
  const count = Math.ceil(ch0.length / bucket);
  const min = new Float32Array(count);
  const max = new Float32Array(count);
  for (let i = 0; i < count; i++) {
    let mn = 1, mx = -1;
    const start = i * bucket, end = Math.min(ch0.length, start + bucket);
    for (let j = start; j < end; j++) {
      let v = ch0[j];
      if (ch1) v = (v + ch1[j]) * 0.5;
      if (v < mn) mn = v;
      if (v > mx) mx = v;
    }
    min[i] = mn;
    max[i] = mx;
  }
  return { min, max, bucket, sampleRate: buffer.sampleRate, count };
}

/* ============================== flags ============================== */

function makeFlag(time, color, id) {
  const f = { id: id || uuid(), time, color: color || defaultColor, el: null };
  const el = document.createElement('div');
  el.className = 'flag';
  el.title = f.id;
  el.style.setProperty('--c', COLORS[f.color] || COLORS.red);
  el.innerHTML = '<div class="line"></div><div class="cap"></div>';
  attachFlagHandlers(el, f);
  els.flagLayer.appendChild(el);
  f.el = el;
  return f;
}

function setFlags(list) {
  const selectedIds = new Set([...selectedFlags].map(f => f.id));
  const anchorId = selectionAnchor ? selectionAnchor.id : null;
  els.flagLayer.innerHTML = '';
  flags = list.map(f => makeFlag(f.time, f.color, f.id));
  selectedFlags = new Set(flags.filter(f => selectedIds.has(f.id)));
  selectionAnchor = flags.find(f => f.id === anchorId) || null;
  refreshSelection();
  updateFlagCount();
  layoutFlags();
  scheduleSave();
}

function addFlagAt(time) {
  pushUndo();
  const f = makeFlag(clamp(time, 0, duration || time), defaultColor);
  flags.push(f);
  selectFlag(f.id);
  updateFlagCount();
  layoutFlags();
  scheduleSave();
}

function deleteSelected() {
  if (!selectedFlags.size) return;
  pushUndo();
  for (const f of selectedFlags) f.el.remove();
  flags = flags.filter(f => !selectedFlags.has(f));
  selectFlag(null);
  updateFlagCount();
  scheduleSave();
}

function setFlagColor(f, color) {
  f.color = color;
  f.el.style.setProperty('--c', COLORS[color]);
}

function selectFlag(id, mode = 'replace') {
  const flag = flags.find(f => f.id === id) || null;
  if (!flag) {
    selectedFlags.clear();
    selectionAnchor = null;
    pendingPlaybackFlag = null;
  } else if (mode === 'toggle') {
    if (selectedFlags.has(flag)) selectedFlags.delete(flag);
    else selectedFlags.add(flag);
    selectionAnchor = flag;
  } else if (mode === 'range' && selectionAnchor && flags.includes(selectionAnchor)) {
    const ordered = [...flags].sort((a, b) => a.time - b.time);
    const start = ordered.indexOf(selectionAnchor);
    const end = ordered.indexOf(flag);
    selectedFlags = new Set(ordered.slice(Math.min(start, end), Math.max(start, end) + 1));
  } else {
    selectedFlags = new Set([flag]);
    selectionAnchor = flag;
  }
  refreshSelection();
}

function refreshSelection() {
  for (const f of flags) {
    f.el.classList.toggle('selected', selectedFlags.has(f));
  }
  const sel = selectedFlag();
  els.flagMs.disabled = !sel;
  els.flagMs.value = sel ? Math.round(sel.time * 1000) : '';
  els.flagId.disabled = selectedFlags.size === 0;
  els.flagId.setCustomValidity('');
  els.flagId.value = sel ? sel.id : '';
  els.flagId.placeholder = selectedFlags.size > 1 ? `base ID for ${selectedFlags.size} flags` : '';
  updateFlagCount();
  refreshSidebarSelection();
}

function selectedFlag() {
  return selectedFlags.size === 1 ? selectedFlags.values().next().value : null;
}

function selectedFlagsInTimeOrder() {
  return flags.filter(f => selectedFlags.has(f)).sort((a, b) => a.time - b.time);
}

function selectLastPassedFlag(time) {
  let last = null;
  for (const f of flags) {
    if (f.time <= time && (!last || f.time > last.time)) last = f;
  }
  const current = selectedFlag();
  if (last === current) return;
  commitFlagIdEdit();
  if (last) selectFlag(last.id);
  else selectFlag(null);
}

function updateFlagCount() {
  const total = `${flags.length} flag${flags.length === 1 ? '' : 's'}`;
  els.flagCount.textContent = selectedFlags.size ? `${total} · ${selectedFlags.size} selected` : total;
}

function renderFlagList() {
  els.flagList.innerHTML = '';
  const ordered = [...flags].sort((a, b) => a.time - b.time);
  els.sidebarFlagCount.textContent = ordered.length;
  if (!ordered.length) {
    const empty = document.createElement('div');
    empty.className = 'flagListEmpty muted';
    empty.textContent = 'No flags';
    els.flagList.appendChild(empty);
    return;
  }

  for (const f of ordered) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'flagListItem';
    button._flag = f;
    button.title = `Go to ${f.id} at ${fmtTime(f.time)}`;

    const dot = document.createElement('span');
    dot.className = 'flagListDot';
    dot.style.setProperty('--c', COLORS[f.color] || COLORS.red);
    const time = document.createElement('span');
    time.className = 'flagListTime mono';
    time.textContent = fmtTime(f.time);
    const id = document.createElement('span');
    id.className = 'flagListId';
    id.textContent = f.id;
    button.append(dot, time, id);

    button.addEventListener('click', () => {
      commitFlagIdEdit();
      pendingPlaybackFlag = null;
      selectFlag(f.id);
      seek(f.time);
      viewStart = f.time - waveW / pxPerSec / 2;
      clampView();
      dirty = true;
    });
    els.flagList.appendChild(button);
  }
  refreshSidebarSelection();
}

function refreshSidebarSelection() {
  for (const item of els.flagList.children) {
    if (item._flag) item.classList.toggle('selected', selectedFlags.has(item._flag));
  }
}

function layoutFlags() {
  for (const f of flags) {
    const x = (f.time - viewStart) * pxPerSec;
    if (x < -20 || x > waveW + 20) {
      f.el.style.display = 'none';
    } else {
      f.el.style.display = '';
      f.el.style.left = x + 'px';
    }
  }
}

function attachFlagHandlers(el, f) {
  el.addEventListener('pointerdown', e => {
    if (e.button !== 0) return;
    e.stopPropagation();
    e.preventDefault();
    if (e.ctrlKey || e.metaKey) {
      selectFlag(f.id, 'toggle');
      return;
    }
    if (e.shiftKey) {
      selectFlag(f.id, 'range');
      return;
    }
    selectFlag(f.id);
    pendingPlaybackFlag = f;

    const startX = e.clientX, startTime = f.time;
    let moved = false;
    el.setPointerCapture(e.pointerId);

    const onMove = ev => {
      const dx = ev.clientX - startX;
      if (!moved) {
        if (Math.abs(dx) < 3) return;
        pushUndo(startTime, f); // snapshot pre-drag state once
        moved = true;
      }
      f.time = clamp(startTime + dx / pxPerSec, 0, duration || Infinity);
      layoutFlags();
      refreshSelection();
    };
    const onUp = () => {
      el.removeEventListener('pointermove', onMove);
      el.removeEventListener('pointerup', onUp);
      el.removeEventListener('pointercancel', onUp);
      if (moved) scheduleSave();
    };
    el.addEventListener('pointermove', onMove);
    el.addEventListener('pointerup', onUp);
    el.addEventListener('pointercancel', onUp);
  });

  el.addEventListener('dblclick', e => {
    e.stopPropagation();
    pushUndo();
    const next = COLOR_ORDER[(COLOR_ORDER.indexOf(f.color) + 1) % COLOR_ORDER.length];
    setFlagColor(f, next);
    scheduleSave();
  });
}

/* ============================== undo ============================== */

function snapshot() {
  return flags.map(f => ({ id: f.id, time: f.time, color: f.color }));
}

// preTime/preFlag: when called mid-drag, record the flag's pre-drag time.
function pushUndo(preTime, preFlag) {
  const snap = snapshot();
  if (preFlag !== undefined) {
    const s = snap.find(x => x.id === preFlag.id);
    if (s) s.time = preTime;
  }
  undoStack.push(snap);
  if (undoStack.length > 200) undoStack.shift();
}

function undo() {
  if (!undoStack.length) return;
  setFlags(undoStack.pop());
}

/* ============================== import / export ============================== */

function flagsToJson() {
  return [...flags]
    .sort((a, b) => a.time - b.time)
    .map(f => ({ id: f.id, ms: Math.round(f.time * 1000 * 1000) / 1000, color: f.color }));
}

// mm:ss.mmm with 2-digit minutes, matching the CSV "Time (formatted)" column
function fmtCsvTime(sec) {
  const ms = Math.round(sec * 1000);
  const m = Math.floor(ms / 60000);
  const s = Math.floor(ms / 1000) % 60;
  const f = ms % 1000;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}.${String(f).padStart(3, '0')}`;
}

function downloadBlob(text, mime, name) {
  const blob = new Blob([text], { type: mime });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 5000);
}

function exportBaseName() {
  return fileName ? fileName.replace(/\.[^.]+$/, '') : 'flags';
}

function exportFlagsCsv() {
  const rows = ['ID,Time (ms),Time (formatted),Color Label'];
  for (const f of flagsToJson()) {
    rows.push(`${f.id},${f.ms},${fmtCsvTime(f.ms / 1000)},${f.color}`);
  }
  downloadBlob(rows.join('\n') + '\n', 'text/csv', `${exportBaseName()}.flags.csv`);
}

function exportFlagsJson() {
  const data = { version: 1, audio: fileName || null, flags: flagsToJson() };
  downloadBlob(JSON.stringify(data, null, 2), 'application/json', `${exportBaseName()}.flags.json`);
}

// Accepts: {flags:[...]}, [...objects], or a bare array of numbers (ms).
function parseFlagsJson(text) {
  const data = JSON.parse(text);
  const arr = Array.isArray(data) ? data : Array.isArray(data.flags) ? data.flags : null;
  if (!arr) throw new Error('Expected a JSON array or an object with a "flags" array');
  return arr.map(item => {
    if (typeof item === 'number') return { id: uuid(), time: item / 1000, color: defaultColor };
    const ms = item.ms ?? item.timeMs ?? item.time_ms ?? item.time ?? item.t;
    if (typeof ms !== 'number') throw new Error('Each flag needs a numeric "ms" value');
    const color = COLORS[item.color] ? item.color : defaultColor;
    return { id: typeof item.id === 'string' ? item.id : uuid(), time: ms / 1000, color };
  });
}

// CSV: "ID,Time (ms),Time (formatted),Color Label" (header optional).
// Also accepts one-column ms lists.
function parseFlagsCsv(text) {
  const out = [];
  for (const line of text.split(/\r?\n/)) {
    const row = line.trim();
    if (!row) continue;
    const cols = row.split(',').map(c => c.trim());
    if (cols.length === 1) {
      const ms = parseFloat(cols[0]);
      if (isFinite(ms)) out.push({ id: uuid(), time: ms / 1000, color: defaultColor });
      continue;
    }
    const ms = parseFloat(cols[1]);
    if (!isFinite(ms)) continue; // header or junk row
    const color = COLORS[cols[3]] ? cols[3] : defaultColor;
    out.push({ id: cols[0] || uuid(), time: ms / 1000, color });
  }
  if (!out.length) throw new Error('No flag rows found (expected "ID,Time (ms),Time (formatted),Color Label")');
  return out;
}

async function importFlags(file) {
  try {
    const text = await file.text();
    const parsed = text.trimStart().startsWith('{') || text.trimStart().startsWith('[')
      ? parseFlagsJson(text)
      : parseFlagsCsv(text);
    pushUndo();
    setFlags(parsed);
    selectFlag(null);
  } catch (err) {
    alert(`Could not import flags from "${file.name}":\n${err.message || err}`);
  }
}

/* ============================== persistence ============================== */

function storageKey() {
  return fileName ? `audio-flagger:${fileName}` : null;
}

function scheduleSave() {
  renderFlagList();
  const key = storageKey();
  if (!key) return;
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    try { localStorage.setItem(key, JSON.stringify(flagsToJson())); } catch (e) { /* quota */ }
  }, 400);
}

function loadSavedFlags() {
  const key = storageKey();
  if (!key) return null;
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    return parseFlagsJson(raw);
  } catch (e) {
    return null;
  }
}

/* ============================== pulse vignette ============================== */

function pulse(color) {
  const el = document.createElement('div');
  el.className = 'pulse';
  el.style.setProperty('--c', COLORS[color] || COLORS.red);
  document.body.appendChild(el);
  el.addEventListener('animationend', () => el.remove());
}

/* ============================== rendering ============================== */

function resizeCanvases() {
  const dpr = window.devicePixelRatio || 1;
  const waveRect = els.waveWrap.getBoundingClientRect();
  const rulerRect = els.ruler.getBoundingClientRect();
  waveW = Math.max(1, Math.round(waveRect.width));
  waveH = Math.max(1, Math.round(waveRect.height));
  rulerH = Math.max(1, Math.round(rulerRect.height));

  els.wave.width = waveW * dpr;
  els.wave.height = waveH * dpr;
  waveCtx.setTransform(dpr, 0, 0, dpr, 0, 0);

  els.ruler.width = waveW * dpr;
  els.ruler.height = rulerH * dpr;
  rulerCtx.setTransform(dpr, 0, 0, dpr, 0, 0);

  clampView();
  dirty = true;
}

function drawWave() {
  const ctx = waveCtx;
  ctx.clearRect(0, 0, waveW, waveH);
  if (!peaks) return;

  const secPerBucket = peaks.bucket / peaks.sampleRate;
  const playX = (els.player.currentTime - viewStart) * pxPerSec;
  const cs = getComputedStyle(document.documentElement);
  const played = cs.getPropertyValue('--wave-played').trim() || '#6ea8fe';
  const rest = cs.getPropertyValue('--wave').trim() || '#4a5568';
  const mid = waveH / 2;

  ctx.fillStyle = played;
  let switched = false;
  for (let x = 0; x < waveW; x++) {
    if (!switched && x >= playX) { ctx.fillStyle = rest; switched = true; }
    const t0 = viewStart + x / pxPerSec;
    const t1 = viewStart + (x + 1) / pxPerSec;
    let b0 = Math.floor(t0 / secPerBucket);
    let b1 = Math.max(b0 + 1, Math.ceil(t1 / secPerBucket));
    if (b1 <= 0 || b0 >= peaks.count) continue;
    b0 = Math.max(0, b0);
    b1 = Math.min(peaks.count, b1);
    let mn = 1, mx = -1;
    for (let b = b0; b < b1; b++) {
      if (peaks.min[b] < mn) mn = peaks.min[b];
      if (peaks.max[b] > mx) mx = peaks.max[b];
    }
    if (mx < mn) continue;
    const y0 = mid - mx * mid * 0.95;
    const y1 = mid - mn * mid * 0.95;
    ctx.fillRect(x, y0, 1, Math.max(1, y1 - y0));
  }

  ctx.fillStyle = 'rgba(255,255,255,0.12)';
  ctx.fillRect(0, mid, waveW, 1);
}

const TICK_STEPS = [0.05, 0.1, 0.25, 0.5, 1, 2, 5, 10, 15, 30, 60, 120, 300, 600];

function drawRuler() {
  const ctx = rulerCtx;
  ctx.clearRect(0, 0, waveW, rulerH);
  if (!peaks) return;

  let step = TICK_STEPS[TICK_STEPS.length - 1];
  for (const s of TICK_STEPS) {
    if (s * pxPerSec >= 70) { step = s; break; }
  }
  const sub = step / 5;

  ctx.font = '11px system-ui, sans-serif';
  ctx.textBaseline = 'top';
  const viewEnd = viewStart + waveW / pxPerSec;

  ctx.fillStyle = '#556070';
  for (let t = Math.floor(viewStart / sub) * sub; t <= viewEnd; t += sub) {
    if (t < 0) continue;
    const x = Math.round((t - viewStart) * pxPerSec);
    ctx.fillRect(x, rulerH - 6, 1, 6);
  }

  ctx.fillStyle = '#9aa5b5';
  for (let t = Math.floor(viewStart / step) * step; t <= viewEnd; t += step) {
    if (t < 0) continue;
    const x = Math.round((t - viewStart) * pxPerSec);
    ctx.fillRect(x, rulerH - 12, 1, 12);
    const label = step >= 1 ? fmtTime(t).replace(/\.\d+$/, '') : fmtTime(t);
    ctx.fillText(label, x + 4, 1);
  }
}

/* ============================== main loop ============================== */

function frame() {
  const t = els.player.currentTime;

  if (!els.player.paused && peaks) {
    let lastPassed = null;
    if (t > lastPulseT) {
      for (const f of flags) {
        if (f.time > lastPulseT && f.time <= t) {
          pulse(f.color);
          if (!lastPassed || f.time > lastPassed.time) lastPassed = f;
        }
      }
    }
    if (lastPassed) {
      commitFlagIdEdit();
      selectFlag(lastPassed.id);
    }
    lastPulseT = t;

    if (els.follow.checked) {
      const x = (t - viewStart) * pxPerSec;
      if (x > waveW * 0.9 || x < 0) {
        viewStart = t - (waveW * 0.1) / pxPerSec;
        clampView();
      }
    }
    dirty = true;
  }

  els.playhead.style.left = ((t - viewStart) * pxPerSec) + 'px';
  els.timeDisp.textContent = `${fmtTime(t)} / ${fmtTime(duration)}`;
  els.playBtn.innerHTML = els.player.paused ? '&#9654;' : '&#10074;&#10074;';

  if (dirty) {
    drawWave();
    drawRuler();
    layoutFlags();
    dirty = false;
  }
  requestAnimationFrame(frame);
}

/* ============================== playback / seeking ============================== */

function togglePlay() {
  if (!peaks) return;
  if (els.player.paused) {
    if (pendingPlaybackFlag && flags.includes(pendingPlaybackFlag)) seek(pendingPlaybackFlag.time);
    pendingPlaybackFlag = null;
    els.player.play();
  } else {
    els.player.pause();
  }
}

function restartPlayback() {
  if (!peaks) return;
  pendingPlaybackFlag = null;
  seek(0);
  els.player.play();
}

function seek(t) {
  if (!peaks) return;
  t = clamp(t, 0, duration);
  els.player.currentTime = t;
  lastPulseT = t;
  dirty = true;
}

function zoomAt(clientX, factor) {
  if (!peaks) return;
  const rect = els.waveWrap.getBoundingClientRect();
  const x = clientX - rect.left;
  const anchor = viewStart + x / pxPerSec;
  pxPerSec = clamp(pxPerSec * factor, fitPxPerSec(), 8000);
  viewStart = anchor - x / pxPerSec;
  clampView();
  dirty = true;
}

/* ============================== event wiring ============================== */

els.openBtn.addEventListener('click', () => els.audioInput.click());
els.audioInput.addEventListener('change', () => {
  if (els.audioInput.files[0]) loadAudio(els.audioInput.files[0]);
  els.audioInput.value = '';
});

els.playBtn.addEventListener('click', togglePlay);
els.restartBtn.addEventListener('click', restartPlayback);
els.sidebarBtn.addEventListener('click', () => {
  const opening = els.flagSidebar.hidden;
  els.flagSidebar.hidden = !opening;
  els.sidebarBtn.setAttribute('aria-expanded', String(opening));
  els.sidebarBtn.setAttribute('aria-label', opening ? 'Hide flags' : 'Show flags');
  els.sidebarBtn.title = opening ? 'Hide flags' : 'Show flags';
  if (opening) renderFlagList();
  resizeCanvases();
});
els.speed.addEventListener('change', () => { els.player.playbackRate = parseFloat(els.speed.value); });
els.volume.addEventListener('input', () => { els.player.volume = parseFloat(els.volume.value); });

els.player.addEventListener('play', () => {
  lastPulseT = els.player.currentTime;
  selectLastPassedFlag(lastPulseT);
});
els.player.addEventListener('seeked', () => {
  lastPulseT = els.player.currentTime;
  if (!els.player.paused) selectLastPassedFlag(lastPulseT);
  dirty = true;
});

document.querySelectorAll('.swatch').forEach(btn => {
  btn.addEventListener('click', () => setDefaultColor(btn.dataset.color));
});

function setDefaultColor(color) {
  defaultColor = color;
  document.querySelectorAll('.swatch').forEach(b => b.classList.toggle('active', b.dataset.color === color));
  const changed = [...selectedFlags].filter(f => f.color !== color);
  if (changed.length) {
    pushUndo();
    for (const f of changed) setFlagColor(f, color);
    scheduleSave();
  }
}

els.flagMs.addEventListener('change', () => {
  const sel = selectedFlag();
  if (!sel) return;
  const ms = parseFloat(els.flagMs.value);
  if (!isFinite(ms)) return;
  pushUndo();
  sel.time = clamp(ms / 1000, 0, duration || ms / 1000);
  layoutFlags();
  refreshSelection();
  scheduleSave();
});

els.flagId.addEventListener('input', () => {
  els.flagId.setCustomValidity('');
  flagIdDirty = true;
});

function commitFlagIdEdit() {
  if (!flagIdDirty) return;
  flagIdDirty = false;
  const selected = selectedFlagsInTimeOrder();
  const baseId = els.flagId.value.trim();
  if (!selected.length || !baseId) {
    refreshSelection();
    return;
  }

  const newIds = selected.map((f, index) => index ? `${baseId}${index}` : baseId);
  const selectedSet = new Set(selected);
  const existingIds = new Set(flags.filter(f => !selectedSet.has(f)).map(f => f.id));
  const conflict = newIds.find(id => existingIds.has(id));
  if (conflict) {
    els.flagId.setCustomValidity(`A flag with ID "${conflict}" already exists.`);
    els.flagId.reportValidity();
    return;
  }

  if (selected.every((f, index) => f.id === newIds[index])) {
    refreshSelection();
    return;
  }

  pushUndo();
  selected.forEach((f, index) => {
    f.id = newIds[index];
    f.el.title = f.id;
  });
  selectionAnchor = selected[0];
  refreshSelection();
  scheduleSave();
}

els.flagId.addEventListener('change', commitFlagIdEdit);
document.addEventListener('pointerdown', e => {
  if (e.target !== els.flagId && !els.flagList.contains(e.target)) commitFlagIdEdit();
}, true);

els.importBtn.addEventListener('click', () => els.importInput.click());
els.importInput.addEventListener('change', () => {
  if (els.importInput.files[0]) importFlags(els.importInput.files[0]);
  els.importInput.value = '';
});
els.exportCsvBtn.addEventListener('click', exportFlagsCsv);
els.exportJsonBtn.addEventListener('click', exportFlagsJson);

els.zoomInBtn.addEventListener('click', () => zoomAt(els.waveWrap.getBoundingClientRect().left + waveW / 2, 1.4));
els.zoomOutBtn.addEventListener('click', () => zoomAt(els.waveWrap.getBoundingClientRect().left + waveW / 2, 1 / 1.4));
els.fitBtn.addEventListener('click', () => {
  if (!peaks) return;
  pxPerSec = fitPxPerSec();
  viewStart = 0;
  dirty = true;
});

// The ruler always scrubs. A waveform click seeks; dragging selects a time range.
function scrubHandler(e) {
  if (e.button !== 0 || !peaks) return;
  pendingPlaybackFlag = null;
  selectFlag(null);
  seek(timeAtX(e.clientX));
  const target = e.currentTarget;
  target.setPointerCapture(e.pointerId);
  const onMove = ev => seek(timeAtX(ev.clientX));
  const onUp = () => {
    target.removeEventListener('pointermove', onMove);
    target.removeEventListener('pointerup', onUp);
    target.removeEventListener('pointercancel', onUp);
  };
  target.addEventListener('pointermove', onMove);
  target.addEventListener('pointerup', onUp);
  target.addEventListener('pointercancel', onUp);
}
els.ruler.addEventListener('pointerdown', scrubHandler);

els.waveWrap.addEventListener('pointerdown', e => {
  if (e.button !== 0 || !peaks) return;
  pendingPlaybackFlag = null;
  e.preventDefault();
  const target = e.currentTarget;
  const rect = target.getBoundingClientRect();
  const startX = clamp(e.clientX - rect.left, 0, rect.width);
  const originalSelection = (e.ctrlKey || e.metaKey) ? new Set(selectedFlags) : new Set();
  let selecting = false;

  target.setPointerCapture(e.pointerId);
  const onMove = ev => {
    const currentX = clamp(ev.clientX - rect.left, 0, rect.width);
    if (!selecting && Math.abs(currentX - startX) < 4) return;
    selecting = true;

    const left = Math.min(startX, currentX);
    const right = Math.max(startX, currentX);
    els.selectionBox.hidden = false;
    els.selectionBox.style.left = `${left}px`;
    els.selectionBox.style.width = `${right - left}px`;

    selectedFlags = new Set(originalSelection);
    for (const f of flags) {
      const x = (f.time - viewStart) * pxPerSec;
      if (x >= left && x <= right) selectedFlags.add(f);
    }
    refreshSelection();
  };
  const onUp = ev => {
    target.removeEventListener('pointermove', onMove);
    target.removeEventListener('pointerup', onUp);
    target.removeEventListener('pointercancel', onUp);
    els.selectionBox.hidden = true;
    if (selecting) {
      const selected = selectedFlagsInTimeOrder();
      selectionAnchor = selected.length ? selected[selected.length - 1] : null;
      refreshSelection();
    } else if (ev.type !== 'pointercancel') {
      selectFlag(null);
      seek(timeAtX(ev.clientX));
    }
  };
  target.addEventListener('pointermove', onMove);
  target.addEventListener('pointerup', onUp);
  target.addEventListener('pointercancel', onUp);
});

// wheel: zoom (plain) / pan (shift or horizontal scroll)
els.waveWrap.addEventListener('wheel', e => {
  if (!peaks) return;
  e.preventDefault();
  const dx = e.shiftKey ? (e.deltaX || e.deltaY) : e.deltaX;
  if (dx) {
    viewStart += dx / pxPerSec;
    clampView();
    dirty = true;
  }
  if (!e.shiftKey && e.deltaY) {
    zoomAt(e.clientX, e.deltaY < 0 ? 1.25 : 1 / 1.25);
  }
}, { passive: false });

// drag & drop: audio or flags json
window.addEventListener('dragover', e => { e.preventDefault(); document.body.classList.add('dropping'); });
window.addEventListener('dragleave', e => { if (!e.relatedTarget) document.body.classList.remove('dropping'); });
window.addEventListener('drop', e => {
  e.preventDefault();
  document.body.classList.remove('dropping');
  const file = e.dataTransfer.files[0];
  if (!file) return;
  if (/\.(json|csv)$/i.test(file.name)) importFlags(file);
  else loadAudio(file);
});

/* ============================== keyboard ============================== */

window.addEventListener('keydown', e => {
  const tag = e.target.tagName;
  if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') {
    if (e.key === 'Escape') e.target.blur();
    return;
  }

  if (e.ctrlKey || e.metaKey) {
    if (e.key === 'z' || e.key === 'Z') {
      e.preventDefault();
      undo();
    }
    return;
  }

  switch (e.key) {
    case ' ':
      e.preventDefault();
      togglePlay();
      break;
    case 'f': case 'F':
      if (peaks) addFlagAt(els.player.currentTime);
      break;
    case 'Delete': case 'Backspace':
      deleteSelected();
      break;
    case '1': setDefaultColor('red'); break;
    case '2': setDefaultColor('yellow'); break;
    case '3': setDefaultColor('green'); break;
    case '4': setDefaultColor('blue'); break;
    case 'ArrowLeft':
      e.preventDefault();
      seek(els.player.currentTime - (e.shiftKey ? 5 : 1));
      break;
    case 'ArrowRight':
      e.preventDefault();
      seek(els.player.currentTime + (e.shiftKey ? 5 : 1));
      break;
    case ',': case '<': nudgeSelected(e.shiftKey ? -0.001 : -0.010); break;
    case '.': case '>': nudgeSelected(e.shiftKey ? 0.001 : 0.010); break;
    case 'Home':
      e.preventDefault();
      seek(0);
      viewStart = 0;
      dirty = true;
      break;
    case 'Escape':
      selectFlag(null);
      break;
  }
});

function nudgeSelected(deltaSec) {
  const sel = selectedFlag();
  if (!sel) return;
  pushUndo();
  sel.time = clamp(sel.time + deltaSec, 0, duration || Infinity);
  layoutFlags();
  refreshSelection();
  scheduleSave();
}

/* ============================== boot ============================== */

new ResizeObserver(resizeCanvases).observe(els.waveWrap);
window.addEventListener('resize', resizeCanvases);
resizeCanvases();
requestAnimationFrame(frame);
