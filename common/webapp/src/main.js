/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 */

import * as Vue from 'vue';
import App from './App.vue';
import * as BlueMap from "./js/BlueMap";
import {BlueMapApp} from "./js/BlueMapApp";
import {i18nModule, loadLanguageSettings} from "./i18n";
import { getDialogueData } from './js/dialogues.js';
import { setCompleted } from './js/map/Sounds.js';

// utils
String.prototype.includesCI = function (val) {
  return this.toLowerCase().includes(val.toLowerCase());
}

// ============================================================
// 问候页面（Greeting Page）
// ============================================================

// ---- 工具函数 ----
function getToday() {
  const d = new Date();
  return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
}

function getHour() {
  return new Date().getHours();
}

// ---- 存储键名 ----
const STORAGE_KEY_FIRST = 'greeting_first_date';
const STORAGE_KEY_LAST  = 'greeting_last_date';
const STORAGE_KEY_COUNT = 'greeting_visit_count';

// ---- 检查是否应显示问候页面 ----
function shouldShowGreeting() {
  const hour = getHour();
  if (hour < 2 || hour >= 3) return false;
  const today = getToday();
  const last = localStorage.getItem(STORAGE_KEY_LAST);
  if (last === today) return false;
  const count = parseInt(localStorage.getItem(STORAGE_KEY_COUNT) || '0', 10);
  if (count >= 7) return false;
  return true;
}

