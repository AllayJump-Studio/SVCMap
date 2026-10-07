import clickMp3 from '../../assets/click.mp3';
import zoomplusMp3 from '../../assets/zoomplus.mp3';
import zoomminusMp3 from '../../assets/zoomminus.mp3';
import mapwriteMp3 from '../../assets/mapwrite.mp3';
import compassMp3 from '../../assets/compass.mp3';
import hurtMp3 from '../../assets/hurt.mp3';
import flowMp3 from '../../assets/flow.mp3';
import flatMp3 from '../../assets/flat.mp3';
import inboxflowMp3 from '../../assets/inboxflow.mp3';
import inboxflatMp3 from '../../assets/inboxflat.mp3';
import inboxhurtMp3 from '../../assets/inboxhurt.mp3';
import patOgg from '../../assets/pat.ogg';
import pat1Ogg from '../../assets/pat1.ogg';
import pat2Ogg from '../../assets/pat2.ogg';

// ---- 通关音效（AllayTreasures） ----
import click1 from '../../assets/AllayTreasures/click/click1.mp3';
import click2 from '../../assets/AllayTreasures/click/click2.mp3';
import click3 from '../../assets/AllayTreasures/click/click3.mp3';
import click4 from '../../assets/AllayTreasures/click/click4.mp3';
import click5 from '../../assets/AllayTreasures/click/click5.mp3';
import click6 from '../../assets/AllayTreasures/click/click6.mp3';
import click7 from '../../assets/AllayTreasures/click/click7.mp3';
import click8 from '../../assets/AllayTreasures/click/click8.mp3';
import click9 from '../../assets/AllayTreasures/click/click9.mp3';
import click10 from '../../assets/AllayTreasures/click/click10.mp3';
import click11 from '../../assets/AllayTreasures/click/click11.mp3';
import click12 from '../../assets/AllayTreasures/click/click12.mp3';
import click13 from '../../assets/AllayTreasures/click/click13.mp3';
import click14 from '../../assets/AllayTreasures/click/click14.mp3';

import hurt1 from '../../assets/AllayTreasures/hurt/hurt1.mp3';
import hurt2 from '../../assets/AllayTreasures/hurt/hurt2.mp3';
import hurt3 from '../../assets/AllayTreasures/hurt/hurt3.mp3';
import hurt4 from '../../assets/AllayTreasures/hurt/hurt4.mp3';

import ringingMp3 from '../../assets/AllayTreasures/Ringing.mp3';

// ---- 原有音效（音量不变） ----
const clickSound = new Audio(clickMp3);
clickSound.volume = 0.6;

const zoomPlusSound = new Audio(zoomplusMp3);
zoomPlusSound.volume = 0.6;

const zoomMinusSound = new Audio(zoomminusMp3);
zoomMinusSound.volume = 0.6;

const mapWriteSound = new Audio(mapwriteMp3);
mapWriteSound.volume = 0.6;

const compassSound = new Audio(compassMp3);
compassSound.volume = 0.6;

const hurtSound = new Audio(hurtMp3);
hurtSound.volume = 0.6;

const flowSound = new Audio(flowMp3);
flowSound.volume = 0.6;

const flatSound = new Audio(flatMp3);
flatSound.volume = 0.4;

const inboxFlowSound = new Audio(inboxflowMp3);
inboxFlowSound.volume = 0.6;
const inboxFlatSound = new Audio(inboxflatMp3);
inboxFlatSound.volume = 0.4;
const inboxHurtSound = new Audio(inboxhurtMp3);
inboxHurtSound.volume = 0.6;

const patSound = new Audio(patOgg);
patSound.volume = 0.6;
const pat1Sound = new Audio(pat1Ogg);
pat1Sound.volume = 0.6;
const pat2Sound = new Audio(pat2Ogg);
pat2Sound.volume = 0.6;

// ---- 通关音效音频对象（音量均为1.0） ----
const clickTreasureSounds = [
  new Audio(click1),
  new Audio(click2),
  new Audio(click3),
  new Audio(click4),
  new Audio(click5),
  new Audio(click6),
  new Audio(click7),
  new Audio(click8),
  new Audio(click9),
  new Audio(click10),
  new Audio(click11),
  new Audio(click12),
  new Audio(click13),
  new Audio(click14)
];
clickTreasureSounds.forEach(s => s.volume = 1.0);

