<template>
<div class="switch-button" @click="handleClick">
  <div class="label"><slot /></div>
  <span class="switch-text">{{ $t('switch.' + (on ? 'on' : 'off')) }}</span>
</div>
</template>

<script>
import { playClickSound } from '../../js/map/Sounds';

export default {
  name: "SwitchButton",
  props: {
    on: Boolean
  },
  methods: {
    handleClick(event) {
      playClickSound();          // 播放点击音效
      this.$emit('action', event); // 触发父组件的 action 事件
    }
  }
}
</script>

<style lang="scss">
.side-menu .switch-button {
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

  > .switch-text {
    margin-left: 1em;
    font-weight: bold;
    color: var(--theme-fg-light);
    transition: color 0.3s;
  }

  &:hover {
    background-color: var(--theme-bg-hover);
  }
}
</style>