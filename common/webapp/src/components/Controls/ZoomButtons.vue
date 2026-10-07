<template>
  <div id="zoom-buttons">
    <!-- 放大按钮（加号） -->
    <SvgButton @action="zoom(-3)">
      <svg viewBox="0 0 30 30">
        <!-- 横线（直角） -->
        <rect x="5" y="13" width="20" height="4" />
        <!-- 竖线（直角） -->
        <rect x="13" y="5" width="4" height="20" />
      </svg>
    </SvgButton>
    <!-- 缩小按钮（减号） -->
    <SvgButton @action="zoom(3)">
      <svg viewBox="0 0 30 30">
        <!-- 横线（直角） -->
        <rect x="5" y="13" width="20" height="4" />
      </svg>
    </SvgButton>
  </div>
</template>

<script>
import SvgButton from "../ControlBar/SvgButton.vue";
import { playZoomPlus, playZoomMinus } from '../../js/map/Sounds'; // 导入音效函数

export default {
  name: "ZoomButtons",
  components: {
    SvgButton
  },
  methods: {
    zoom(delta) {
      // 播放对应音效
      if (delta < 0) {
        playZoomPlus();   // 放大（负值）
      } else if (delta > 0) {
        playZoomMinus();  // 缩小（正值）
      }

      // 原有缩放逻辑
      let mouseZoom = this.$bluemap.mapViewer.controlsManager.controls?.mouseZoom;
      if (mouseZoom) {
        mouseZoom.deltaZoom += delta;
      }
    }
  }
}
</script>

<style lang="scss">
  #zoom-buttons {
    position: fixed;
    bottom: 0;
    right: 0;

    display: flex;
    flex-direction: column;

    filter: drop-shadow(1px 1px 3px rgba(0, 0, 0, 0.53));
    width: 2em;

    margin: 0.5em;
  }
</style>