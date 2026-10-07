<template>
  <div class="controls-switch">
    <SvgButton v-if="mapViewer.map.perspectiveView" :active="isPerspectiveView" @action="setPerspectiveView" :title="$t('controls.perspective.tooltip')">
      <svg viewBox="0 0 30 30">
        <polygon points="3,11 22,9 28,19 9,21">
        </polygon>
      </svg>
    </SvgButton>
    <SvgButton v-if="mapViewer.map.flatView" :active="isFlatView" @action="setFlatView" :title="$t('controls.flatView.tooltip')">
      <svg viewBox="0 0 30 30">
        <polygon points="4,4 26,4 26,26 4,26">
        </polygon>
      </svg>
    </SvgButton>
    <SvgButton v-if="mapViewer.map.freeFlightView" :active="isFreeFlight" @action="setFreeFlight" :title="$t('controls.freeFlight.tooltip')">
      <svg viewBox="0 0 30 30"><rect x="10" y="3" width="10" height="10"></rect>
        <rect x="9" y="14" width="12" height="13"></rect>
        <rect x="4" y="14" width="5" height="13"></rect>
        <rect x="21" y="14" width="5" height="13"></rect>
        <rect x="10" y="3" width="10" height="2" fill="#c7cbd1"></rect>
        <rect x="10" y="5" width="2" height="2" fill="#c7cbd1"></rect>
        <rect x="18" y="5" width="2" height="2" fill="#c7cbd1"></rect>
      </svg>
    </SvgButton>
  </div>
</template>

<script>
  import SvgButton from "./SvgButton.vue";
  import { playHurt, playFlow, playFlat } from '../../js/map/Sounds';

  export default {
    name: "ControlsSwitch",
    components: {SvgButton},
    data() {
      return {
        controls: this.$bluemap.appState.controls,
        mapViewer: this.$bluemap.mapViewer.data
      }
    },
    computed: {
      isPerspectiveView() {
        return this.controls.state === "perspective";
      },
      isFlatView() {
        return this.controls.state === "flat";
      },
      isFreeFlight() {
        return this.controls.state === "free";
      }
    },
    methods: {
      setPerspectiveView() {
        // 仅当当前不是透视视角时播放音效并切换
        if (!this.isPerspectiveView) {
          playFlow(); // 流动音效
          this.$bluemap.setPerspectiveView(500, this.isFreeFlight ? 100 : 0);
        }
      },
      setFlatView() {
        // 仅当当前不是平视视角时播放音效并切换
        if (!this.isFlatView) {
          playFlat(); // 平视音效
          this.$bluemap.setFlatView(500, this.isFreeFlight ? 100 : 0);
        }
      },
      setFreeFlight() {
        // 仅当当前不是自由飞行时播放音效并切换
        if (!this.isFreeFlight) {
          playHurt(); // 受伤音效
          this.$bluemap.setFreeFlight(500);
        }
      }
    }
  }
</script>

<style lang="scss">
  .controls-switch {
    display: flex;
  }
</style>