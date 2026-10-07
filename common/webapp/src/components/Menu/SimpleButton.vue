<template>
<div class="simple-button" :class="{active: active}" @click="handleClick">
  <div class="label"><slot /></div>
  <div class="submenu-icon" v-if="submenu">
    <svg viewBox="0 0 30 30">
      <rect x="9" y="5" width="4" height="4" />
      <rect x="13" y="9" width="4" height="4" />
      <rect x="17" y="13" width="4" height="4" />
      <rect x="13" y="17" width="4" height="4" />
      <rect x="9" y="21" width="4" height="4" />
    </svg>
  </div>
</div>
</template>

<script>
import { playClickSound } from '../../js/map/Sounds'; // 根据实际路径调整

export default {
  name: "SimpleButton",
  props: {
    submenu: Boolean,
    active: {
      type: Boolean,
      default: false,
    }
  },
  methods: {
    handleClick(event) {
      playClickSound();            // 播放音效
      this.$emit('action', event); // 触发父组件事件
    }
  }
}
</script>

<style lang="scss">

.side-menu .simple-button {
  cursor: pointer;
  user-select: none;
  display: flex;
  line-height: 2em;

  padding: 0 0.5em;

  > .label {
    flex-grow: 1;

    white-space: nowrap;
    overflow-x: hidden;
    text-overflow: ellipsis;
  }

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  &.active {
    background-color: var(--theme-bg-light);
  }

  > .submenu-icon {
    width: 2em;
    height: 2em;

    flex-shrink: 0;

    margin-right: -0.5em;

    > svg {
      fill: var(--theme-fg-light);

      path:nth-child(1) {
        transform-origin: 15px 9px;
        transform: translate(0, 10px) rotate(-30deg);
      }
      path:nth-child(2) {
        transform-origin: 15px 21px;
        transform: translate(0, -10px) rotate(30deg);
      }

      transform: scale(0.75);
    }
  }

  &:active {
    background-color: #ffffff66;
    color: var(--theme-bg);


    > .submenu-icon > svg {
      fill: var(--theme-bg-light);
    }
  }
}

</style>