const hurtTreasureSounds = [
  new Audio(hurt1),
  new Audio(hurt2),
  new Audio(hurt3),
  new Audio(hurt4)
];
hurtTreasureSounds.forEach(s => s.volume = 1.0);

// ---- 使用 Web Audio API 实现纯音高变化 ----
let audioCtx = null;
let ringingBuffer = null;

// 预加载 Ringing.mp3 并解码为 AudioBuffer
async function loadRingingBuffer() {
  if (ringingBuffer) return;
  try {
    const response = await fetch(ringingMp3);
    const arrayBuffer = await response.arrayBuffer();
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    ringingBuffer = await audioCtx.decodeAudioData(arrayBuffer);
  } catch (e) {
    console.warn('加载 Ringing 音效失败:', e);
  }
}

// 在用户交互后初始化（例如首次点击时）
function ensureAudioContext() {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume().catch(() => {});
  }
}

// 播放铃声，随机音高 0.5~1.7 倍（纯音高变化，速度不变）
function playRinging() {
  if (!isCompleted) return;
  if (!ringingBuffer) {
    // 如果缓冲未加载，尝试同步加载（但通常已在初始化时加载）
    loadRingingBuffer().then(() => {
      playRinging();
    });
    return;
  }
  try {
    ensureAudioContext();
    const source = audioCtx.createBufferSource();
    source.buffer = ringingBuffer;
    // 计算分偏移：0.5 倍音高 = -1200 分，1.7 倍音高 ≈ +1000 分
    const randomPitch = 0.5 + Math.random() * 1.2; // 0.5 ~ 1.7
    const cents = 1200 * Math.log2(randomPitch);
    source.detune.value = cents;
    const gainNode = audioCtx.createGain();
    gainNode.gain.value = 0.5; // 音量，可调节
    source.connect(gainNode);
    gainNode.connect(audioCtx.destination);
    source.start();
  } catch (e) {
    console.warn('播放铃声失败:', e);
  }
}

// ---- 通关状态 ----
let isCompleted = false;

export function setCompleted(completed) {
  isCompleted = completed;
  if (isCompleted) {
    // 预加载铃声缓冲（但不播放）
    loadRingingBuffer();
  }
}

// ---- 导出的音效函数 ----
export function playClickSound() {
  if (isCompleted) {
    const idx = Math.floor(Math.random() * clickTreasureSounds.length);
    const sound = clickTreasureSounds[idx];
    sound.currentTime = 0;
    sound.play().catch(() => {});
    playRinging();
  } else {
    clickSound.currentTime = 0;
    clickSound.play().catch(() => {});
  }
}

export function playHurt() {
  if (isCompleted) {
    const idx = Math.floor(Math.random() * hurtTreasureSounds.length);
    const sound = hurtTreasureSounds[idx];
    sound.currentTime = 0;
    sound.play().catch(() => {});
    playRinging();
  } else {
    hurtSound.currentTime = 0;
    hurtSound.play().catch(() => {});
  }
}

export function playZoomPlus() {
  zoomPlusSound.currentTime = 0;
  zoomPlusSound.play().catch(() => {});
  playRinging();
}

export function playZoomMinus() {
  zoomMinusSound.currentTime = 0;
  zoomMinusSound.play().catch(() => {});
  playRinging();
}

export function playMapWrite() {
  mapWriteSound.currentTime = 0;
  mapWriteSound.play().catch(() => {});
  playRinging();
}

export function playCompass() {
  compassSound.currentTime = 0;
  compassSound.play().catch(() => {});
  playRinging();
}

export function playFlow() {
  flowSound.currentTime = 0;
  flowSound.play().catch(() => {});
  playRinging();
}

export function playFlat() {
  flatSound.currentTime = 0;
  flatSound.play().catch(() => {});
  playRinging();
}

export function playInboxFlow() {
  inboxFlowSound.currentTime = 0;
  inboxFlowSound.play().catch(() => {});
  playRinging();
}

export function playInboxFlat() {
  inboxFlatSound.currentTime = 0;
  inboxFlatSound.play().catch(() => {});
  playRinging();
}

export function playInboxHurt() {
  inboxHurtSound.currentTime = 0;
  inboxHurtSound.play().catch(() => {});
  playRinging();
}

export function playRandomPat() {
  const sounds = [patSound, pat1Sound, pat2Sound];
  const selected = sounds[Math.floor(Math.random() * sounds.length)];
  selected.currentTime = 0;
  selected.play().catch(() => {});
  playRinging();
}