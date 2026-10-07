<template>
  <div id="ff-mobile-controls" v-show="isMobileDevice">
    <!-- 前后左右移动（十字布局） -->
    <div class="move-fields">
      <div class="button up" @touchstart.passive="forward = 1; forwardPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
      <div class="button left" @touchstart.passive="side = -1; sidePointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50" class="left">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
      <div class="button right" @touchstart.passive="side = 1; sidePointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50" class="right">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
      <div class="button down" @touchstart.passive="forward = -1; forwardPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50" class="down">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
    </div>

    <!-- 上升/下降（右侧垂直排列） -->
    <div class="height-fields">
      <div class="button up" @touchstart.passive="up = 1; upPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
      <div class="button down" @touchstart.passive="up = -1; upPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50" class="down">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "FreeFlightMobileControls",
  data() {
    return {
      // 检测是否为移动设备（手机/平板）
      isMobileDevice: /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent),
      forward: 0,
      forwardPointer: -1,
      side: 0,
      sidePointer: -1,
      up: 0,
      upPointer: -1,
    }
  },
  methods: {
    onTouchStop(evt) {
      for (const touch of evt.changedTouches) {
        if (touch.identifier === this.forwardPointer) this.forward = 0;
        if (touch.identifier === this.sidePointer) this.side = 0;
        if (touch.identifier === this.upPointer) this.up = 0;
      }
    },
    onFrame(evt) {
      const cm = this.$bluemap.mapViewer.controlsManager;
      const delta = evt.detail.delta;
      // 前后
      cm.position.x += this.forward * Math.sin(cm.rotation) * delta * 0.02;
      cm.position.z += this.forward * -Math.cos(cm.rotation) * delta * 0.02;
      // 左右
      cm.position.x += this.side * Math.cos(cm.rotation) * delta * 0.02;
      cm.position.z += this.side * Math.sin(cm.rotation) * delta * 0.02;
      // 升降
      cm.position.y += this.up * delta * 0.01;
    }
  },
  mounted() {
    window.addEventListener("touchend", this.onTouchStop);
    window.addEventListener("touchcancel", this.onTouchStop);
    this.$bluemap.events.addEventListener("bluemapRenderFrame", this.onFrame);
  },
  beforeUnmount() {
    window.removeEventListener("touchend", this.onTouchStop);
    window.removeEventListener("touchcancel", this.onTouchStop);
    this.$bluemap.events.removeEventListener("bluemapRenderFrame", this.onFrame);
  }
}
</script>

<style lang="scss">
#ff-mobile-controls {
  position: fixed;
  bottom: 0.5em;
  left: 0.5em;
  z-index: 1000;
  font-size: 8vw; /* 整体缩小 */
  display: flex;
  align-items: flex-end;
  gap: 0.5em;
  pointer-events: none;

  @media (orientation: portrait) {
    font-size: 8vh;
  }

  .move-fields {
    display: grid;
    grid-template-columns: 1.2em 1.2em 1.2em;
    grid-template-rows: 1.2em 1.2em 1.2em;
    gap: 0.1em;
    pointer-events: auto;

    .button {
      width: 100%;
      height: 100%;
      opacity: 0.5;
      cursor: pointer;
      touch-action: none;

      svg {
        display: block;
        width: 100%;
        height: 100%;
        fill: #ffffff99;
        transition: fill 0.1s;
        &.down { transform: scaleY(-1); }
        &.left { transform: rotate(-90deg); }
        &.right { transform: rotate(90deg); }
        &:active {
          fill: var(--theme-bg-light);
          opacity: 0.8;
        }
      }
    }

    .up { grid-column: 2; grid-row: 1; }
    .left { grid-column: 1; grid-row: 2; }
    .right { grid-column: 3; grid-row: 2; }
    .down { grid-column: 2; grid-row: 3; }
  }

  .height-fields {
    display: flex;
    flex-direction: column;
    gap: 0.1em;
    pointer-events: auto;

    .button {
      width: 1.2em;
      height: 1.2em;
      opacity: 0.5;
      cursor: pointer;
      touch-action: none;

      svg {
        display: block;
        width: 100%;
        height: 100%;
        fill: #ffffff99;
        transition: fill 0.1s;
        &.down { transform: scaleY(-1); }
        &:active {
          fill: var(--theme-bg-light);
          opacity: 0.8;
        }
      }
    }
  }
}
</style>