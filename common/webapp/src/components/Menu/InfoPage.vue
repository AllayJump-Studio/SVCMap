<template>
  <div class="info-page">
    <div class="logo-container" @click="handleLogoClick">
      <img :src="currentLogo" alt="Logo" class="logo" />
    </div>
    <div class="info-content" v-html="$t('info.content')"></div>
  </div>
</template>

<script>
import logo from '@/assets/logo.png';
import petLogo from '@/assets/petlogo.gif';
import { playRandomPat } from '../../js/map/Sounds'; 

export default {
  name: "InfoPage",
  data() {
    return {
      showPet: false,
      logoStatic: logo,
      logoGif: petLogo,
      targetUrl: "https://docs.qq.com/aio/DZU9kSm9XdU9HTkpj",
      clickCount: 0,
      timer: null,
    };
  },
  computed: {
    currentLogo() {
      return this.showPet ? this.logoGif : this.logoStatic;
    },
  },
  methods: {
    handleLogoClick() {
      playRandomPat();

      this.showPet = true;
      if (this.timer) clearTimeout(this.timer);
      this.timer = setTimeout(() => {
        this.showPet = false;
        this.timer = null;
      }, 200);

      this.clickCount++;
      if (this.clickCount >= 6) {
        window.open(this.targetUrl, "_blank");
        this.clickCount = 0;
      }
    },
  },
  beforeDestroy() {
    if (this.timer) clearTimeout(this.timer);
  },
};
</script>

<style scoped lang="scss">
.info-page {
  .logo-container {
    display: flex;
    justify-content: center;
    margin: 1em 0;
    cursor: pointer;
    .logo {
      width: 40%;
      max-width: 10em;
      border-radius: 10%;
      user-select: none;
    }
  }
  .info-content {
    font-size: 0.8em;
    table {
      border-collapse: collapse;
      width: 100%;
      th,
      td {
        padding: 0.2em 0.5em;
        border: solid 1px var(--theme-bg-light);
      }
      th {
        font-weight: inherit;
        text-align: inherit;
      }
    }
    .info-footer {
      text-align: center;
    }
  }
}
</style>