<template>
  <div class="control-bar">
    <MenuButton :close="appState.menu.isOpen" :back="false" @action="appState.menu.reOpenPage()" :title="$t('menu.tooltip')" />
    <div class="space thin-hide"></div>
    <!-- 地图按钮 -->
    <SvgButton v-if="appState.maps.length > 1" class="thin-hide" :title="$t('maps.tooltip')"
               @action="openMenuWithSound('maps', $t('maps.title'))">
      <svg viewBox="0 0 30 30">
        <polygon points="26.708,22.841 19.049,25.186 11.311,20.718 3.292,22.841 7.725,5.96 13.475,4.814 19.314,7.409 25.018,6.037 "/>
      </svg>
    </SvgButton>
    <!-- 标记按钮 -->
    <SvgButton v-if="showMapMenu && showMarkerMenu" class="thin-hide" :title="$t('markers.tooltip')"
               @action="openMenuWithSound('markers', $t('markers.title'), {markerSet: markers})">
      <svg viewBox="0 0 30 30">
        <path d="M15,3.563c-4.459,0-8.073,3.615-8.073,8.073c0,6.483,8.196,14.802,8.196,14.802s7.951-8.013,7.951-14.802
			C23.073,7.177,19.459,3.563,15,3.563z M15,15.734c-2.263,0-4.098-1.835-4.098-4.099c0-2.263,1.835-4.098,4.098-4.098
			c2.263,0,4.098,1.835,4.098,4.098C19.098,13.899,17.263,15.734,15,15.734z"/>
      </svg>
    </SvgButton>
    <!-- 玩家列表按钮 -->
    <SvgButton v-if="showMapMenu && !playerMarkerSet.fake" class="thin-hide" :title="$t('players.tooltip')" @action="openPlayerList">
      <svg viewBox="0 0 30 30" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <mask id="centerMask">
            <rect x="0" y="0" width="30" height="30" fill="white"></rect>
            <g fill="black" stroke="black" stroke-width="1">
              <rect x="12" y="10" width="5" height="5"></rect>
              <rect x="12" y="16" width="5" height="6"></rect>
              <rect x="10" y="16" width="2" height="6"></rect>
              <rect x="17" y="16" width="2" height="6"></rect>
              <rect x="12" y="10" width="5" height="1"></rect>
              <rect x="12" y="11" width="1" height="1"></rect>
              <rect x="16" y="11" width="1" height="1"></rect>
            </g>
          </mask>
        </defs>
        <g transform="translate(-5.3, -6.7) scale(1.4)">
          <g mask="url(#centerMask)">
            <rect x="6" y="9" width="5" height="5"></rect>
            <rect x="6" y="15" width="5" height="6"></rect>
            <rect x="4" y="15" width="2" height="6"></rect>
            <rect x="11" y="15" width="2" height="6"></rect>
            <rect x="6" y="9" width="5" height="1" fill="#c7cbd1"></rect>
            <rect x="6" y="10" width="1" height="1" fill="#c7cbd1"></rect>
            <rect x="10" y="10" width="1" height="1" fill="#c7cbd1"></rect>
          </g>
          <g mask="url(#centerMask)">
            <rect x="18" y="9" width="5" height="5"></rect>
            <rect x="18" y="15" width="5" height="6"></rect>
            <rect x="16" y="15" width="2" height="6"></rect>
            <rect x="23" y="15" width="2" height="6"></rect>
            <rect x="18" y="9" width="5" height="1" fill="#c7cbd1"></rect>
            <rect x="18" y="10" width="1" height="1" fill="#c7cbd1"></rect>
            <rect x="22" y="10" width="1" height="1" fill="#c7cbd1"></rect>
          </g>
          <g>
            <rect x="12" y="10" width="5" height="5"></rect>
            <rect x="12" y="16" width="5" height="6"></rect>
            <rect x="10" y="16" width="2" height="6"></rect>
            <rect x="17" y="16" width="2" height="6"></rect>
            <rect x="12" y="10" width="5" height="1" fill="#c7cbd1"></rect>
            <rect x="12" y="11" width="1" height="1" fill="#c7cbd1"></rect>
            <rect x="16" y="11" width="1" height="1" fill="#c7cbd1"></rect>
          </g>
        </g>
      </svg>
    </SvgButton>
    <div class="space thin-hide greedy"></div>
    <DayNightSwitch v-if="showMapMenu" class="thin-hide" :title="$t('lighting.dayNightSwitch.tooltip')" />
    <div class="space thin-hide"></div>
    <ControlsSwitch v-if="showMapMenu && showViewControls" class="thin-hide"></ControlsSwitch>
    <div class="space thin-hide" v-if ="showViewControls"></div>
    <SvgButton v-if="showMapMenu" class="thin-hide" :title="$t('resetCamera.tooltip')" @action="$bluemap.resetCamera()">
      <svg viewBox="0 0 30 30">
        <rect x="7.085" y="4.341" transform="matrix(0.9774 0.2116 -0.2116 0.9774 3.2046 -1.394)" width="2.063" height="19.875"/>
        <path d="M12.528,5.088c0,0,3.416-0.382,4.479-0.031c1.005,0.332,2.375,2.219,3.382,2.545c1.096,0.354,4.607-0.089,4.607-0.089
      l-2.738,8.488c0,0-3.285,0.641-4.344,0.381c-1.049-0.257-2.607-2.015-3.642-2.324c-0.881-0.264-3.678-0.052-3.678-0.052
      L12.528,5.088z"/>
      </svg>
    </SvgButton>
    <PositionInput v-if="showMapMenu" class="pos-input" />
    <Compass v-if="showMapMenu" :title="$t('compass.tooltip')" />
  </div>
