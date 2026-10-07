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

// 点击音效
const clickSound = new Audio(clickMp3);
clickSound.volume = 0.6;

// 放大音效
const zoomPlusSound = new Audio(zoomplusMp3);
zoomPlusSound.volume = 0.6;

// 缩小音效
const zoomMinusSound = new Audio(zoomminusMp3);
zoomMinusSound.volume = 0.6;

// 地图切换音效
const mapWriteSound = new Audio(mapwriteMp3);
mapWriteSound.volume = 0.6;

// 指南针音效
const compassSound = new Audio(compassMp3);
compassSound.volume = 0.6;

// 受伤音效（用于自由飞行）
const hurtSound = new Audio(hurtMp3);
hurtSound.volume = 0.6;

// 流动音效（用于透视）
const flowSound = new Audio(flowMp3);
flowSound.volume = 0.6;

// 平视音效（用于平视）
const flatSound = new Audio(flatMp3);
flatSound.volume = 0.4;

// 上面三个音效的箱子音效版
const inboxFlowSound = new Audio(inboxflowMp3);
inboxFlowSound.volume = 0.6;
const inboxFlatSound = new Audio(inboxflatMp3);
inboxFlatSound.volume = 0.6;
const inboxHurtSound = new Audio(inboxhurtMp3);
inboxHurtSound.volume = 0.6;

// 随机彩蛋音效拍拍
const patSound = new Audio(patOgg);
patSound.volume = 0.6;
const pat1Sound = new Audio(pat1Ogg);
pat1Sound.volume = 0.6;
const pat2Sound = new Audio(pat2Ogg);
pat2Sound.volume = 0.6;

/**
 * 播放点击音效
 */
export function playClickSound() {
  clickSound.currentTime = 0;
  clickSound.play().catch(() => {});
}

/**
 * 播放放大音效
 */
export function playZoomPlus() {
  zoomPlusSound.currentTime = 0;
  zoomPlusSound.play().catch(() => {});
}

/**
 * 播放缩小音效
 */
export function playZoomMinus() {
  zoomMinusSound.currentTime = 0;
  zoomMinusSound.play().catch(() => {});
}

/**
 * 播放地图切换音效
 */
export function playMapWrite() {
  mapWriteSound.currentTime = 0;
  mapWriteSound.play().catch(() => {});
}

/**
 * 播放指南针音效
 */
export function playCompass() {
  compassSound.currentTime = 0;
  compassSound.play().catch(() => {});
}

/**
 * 播放受伤音效（自由飞行）
 */
export function playHurt() {
  hurtSound.currentTime = 0;
  hurtSound.play().catch(() => {});
}

/**
 * 播放流动音效（透视）
 */
export function playFlow() {
  flowSound.currentTime = 0;
  flowSound.play().catch(() => {});
}

/**
 * 播放平视音效（平视）
 */
export function playFlat() {
  flatSound.currentTime = 0;
  flatSound.play().catch(() => {});
}

export function playInboxFlow() {
  inboxFlowSound.currentTime = 0;
  inboxFlowSound.play().catch(() => {});
}
export function playInboxFlat() {
  inboxFlatSound.currentTime = 0;
  inboxFlatSound.play().catch(() => {});
}
export function playInboxHurt() {
  inboxHurtSound.currentTime = 0;
  inboxHurtSound.play().catch(() => {});
}

/**
 * 随机播放一个拍打音效
 */
export function playRandomPat() {
  const sounds = [patSound, pat1Sound, pat2Sound];
  const selected = sounds[Math.floor(Math.random() * sounds.length)];
  selected.currentTime = 0;
  selected.play().catch(() => {});
}