// ---- 显示问候页面 ----
function showGreetingPage() {
  const today = getToday();

  const lastDateStr = localStorage.getItem(STORAGE_KEY_LAST);
  const firstDateStr = localStorage.getItem(STORAGE_KEY_FIRST);

  let first = firstDateStr;
  if (!first) {
    first = today;
    localStorage.setItem(STORAGE_KEY_FIRST, first);
  }
  let count = parseInt(localStorage.getItem(STORAGE_KEY_COUNT) || '0', 10);
  count += 1;
  if (count > 7) {
    localStorage.setItem(STORAGE_KEY_COUNT, count);
    localStorage.setItem(STORAGE_KEY_LAST, today);
    loadMap();
    return;
  }
  localStorage.setItem(STORAGE_KEY_COUNT, count);
  localStorage.setItem(STORAGE_KEY_LAST, today);

  let daysSinceLastVisit = 1;
  if (lastDateStr && lastDateStr !== today) {
    const parts = lastDateStr.split('-').map(Number);
    const lastDate = new Date(parts[0], parts[1]-1, parts[2]);
    const nowParts = today.split('-').map(Number);
    const nowDate = new Date(nowParts[0], nowParts[1]-1, nowParts[2]);
    daysSinceLastVisit = Math.floor((nowDate - lastDate) / (1000 * 60 * 60 * 24));
  }

  const firstDate = new Date(first);
  const nowDate = new Date();
  const dayIndex = Math.floor((nowDate - firstDate) / (1000 * 60 * 60 * 24)) + 1;

  const dialogues = getDialogueData(count, dayIndex, daysSinceLastVisit, first, lastDateStr, today);

  // 构建覆盖层
  const overlay = document.createElement('div');
  overlay.id = 'greeting-overlay';
  overlay.style.cssText = `
    position: fixed;
    top: 0; left: 0;
    width: 100%; height: 100%;
    z-index: 999999;
    background: #000000;
    transition: background 1.2s ease;
    cursor: default;
    font-family: 'Unifont', sans-serif;
    color: #a2d9b2;
  `;
  requestAnimationFrame(() => {
    overlay.style.background = '#fcfffc';
  });

  // ---- 角色容器（默认尺寸：500px 方形居中） ----
  const roleContainer = document.createElement('div');
  roleContainer.style.cssText = `
    position: absolute;
    top: 2em;
    left: 50%;
    transform: translateX(-50%);
    width: 500px;
    height: 500px;
    opacity: 0;
    transition: opacity 0.8s ease 0.3s;
  `;

  // ---- 四种状态图片 ----
  const normalImg = document.createElement('img');
  normalImg.src = '/assets/Allay/normal.gif';
  normalImg.draggable = false;
  normalImg.ondragstart = () => false;
  normalImg.oncontextmenu = (e) => e.preventDefault();
  normalImg.style.cssText = `
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    object-fit: contain;
    opacity: 1;
    transition: opacity 0.5s ease;
    pointer-events: none;
    user-select: none;
    -webkit-user-drag: none;
    -webkit-user-select: none;
  `;
  roleContainer.appendChild(normalImg);

  const questionImg = document.createElement('img');
  questionImg.src = '/assets/Allay/question.gif';
  questionImg.draggable = false;
  questionImg.ondragstart = () => false;
  questionImg.oncontextmenu = (e) => e.preventDefault();
  questionImg.style.cssText = `
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    object-fit: contain;
    opacity: 0;
    transition: opacity 0.5s ease;
    pointer-events: none;
    user-select: none;
    -webkit-user-drag: none;
    -webkit-user-select: none;
  `;
  roleContainer.appendChild(questionImg);

  const shrugImg = document.createElement('img');
  shrugImg.src = '/assets/Allay/shrug.gif';
  shrugImg.draggable = false;
  shrugImg.ondragstart = () => false;
  shrugImg.oncontextmenu = (e) => e.preventDefault();
  shrugImg.style.cssText = `
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    object-fit: contain;
    opacity: 0;
    transition: opacity 0.5s ease;
    pointer-events: none;
    user-select: none;
    -webkit-user-drag: none;
    -webkit-user-select: none;
  `;
  roleContainer.appendChild(shrugImg);

  const hugImg = document.createElement('img');
  hugImg.src = '/assets/Allay/hug.gif';
  hugImg.draggable = false;
  hugImg.ondragstart = () => false;
  hugImg.oncontextmenu = (e) => e.preventDefault();
  hugImg.style.cssText = `
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    object-fit: cover;
    opacity: 0;
    transition: opacity 0.5s ease;
    pointer-events: none;
    user-select: none;
    -webkit-user-drag: none;
    -webkit-user-select: none;
  `;
  roleContainer.appendChild(hugImg);

  overlay.appendChild(roleContainer);

  // ---- 底部容器（按钮 + 文字，按钮在上，文字在下） ----
  const bottomContainer = document.createElement('div');
  bottomContainer.style.cssText = `
    position: absolute;
    bottom: 6em;
    left: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    pointer-events: none;
  `;
  overlay.appendChild(bottomContainer);

  // 回应按钮容器（放在上方）
  const buttonContainer = document.createElement('div');
  buttonContainer.style.cssText = `
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    justify-content: center;
    margin-top: 0;
    min-height: 1em;
    opacity: 0;
    transition: opacity 0.8s ease 0.3s;
    pointer-events: auto;
  `;
  bottomContainer.appendChild(buttonContainer);

  // 文字展示区（放在下方）
  const textContainer = document.createElement('div');
  textContainer.style.cssText = `
    padding: 0 20px;
    text-align: center;
    color: #a2d9b2;
    opacity: 0;
    transition: opacity 0.8s ease 0.3s;
    min-height: 1em;
    line-height: 1.6;
    font-family: 'Unifont', sans-serif;
    word-break: break-word;
    white-space: pre-wrap;
    pointer-events: auto;
    max-width: 60%;
    margin-top: 2em;
  `;
  if (window.innerWidth < 768) {
    textContainer.style.maxWidth = '90%';
    textContainer.style.fontSize = '1.2rem';
  } else {
    textContainer.style.fontSize = '1.6rem';
  }
  bottomContainer.appendChild(textContainer);

  document.body.appendChild(overlay);

  // ---- 背景音乐 ----
  const echoAudio = new Audio('/assets/Echo.mp3');
  echoAudio.loop = true;
  echoAudio.volume = 0.8;
  echoAudio.addEventListener('error', (e) => {
    console.warn('俺音响怎么坏了：（', e);
  });

  // ---- 入场时序 ----
  setTimeout(() => {
    roleContainer.style.opacity = '1';
  }, 3000);

  // ---- 对话状态 ----
  let currentIndex = 0;
  let isAnimating = false;
  let animationTimer = null;
  let isEnding = false;
  let roleFadeTimer = null;
  let endSequenceTimer = null;
  let startJourneyAudio = null;
  let pendingButtons = null;
  let isAwaitingButton = false;
  let hasStarted = false;
  let isFading = false;
  let isEndWaiting = false;
  let hugActive = false;
  let hugTimer = null;
  const clickSound = new Audio('/assets/echo-click.mp3');
  clickSound.volume = 1.0;

  // ---- 重置角色容器为默认尺寸 ----
  function resetRoleSize() {
    if (!hugActive) return;
    hugActive = false;
    roleContainer.style.width = '500px';
    roleContainer.style.height = '500px';
    roleContainer.style.top = '2em';
    roleContainer.style.left = '50%';
    roleContainer.style.transform = 'translateX(-50%)';
    // 恢复普通图片的 object-fit
    normalImg.style.objectFit = 'contain';
    questionImg.style.objectFit = 'contain';
    shrugImg.style.objectFit = 'contain';
    hugImg.style.objectFit = 'cover';
  }

  // ---- 执行 hug 状态切换动画 ----
  function performHugTransition() {
    if (hugTimer) {
      clearTimeout(hugTimer);
      hugTimer = null;
    }
    isAnimating = true;

    // 1. 渐隐所有图片（3秒）
    normalImg.style.transition = 'opacity 3s ease';
    questionImg.style.transition = 'opacity 3s ease';
    shrugImg.style.transition = 'opacity 3s ease';
    hugImg.style.transition = 'opacity 3s ease';
    normalImg.style.opacity = '0';
    questionImg.style.opacity = '0';
    shrugImg.style.opacity = '0';
    hugImg.style.opacity = '0';

    hugTimer = setTimeout(() => {
      // 2. 放大角色容器（瞬间）
      hugActive = true;
      roleContainer.style.width = '100vw';
      roleContainer.style.height = '100vh';
      roleContainer.style.top = '0';
      roleContainer.style.left = '0';
      roleContainer.style.transform = 'none';
      hugImg.style.objectFit = 'cover';
      hugImg.style.opacity = '1';
      normalImg.style.opacity = '0';
      questionImg.style.opacity = '0';
      shrugImg.style.opacity = '0';

      // 3. 渐显 hug 图片（3秒）
      hugImg.style.transition = 'opacity 3s ease';
      hugImg.style.opacity = '1';

      hugTimer = setTimeout(() => {
        isAnimating = false;
        hugTimer = null;
        // 恢复普通过渡时间
        normalImg.style.transition = 'opacity 0.5s ease';
        questionImg.style.transition = 'opacity 0.5s ease';
        shrugImg.style.transition = 'opacity 0.5s ease';
        hugImg.style.transition = 'opacity 0.5s ease';
      }, 3000);
    }, 3000);
  }

  // ---- 普通状态切换（normal / question / shrug） ----
  function switchToNormalState(state) {
    // 如果当前是 hug 状态，先重置尺寸
    if (hugActive) {
      resetRoleSize();
    }
    // 取消可能正在进行的 hug 动画
    if (hugTimer) {
      clearTimeout(hugTimer);
      hugTimer = null;
      isAnimating = false;
    }
    // 隐藏所有图片
    normalImg.style.opacity = '0';
    questionImg.style.opacity = '0';
    shrugImg.style.opacity = '0';
    hugImg.style.opacity = '0';
    // 根据状态显示对应图片
    if (state === 'question') {
      questionImg.style.opacity = '1';
    } else if (state === 'shrug') {
      shrugImg.style.opacity = '1';
    } else { // normal 或未知状态
      normalImg.style.opacity = '1';
    }
  }

  // ---- 更新对话 ----
  function updateDialogue(index) {
    if (index >= dialogues.length) {
      closeGreeting();
      return;
    }
    if (animationTimer) {
      clearTimeout(animationTimer);
      animationTimer = null;
      isAnimating = false;
    }
    const entry = dialogues[index];
    // 处理角色状态
    if (entry.state === 'hug') {
      if (!hugActive && !isAnimating) {
        performHugTransition();
      } else if (hugActive) {
        // 确保显示 hug 图片
        normalImg.style.opacity = '0';
        questionImg.style.opacity = '0';
        shrugImg.style.opacity = '0';
        hugImg.style.opacity = '1';
      }
    } else {
      // 普通状态（normal / question / shrug）
      switchToNormalState(entry.state || 'normal');
    }

    // 如果当前句触发渐隐（倒数第二句）
    if (entry.fadeStart && !isFading) {
      isFading = true;
      roleContainer.style.transition = 'opacity 5s ease';
      roleContainer.style.opacity = '0';
      setTimeout(() => {
        isFading = false;
      }, 5000);
    }

    const oldSpans = textContainer.querySelectorAll('span');
    if (oldSpans.length > 0) {
      isAnimating = true;
      oldSpans.forEach(span => {
        span.style.opacity = '0';
      });
      animationTimer = setTimeout(() => {
        textContainer.innerHTML = '';
        showNewText(entry);
      }, 300 + 200);
    } else {
      showNewText(entry);
    }
  }

  // ---- 显示新文本 ----
  function showNewText(entry) {
    // 如果是最后一句，先移动容器到居中位置（动画开始前）
    if (entry.isEnd) {
      const parent = textContainer.parentNode;
      if (parent) parent.removeChild(textContainer);
      overlay.appendChild(textContainer);
      textContainer.style.position = 'absolute';
      textContainer.style.top = '50%';
      textContainer.style.left = '50%';
      textContainer.style.transform = 'translate(-50%, -50%)';
      textContainer.style.marginTop = '0';
      const isMobile = window.innerWidth < 768;
      textContainer.style.maxWidth = isMobile ? '90%' : '60%';
      textContainer.style.fontSize = isMobile ? '1.2rem' : '1.6rem';
      bottomContainer.style.display = 'none';
    }

    textContainer.innerHTML = '';
    const text = entry.text;
    const chars = text.split('');

    const isMobile = window.innerWidth < 768;
    textContainer.style.maxWidth = isMobile ? '90%' : '60%';
    textContainer.style.fontSize = isMobile ? '1.2rem' : '1.6rem';

    const spans = [];
    chars.forEach((char) => {
      const span = document.createElement('span');
      span.textContent = char;
      span.style.cssText = `
        display: inline-block;
        opacity: 0;
        transform: translateY(-20px);
        transition: opacity 0.9s ease, transform 1.1s ease;
      `;
      textContainer.appendChild(span);
      spans.push(span);
    });

    isAnimating = true;
    let delay = 0;
    spans.forEach((span) => {
      setTimeout(() => {
        span.style.opacity = '1';
        span.style.transform = 'translateY(0)';
      }, delay);
      delay += 60;
    });

    const lastDelay = delay - 60 + 1100;
    animationTimer = setTimeout(() => {
      isAnimating = false;
      animationTimer = null;
      if (entry.buttons && entry.buttons.length > 0) {
        pendingButtons = entry.buttons;
        isAwaitingButton = true;
        buttonContainer.innerHTML = '';
        buttonContainer.style.minHeight = '40px';
      } else {
        pendingButtons = null;
        isAwaitingButton = false;
        buttonContainer.innerHTML = '';
        buttonContainer.style.minHeight = '40px';
      }

      if (entry.isEnd) {
        isEndWaiting = true;
        setTimeout(() => {
          isEndWaiting = false;
        }, 3000);
      }
    }, lastDelay);

    buttonContainer.innerHTML = '';
    buttonContainer.style.minHeight = '40px';
  }

  // ---- 显示按钮 ----
  function showButtons() {
    if (!pendingButtons || pendingButtons.length === 0 || isAnimating) return;
    const buttonsCopy = pendingButtons;
    pendingButtons = null;
    const oldSpans = textContainer.querySelectorAll('span');
    if (oldSpans.length > 0) {
      isAnimating = true;
      oldSpans.forEach(span => {
        span.style.opacity = '0';
      });
      setTimeout(() => {
        textContainer.innerHTML = '';
        animateButtonsSequentially(buttonsCopy);
      }, 300);
    } else {
      animateButtonsSequentially(buttonsCopy);
    }
  }

  // ---- 顺序显示按钮字符 ----
  function animateButtonsSequentially(buttons) {
    buttonContainer.innerHTML = '';
    const btnWrappers = [];
    const fontSize = textContainer.style.fontSize || '1.6rem';
    buttons.forEach((btn) => {
      const btnWrapper = document.createElement('span');
      btnWrapper.style.cssText = `
        display: inline-block;
        cursor: pointer;
        font-family: 'Unifont', sans-serif;
        color: #a2d9b2;
        font-size: ${fontSize};
        padding: 4px 8px;
        user-select: none;
        transition: color 0s;
      `;
      btnWrapper.addEventListener('mouseenter', () => {
        btnWrapper.style.color = '#ffc36b';
      });
      btnWrapper.addEventListener('mouseleave', () => {
        btnWrapper.style.color = '#a2d9b2';
      });
      btnWrapper.addEventListener('click', (e) => {
        e.stopPropagation();
        if (isAnimating || isEnding) return;
        clickSound.currentTime = 0;
        clickSound.play().catch(() => {});
        const idx = btnWrappers.indexOf(btnWrapper);
        const btnData = buttons[idx];
        currentIndex = btnData.nextIndex;
        isAwaitingButton = false;
        pendingButtons = null;
        updateDialogue(currentIndex);
      });
      buttonContainer.appendChild(btnWrapper);
      btnWrappers.push(btnWrapper);
    });

    isAnimating = true;
    let totalDelay = 0;
    btnWrappers.forEach((wrapper, idx) => {
      const btn = buttons[idx];
      const chars = btn.label.split('');
      chars.forEach((char, charIdx) => {
        const charSpan = document.createElement('span');
        charSpan.textContent = char;
        charSpan.style.cssText = `
          display: inline-block;
          opacity: 0;
          transition: opacity 0.9s ease;
        `;
        wrapper.appendChild(charSpan);
        const delay = totalDelay + charIdx * 60;
        setTimeout(() => {
          charSpan.style.opacity = '1';
        }, delay);
      });
      totalDelay += chars.length * 60 + 200;
    });

    const lastDelay = totalDelay + 900;
    animationTimer = setTimeout(() => {
      isAnimating = false;
      animationTimer = null;
      isAwaitingButton = false;
    }, lastDelay);
  }

  // ---- 推进对话 ----
  function nextDialogue() {
    const entry = dialogues[currentIndex];
    if (entry.fadeStart && isFading) {
      return;
    }
    if (entry && entry.autoNext !== undefined) {
      currentIndex = entry.autoNext;
    } else {
      currentIndex++;
    }
    updateDialogue(currentIndex);
  }

  // ---- 结束流程 ----
  function startEndSequence() {
    if (isEnding) return;
    isEnding = true;

    echoAudio.pause();
    echoAudio.currentTime = 0;

    overlay.style.transition = 'none';
    overlay.style.background = '#000000';

    roleContainer.style.transition = 'none';
    roleContainer.style.opacity = '0';
    textContainer.style.transition = 'none';
    textContainer.style.opacity = '0';
    buttonContainer.style.transition = 'none';
    buttonContainer.style.opacity = '0';

    void overlay.offsetHeight;

    startJourneyAudio = new Audio('/assets/StartJourney.ogg');
    startJourneyAudio.loop = false;
    startJourneyAudio.volume = 0.6;
    startJourneyAudio.play().catch(() => {});

    loadMap();

    endSequenceTimer = setTimeout(() => {
      overlay.style.transition = 'none';
      overlay.style.background = '#7FB2D0';
      void overlay.offsetHeight;

      overlay.style.transition = 'opacity 0.8s ease';
      overlay.style.opacity = '0';

      setTimeout(() => {
        if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        if (startJourneyAudio) {
          startJourneyAudio.pause();
          startJourneyAudio.currentTime = 0;
        }
      }, 800);
    }, 4000);
  }

  // ---- 点击事件 ----
  overlay.addEventListener('click', (e) => {
    if (isAnimating) return;

    if (!hasStarted) {
      hasStarted = true;
      echoAudio.play().catch(err => console.warn('Echo播放被阻止:', err));
      textContainer.style.opacity = '1';
      buttonContainer.style.opacity = '1';
      setTimeout(() => {
        updateDialogue(0);
      }, 800);
      return;
    }

    const entry = dialogues[currentIndex];
    if (entry.buttons && entry.buttons.length > 0) {
      if (isAwaitingButton) {
        showButtons();
      }
      return;
    }

    if (entry.isEnd) {
      if (isEndWaiting) return;
      startEndSequence();
      return;
    }

    if (entry.fadeStart && isFading) {
      return;
    }

    nextDialogue();
  });

  // ---- 关闭问候 ----
  function closeGreeting() {
    if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
    echoAudio.pause();
    echoAudio.currentTime = 0;
    loadMap();
  }

  // ---- 清理 ----
  window.addEventListener('beforeunload', () => {
    if (echoAudio) echoAudio.pause();
    if (startJourneyAudio) startJourneyAudio.pause();
    if (endSequenceTimer) clearTimeout(endSequenceTimer);
    if (roleFadeTimer) clearTimeout(roleFadeTimer);
    if (animationTimer) clearTimeout(animationTimer);
    if (hugTimer) clearTimeout(hugTimer);
  });
}

