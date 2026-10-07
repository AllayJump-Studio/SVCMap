<template>
  <Transition
    name="side-menu"
    @after-enter="onAfterEnter"
    @before-leave="onBeforeLeave"
    @after-leave="onAfterLeave"
  >
    <div v-if="open" class="side-menu">
      <MenuButton
        :close="open && rendered"
        :back="back"
        @action="$emit('back', $event)"
      />
      <MenuButton
        v-if="open && back"
        class="full-close"
        :close="true"
        @action="$emit('close', $event)"
      />
      <div class="title">{{ displayTitle }}</div>
      <div class="content">
        <slot />
      </div>
    </div>
  </Transition>
</template>

<script>
import MenuButton from "../ControlBar/MenuButton.vue";

export default {
  name: "SideMenu",
  components: { MenuButton },
  props: {
    title: { type: String, default: "Menu" },
    open: { type: Boolean, default: true },
    back: Boolean,
  },
  data() {
    return {
      rendered: false,
      displayTitle: this.title,
    };
  },
  watch: {
    open(newVal) {
      if (newVal) {
        this.displayTitle = this.title;
      }
    },
    title(newVal) {
      if (this.open) {
        this.displayTitle = newVal;
      }
    },
  },
  methods: {
    onAfterEnter() {
      this.rendered = true;
    },
    onBeforeLeave() {
      this.rendered = false;
    },
    onAfterLeave() {
    },
  },
};
</script>

<style lang="scss">
@import "/src/scss/variables.scss";

.side-menu {
  position: fixed;
  top: 0;
  left: 0;
  overflow: hidden;
  pointer-events: auto;
  width: 100%;
  max-width: 20em;
  height: 100%;
  filter: drop-shadow(1px 1px 3px #0008);
  background-color: var(--theme-bg);
  color: var(--theme-fg);

  transform: translateX(0);

  // 进入起始状态
  &-enter {
    transform: translateX(-100%);
    pointer-events: none;
    * {
      pointer-events: none !important;
    }
  }

  // 进入动画（减速缓动）
  &-enter-active {
    transition: transform 250ms cubic-bezier(0, 0, 0, 1);
    will-change: transform;
  }

  // 进入结束状态
  &-enter-to {
    transform: translateX(0);
    pointer-events: auto;
  }

  // 离开起始状态（显示）
  &-leave {
    transform: translateX(0);
  }

  // 离开动画（加速缓动）
  &-leave-active {
    transition: transform 250ms cubic-bezier(1, 0, 1, 1);
    will-change: transform;
  }

  // 离开结束状态
  &-leave-to {
    transform: translateX(-100%);
    pointer-events: none;
    * {
      pointer-events: none !important;
    }
  }

  // ---------- 原有样式 ----------
  > .menu-button {
    position: absolute;
    top: 0;
    left: 0;
    margin: 0.5em;
    @media (max-width: $mobile-break) {
      margin: 0;
    }
    &.full-close {
      right: 0;
      left: unset;
    }
  }

  > .title {
    line-height: 2em;
    text-align: center;
    background-color: inherit;
    border-bottom: solid 2px #463e32c5;
    padding: 0.5em;
    @media (max-width: $mobile-break) {
      padding: 0;
    }
  }

  > .content {
    position: relative;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0.5em;
    height: calc(100% - 4em - 1px);
    @media (max-width: $mobile-break) {
      height: calc(100% - 3em - 1px);
    }
    hr {
      border: none;
      border-bottom: solid 2px #dcac62c9;
      margin: 0.5em 0;
    }
  }
}
</style>