<template>
  <div class="svg-button" :class="{active: active}" @click="handleClick">
    <slot />
  </div>
</template>

<script>
import { playClickSound } from '@/js/map/Sounds'

export default {
  name: "SvgButton",
  props: {
    active: Boolean,
  },
  methods: {
    handleClick(event) {
      // 播放点击音效
      playClickSound();
      // 向上传递点击事件
      this.$emit('action', event);
    }
  }
}
</script>

<style lang="scss">
.svg-button {
  position: relative;
  pointer-events: auto;
  overflow: hidden;
  cursor: pointer;

  min-width: 2em;
  min-height: 2em;

  background-color: var(--theme-bg);
  color: var(--theme-fg);

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  &.active {
    background-color: var(--theme-bg-light);
  }

  &:active {
    background-color: #d9d9d9c4;
    color: var(--theme-bg);
  }

  svg {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);

    height: 1.8em;

    fill: var(--theme-fg-light);
  }

  &:active {
    svg {
      fill: var(--theme-bg-light);
    }
  }
}
</style>