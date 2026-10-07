// common/webapp/src/js/map/BoxSounds.js

import boxOpenMp3 from '@/assets/boxopen.mp3';
import boxCloseMp3 from '@/assets/boxclose.mp3';

// 预创建音频实例，避免重复创建
const boxOpenSound = new Audio(boxOpenMp3);
const boxCloseSound = new Audio(boxCloseMp3);

// 设置默认音量（可根据需要调整）
boxOpenSound.volume = 0.6;
boxCloseSound.volume = 0.6;

/**
 * 播放开箱音效
 */
export function playBoxOpen() {
  boxOpenSound.currentTime = 0;
  boxOpenSound.play().catch(() => {
    // 静默处理浏览器自动播放策略拦截
  });
}

/**
 * 播放关箱音效
 */
export function playBoxClose() {
  boxCloseSound.currentTime = 0;
  boxCloseSound.play().catch(() => {
    // 静默处理浏览器自动播放策略拦截
  });
}