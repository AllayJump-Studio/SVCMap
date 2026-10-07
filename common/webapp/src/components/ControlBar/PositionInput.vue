<template>
  <div class="position-input">
    <NumberInput label="X" :value="controls.position.x" v-on:input="controls.position.x = parseFloat($event.target.value);" />
    <NumberInput label="Y" :value="controls.position.y" v-on:input="controls.position.y = parseFloat($event.target.value);" v-if="appState.controls.state === 'free'" />
    <NumberInput label="Z" :value="controls.position.z" v-on:input="controls.position.z = parseFloat($event.target.value);" />
  </div>
</template>

<script>
  import NumberInput from "./NumberInput.vue";
  import {reactive} from "vue";

  export default {
    name: "PositionInput",
    components: {
      NumberInput
    },
    data() {
      return {
        controls: this.$bluemap.mapViewer.controlsManager.data,
        appState: this.$bluemap.appState
      }
    }
  }
</script>

<style lang="scss">
  .position-input {
    display: flex;
    user-select: none;
    font-family: 'Minecraft', sans-serif;
    letter-spacing: 0.1em;

    > * {
      width: 100%;

      &:not(:first-child) {
        border-left: solid 1px var(--theme-bg-light);
      }
    }
  }
</style>