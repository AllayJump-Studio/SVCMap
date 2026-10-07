<template>
  <span class="switch-text" :class="{on: on}">{{ on ? $t('switch.on') : $t('switch.off') }}</span>
</template>

<script>
export default {
  name: "SwitchHandle",
  props: {
    on: Boolean
  }
}
</script>

<style lang="scss">
.side-menu .switch-text {
  font-weight: bold;
  color: var(--theme-fg-light);
  transition: color 0.3s;
  margin-left: 0.5em;
  user-select: none;

  &.on {
    color: var(--theme-switch-button-on);
  }
}
</style>