// ---- 主加载流程 ----
let mapLoaded = false;

async function loadMap() {
  if (mapLoaded) return;
  mapLoaded = true;

  try {
    const bluemap = new BlueMapApp(document.getElementById("map-container"));
    window.bluemap = bluemap;
    window.BlueMap = BlueMap;

    const vue = Vue.createApp(App, {
      i18nModule,
      render: h => h(App)
    });
    vue.config.globalProperties.$bluemap = bluemap;

    vue.use(i18nModule);
    await loadLanguageSettings();

    const app = vue.mount('#app');
    await app.$nextTick();
    await bluemap.load();

  } catch (e) {
    console.error("加载地图时出错！", e);
    const errorMessages = [
      "嗨，你发现了一个罕见的错误呢。报告给维护组？",
      "这个石山是我写的。——Allager_LSSP",
      "惊世骇俗的报错，爱来自SVC。"
    ];
    const randomMsg = errorMessages[Math.floor(Math.random() * errorMessages.length)];
    console.error(randomMsg);
    document.body.innerHTML = `
    <div id="bm-app-err">
      <div>
        <img src="assets/logo.png" alt="bluemap logo">
        <div class="bm-app-err-main" style="color: #7be9e3;">加载地图时出错！</div>
        <div class="bm-app-err-hint" style="color: #7be9e3;">请确认你已启用 <a href="https://get.webgl.org/webgl2/" style="color: #7be9e3;">WebGL2</a> 支持</div>
      </div>
    </div>
  `;
  }
}

async function load() {
  const count = parseInt(localStorage.getItem(STORAGE_KEY_COUNT) || '0', 10);
  if (count >= 7) {
    setCompleted(true);
  }

  if (shouldShowGreeting()) {
    showGreetingPage();
    return;
  }
  loadMap();
}

load().catch(error => console.error(error));