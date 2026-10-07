<template>
<div class="slider">
  <div class="label"><slot />: <span class="value">{{formatter(value)}}</span></div>
  <label>
    <input type="range" :min="min" :max="max" :step="step" :value="value" @input="$emit('update', parseFloat($event.target.value))" @change="$emit('lazy', parseFloat($event.target.value))">
  </label>
</div>
</template>

<script>

function countDecimals(value) {
  if(Math.floor(value) === value) return 0;
  return value.toString().split(".")[1].length || 0;
}

export default {
  name: "Slider",
  props: {
    value: Number,
    min: Number,
    max: Number,
    step: Number,
    formatter: {
      type: Function,
      default: function(value) {
        return parseFloat(value).toFixed(countDecimals(this.step));
      }
    }
  }
}
</script>

<style lang="scss">
.side-menu .slider {
  line-height: 2em;
  padding: 0 0.5em;

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  > .label {
    > .value {
      float: right;
    }
  }

  > label {
    > input {
      appearance: none;
      -moz-appearance: none;
      -webkit-appearance: none;
      outline: none;

      width: 100%;
      height: 1.5em;                     /* 轨道高度 */

      border-radius: 0;                /* 长方形，无圆角 */
      overflow: hidden;
      background-color: #2c2c2c; /* 轨道背景色 */

      &::-webkit-slider-thumb {
        appearance: none;
        -moz-appearance: none;
        -webkit-appearance: none;
        outline: none;

        width: 0.7em;                  /* 宽度较细 */
        height: 1.5em;                   /* 与轨道同高 */
        border-radius: 0;              /* 矩形滑块 */
        border: solid 0.125em #2c2c2c; /* 边框色与填充色一致 */
        background-color: #6f6f6f;     /* 填充色 */
      }

      &::-moz-range-thumb {
        width: 0.4em;
        height: 1em;
        border-radius: 0;
        border: solid 0.125em var(--theme-bg-light);
        background-color: var(--theme-bg-light);
      }
    }
  }
}
</style>