</template>

<script>
  import PositionInput from "./PositionInput.vue";
  import Compass from "./Compass.vue";
  import DayNightSwitch from "./DayNightSwitch.vue";
  import ControlsSwitch from "./ControlsSwitch.vue";
  import MenuButton from "./MenuButton.vue";
  import SvgButton from "./SvgButton.vue";
  import { playBoxOpen } from '../../js/map/BoxSounds'; // 导入开箱音效

  export default {
    name: "ControlBar",
    components: {
      SvgButton,
      MenuButton,
      ControlsSwitch,
      DayNightSwitch,
      PositionInput,
      Compass
    },
    data() {
      return {
        appState: this.$bluemap.appState,
        markers: this.$bluemap.mapViewer.markers.data,
        mapViewer: this.$bluemap.mapViewer.data
      }
    },
    computed: {
      playerMarkerSet() {
        for (let set of this.markers.markerSets) {
          if (set.id === "bm-players") return set;
        }
        return {
          id: "bm-players",
          label: "Players",
          markerSets: [],
          markers: [],
          fake: true,
        }
      },
      showMapMenu() {
        return this.mapViewer.mapState === "loading" || this.mapViewer.mapState === "loaded";
      },
      showViewControls() {
        if (!this.mapViewer.map) return false;
        return this.mapViewer.map.views.length > 1;
      },
      showMarkerMenu() {
        return this.hasMarkers(this.markers)
      }
    },
    methods: {
      // 地图/标记按钮点击时播放音效并打开菜单
      openMenuWithSound(page, title, params) {
        playBoxOpen();
        this.appState.menu.openPage(page, title, params);
      },
      // 玩家列表按钮点击时播放音效并打开玩家列表
      openPlayerList() {
        playBoxOpen(); // 添加开箱音效
        let playerList = this.playerMarkerSet;
        this.appState.menu.openPage('markers', this.$t("players.title"), {markerSet: playerList});
      },
      hasMarkers(markerSet) {
        if (markerSet.markers.length > 0) return true;
        for (let set of markerSet.markerSets) {
          if (set.id !== "bm-players" && set.id !== "bm-popup-set") {
            if (this.hasMarkers(set)) return true;
          }
        }
        return false;
      }
    }
  }
</script>

<style lang="scss">
@import "/src/scss/variables.scss";

  .control-bar {
    position: fixed;
    top: 0;
    left: 0;

    display: flex;

    filter: drop-shadow(1px 1px 3px rgba(0, 0, 0, 0.53));
    height: 2em;

    margin: 0.5em;
    width: calc(100% - 1em);

    .pos-input {
      max-width: 20em;
      width: 100%;
    }

    > :not(:first-child) {
      border-left: solid 1px var(--theme-bg-light);
    }

    .space {
      width: 0.5em;
      flex-shrink: 0;

      &.greedy {
        flex-grow: 1;
      }
    }

    .space, .space + * {
      border-left: none;
    }

    @media (max-width: $mobile-break) {
      margin: 0;
      width: 100%;

      background-color: var(--theme-bg);

      .pos-input {
        max-width: unset;
      }

      .thin-hide {
        display: none;
      }

      .space {
        width: 1px;
      }
    }

  }
</style>