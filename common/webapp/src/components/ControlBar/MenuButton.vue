<template>
  <SvgButton class="menu-button" :class="{close: close, back: back}" @action="handleAction">
    <svg viewBox="0 0 30 30">
      <g>
      <path d="M6.671,7.833 L25.004,7.833 L25.004,10.754 L6.671,10.754 Z"/>
      <path d="M6.671,13.539 L25.004,13.539 L25.004,16.461 L6.671,16.461 Z"/>
      <path d="M6.671,19.245 L25.004,19.245 L25.004,22.167 L6.671,22.167 Z"/>
      </g>
    </svg>
  </SvgButton>
</template>

<script>
import SvgButton from "./SvgButton.vue";
import { playBoxOpen, playBoxClose } from '@/js/map/BoxSounds';

export default {
  name: "MenuButton",
  components: {SvgButton},
  props: {
    close: Boolean,
    back: Boolean,
  },
  methods: {
    handleAction(event) {
      if (this.close) {
        playBoxClose();   
      } else {
        playBoxOpen();  
      }
      // 传递点击事件
      this.$emit('action', event);
    }
  }
}
</script>

<style lang="scss">
  .menu-button {
    svg {
      g {
        transform-origin: center;
        transition: transform 0.3s;
      }
      path {
        transition: transform 0.3s, fill 0.3s;
        transform: translate(0, 0) rotate(0);

        &:nth-child(1) {
          transform-origin: 15px 9px;
        }

        &:nth-child(2) {
          transform-origin: 15px 15px;
        }

        &:nth-child(3) {
          transform-origin: 15px 21px;
        }
      }
    }

    &.close {
      svg {
        path:nth-child(1) {
          transform: translate(0, 5.75px) rotate(45deg);
        }
        path:nth-child(2) {
          transform: translate(-100%, 0) rotate(0);
        }
        path:nth-child(3) {
          transform: translate(0, -5.75px) rotate(-45deg);
        }
      }

      &.back {
        svg {
          g {
            transform: scale(0.75);
          }
          path:nth-child(1) {
            transform: translate(0, 10px) rotate(30deg);
          }
          path:nth-child(2) {
            transform: translate(-150%, 0) rotate(0);
          }
          path:nth-child(3) {
            transform: translate(0, -10px) rotate(-30deg);
          }
        }
      }
    }
  }
</style>