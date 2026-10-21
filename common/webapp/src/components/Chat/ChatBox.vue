<template>
  <div id="chat" :class="{open: isOpen, 'free-flight': isFreeFlight, moving: moving}">

    <!-- 消息列表：出现/收起时用「从上至下裁切」的动画 -->
    <Transition
      name="chat-list"
      :duration="LIST_ANIM_DURATION"
      @enter="onListEnter"
      @after-enter="onListAfterEnter"
      @leave="onListLeave"
      @after-leave="onListAfterLeave"
    >
      <div v-if="renderedMessages.length > 0" ref="messageList" class="chat-messages has-messages">
        <div
          v-for="item of renderedMessages"
          :key="item.key"
          class="chat-message"
          :class="{fading: item.fading, system: item.message.source === 'system'}"
        >
          <span v-if="item.message.playerName" class="chat-message-name">{{ item.message.playerName }}</span>
          <span class="chat-message-text">{{ item.message.message }}</span>
        </div>
      </div>
    </Transition>

    <!-- 输入框：只有「聊天」状态才存在 -->
    <form v-if="isOpen" class="chat-input-row" @submit.prevent="send">
      <input
        ref="input"
        v-model="draft"
        class="chat-input"
        type="text"
        autocomplete="off"
        :placeholder="$t('chat.inputPlaceholder')"
        :maxlength="MAX_MESSAGE_LENGTH"
        @keydown="onInputKeydown"
        @keyup="onInputKeyup"
      />
    </form>

    <!-- 悬浮按钮：与缩放按钮共用 SvgButton，位置规则与 #zoom-buttons 对称（左下角） -->
    <SvgButton class="chat-button" :active="isOpen" :title="$t('chat.button')" @action="onButtonClick">
      <svg viewBox="0 0 30 30">
        <!-- 直角风格的对话气泡 + 三个点（挖空，靠 evenodd） -->
        <path
          fill-rule="evenodd"
          d="M4 7H26V20H12V24H8V20H4ZM9 11H12V14H9ZM14 11H17V14H14ZM19 11H22V14H19Z"
        />
      </svg>
    </SvgButton>

  </div>
</template>

<script>
import SvgButton from "../ControlBar/SvgButton.vue";

/**
 * 「仅旁观」状态只显示最近这么多毫秒的消息，超时后先渐隐、再移除。
 * 这两个值需要与下面 .chat-message 的 transition 时长保持一致。
 */
const SPECTATOR_WINDOW = 15000;
const FADE_DURATION = 500;

/** 轮询间隔：用于推进「超时 → 渐隐 → 移除」的时间轴 */
const TICK_INTERVAL = 250;

/** 与服务端 plugin.conf 的 chat.message-length-limit 对应 */
const MAX_MESSAGE_LENGTH = 256;

/**
 * 自由飞行模式下，聊天栏从「左下角」平移到「左上角操作栏下方」的动画时长。
 * 只在模式切换期间开启 transform 过渡（见 .moving），动画结束后立即关闭，
 * 这样消息增减引起的高度变化不会被动画化。
 */
const MOVE_DURATION = 300;

/**
 * 消息列表出现/收起时「从上至下裁切」动画的时长。
 * 收起时裁切线从顶部扫到底部（顶部先消失），展开时反向。
 */
const LIST_ANIM_DURATION = 260;

export default {
  name: "ChatBox",
  components: {SvgButton},
  data() {
    return {
      appState: this.$bluemap.appState,
      draft: "",
      // 驱动时间轴：只有它变化，computed 才会重新计算消息是否超时
      now: Date.now(),
      // 是否正在做「左下角 ⇄ 左上角」的平滑位移
      moving: false,
      MAX_MESSAGE_LENGTH,
      LIST_ANIM_DURATION
    }
  },
  computed: {
    isOpen() {
      return this.appState.chat.state === "open";
    },
    isFreeFlight() {
      return this.appState.controls.state === "free";
    },
    renderedMessages() {
      const messages = this.appState.chat.messages;

      // 「聊天」状态：展示全部历史消息，不过期
      if (this.isOpen) {
        return messages.map(message => ({
          key: this.messageKey(message),
          message,
          fading: false
        }));
      }

      // 「仅旁观」状态：只显示最近 15 秒
      // 消息用的是服务端时间戳，而 this.now 是浏览器时钟，所以要先换算到服务端时间轴
      const serverNow = this.now + this.appState.chat.clockOffset;
      const expireAt = serverNow - SPECTATOR_WINDOW;
      const removeAt = expireAt - FADE_DURATION;

      const result = [];
      for (const message of messages) {
        if (message.timestamp <= removeAt) continue; // 渐隐结束，移除
        result.push({
          key: this.messageKey(message),
          message,
          fading: message.timestamp <= expireAt // 已超时，正在渐隐
        });
      }
      return result;
    }
  },
  watch: {
    isOpen(open) {
      // 刷新时间轴：否则从「聊天」切回「仅旁观」时会用陈旧的时间判断过期，
      // 导致全部历史消息先闪现一下再一起渐隐
      this.now = Date.now();

      if (open) {
        this.$nextTick(() => {
          this.focusInput();
          this.scrollToBottom(true);
        });
      }
      this.updateTicker();
    },
    "appState.chat.messages.length"() {
      this.now = Date.now(); // 同理，避免用陈旧时间判断新消息/旧消息是否过期
      // 列表是 v-if 的，第一条消息时元素还没渲染出来，要等下一帧再滚到底部
      this.$nextTick(() => this.scrollToBottom());
      this.updateTicker();
    },
    "appState.chat.clockOffset"() {
      // 时钟偏差被校正后，需要立刻重算消息的过期状态
      this.now = Date.now();
    },
    isFreeFlight() {
      // 只在模式切换时开启位移过渡：平时关掉，避免消息增减导致高度变化时也被动画化
      this.moving = true;
      window.clearTimeout(this.movingTimeout);
      this.movingTimeout = window.setTimeout(() => {
        this.moving = false;
      }, MOVE_DURATION + 50);
    }
  },
  created() {
    /** @type {?number} */
    this.ticker = null;
    /** @type {?number} */
    this.movingTimeout = null;
  },
  mounted() {
    window.addEventListener("keydown", this.onGlobalKeydown);
    this.updateTicker();
  },
  beforeUnmount() {
    window.removeEventListener("keydown", this.onGlobalKeydown);
    this.stopTicker();
    window.clearTimeout(this.movingTimeout);
    // UI 都不在了，就不该保留「聊天」状态（例如刚在设置里关掉聊天栏）
    this.$bluemap.closeChat();
  },
  methods: {
    messageKey(message) {
      // (timestamp, playerUuid, message) 在 BlueMapApp.addChatMessage() 中保证唯一
      return `${message.timestamp}|${message.playerUuid}|${message.message}`;
    },

    // ------------------------------------------------------------ 时间轴

    updateTicker() {
      // 只有「仅旁观」状态且确实有消息时才需要推进时间轴
      const needed = !this.isOpen && this.appState.chat.messages.length > 0;
      if (needed && this.ticker === null) {
        this.ticker = window.setInterval(() => {
          this.now = Date.now();
        }, TICK_INTERVAL);
      } else if (!needed) {
        this.stopTicker();
      }
    },
    stopTicker() {
      if (this.ticker !== null) {
        window.clearInterval(this.ticker);
        this.ticker = null;
      }
    },

    // ------------------------------------------------------------ 键盘

    /**
     * 全局 T 键。
     * 与悬浮按钮完全一致：调用同一个 onButtonClick()，即纯粹的「切换聊天状态」。
     * 焦点已经在输入框里时，输入框自己的 keydown 会 stopPropagation，所以不会走到这里。
     * @param evt {KeyboardEvent}
     */
    onGlobalKeydown(evt) {
      if (evt.ctrlKey || evt.metaKey || evt.altKey) return;
      if (evt.code !== "KeyT") return;
      if (this.isTypingTarget(evt.target)) return;

      evt.preventDefault();
      this.onButtonClick();
    },

    /**
     * 输入框内的按键。
     * 必须 stopPropagation：地图/自由飞行的键盘控制监听在 window 上（WASD、空格、
     * Shift、方向键…），不拦截的话在聊天框里打字会同时移动镜头，而且它们的
     * preventDefault() 会让字符根本打不进去。
     * @param evt {KeyboardEvent}
     */
    onInputKeydown(evt) {
      evt.stopPropagation();

      if (evt.key === "Escape") {
        evt.preventDefault();
        this.$bluemap.closeChat();
        this.blurInput();
      }
      // Enter 由 form 的 submit 处理
    },
    onInputKeyup(evt) {
      evt.stopPropagation();
    },

    isTypingTarget(target) {
      if (!target) return false;
      const tag = target.tagName;
      return tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || target.isContentEditable === true;
    },

    // ------------------------------------------------------------ 交互

    onButtonClick() {
      this.$bluemap.toggleChat();
    },

    async send() {
      const text = this.draft.trim();
      if (!text) return;

      this.draft = "";
      const success = await this.$bluemap.sendChat(text);
      if (!success) this.draft = text; // 发送失败：内容留着，下次打开可重发

      // 发送后是否自动关闭输入框由设置决定（默认开启，与原版 MC 一致）
      if (this.appState.controls.chatCloseAfterSend) {
        this.$bluemap.closeChat();
        this.blurInput();
      } else {
        this.$nextTick(() => this.focusInput());
      }
    },

    // ------------------------------------------------------------ 消息列表动画

    /**
     * 展开：从「高度 0 + 顶部裁切掉全部」过渡到完整高度。
     * clip-path 的 inset(top ...) 以元素自身盒子为参照，top 从 100% 变到 0，
     * 视觉上就是裁切线由底部向上退开，内容自下而上出现。
     */
    onListEnter(el) {
      const height = el.getBoundingClientRect().height;

      this.prepareListTransition(el);
      el.style.height = "0px";
      el.style.paddingTop = "0px";
      el.style.paddingBottom = "0px";
      el.style.clipPath = "inset(100% 0 0 0)";

      // 下一帧再改目标值，浏览器才会把起始状态先画出来
      requestAnimationFrame(() => {
        el.style.transition = this.listTransition();
        el.style.height = `${height}px`;
        el.style.paddingTop = "";
        el.style.paddingBottom = "";
        el.style.clipPath = "inset(0 0 0 0)";
      });
    },

    onListAfterEnter(el) {
      this.resetListTransition(el);
    },

    /**
     * 收起：裁切线由顶部扫到底部，顶部先消失。
     */
    onListLeave(el) {
      const height = el.getBoundingClientRect().height;

      this.prepareListTransition(el);
      el.style.height = `${height}px`;
      el.style.clipPath = "inset(0 0 0 0)";

      requestAnimationFrame(() => {
        el.style.transition = this.listTransition();
        el.style.height = "0px";
        el.style.paddingTop = "0px";
        el.style.paddingBottom = "0px";
        el.style.clipPath = "inset(100% 0 0 0)";
      });
    },

    onListAfterLeave(el) {
      this.resetListTransition(el);
    },

    listTransition() {
      return `height ${LIST_ANIM_DURATION}ms ease, padding ${LIST_ANIM_DURATION}ms ease, ` +
             `clip-path ${LIST_ANIM_DURATION}ms ease`;
    },

    prepareListTransition(el) {
      el.style.transition = "none";
      // 动画期间隐藏滚动条，否则高度变化时它会闪烁
      el.style.overflowY = "hidden";
    },

    resetListTransition(el) {
      el.style.transition = "";
      el.style.height = "";
      el.style.paddingTop = "";
      el.style.paddingBottom = "";
      el.style.clipPath = "";
      el.style.overflowY = "";
    },

    focusInput() {
      if (this.$refs.input) this.$refs.input.focus();
    },
    blurInput() {
      if (this.$refs.input) this.$refs.input.blur();
    },

    /**
     * 滚动到底部。默认只在用户本来就贴近底部时才自动滚动，
     * 避免用户翻看历史消息时被新消息打断。
     * @param force {boolean}
     */
    scrollToBottom(force = false) {
      const el = this.$refs.messageList;
      if (!el) return;
      const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
      if (force || nearBottom) el.scrollTop = el.scrollHeight;
    }
  }
}
</script>

<style lang="scss">
  #chat {
    position: fixed;
    left: 0.5em; // 与 .control-bar 的 margin-left: 0.5em 对齐
    bottom: 0.5em;

    display: flex;
    flex-direction: column;
    align-items: flex-start;

    width: min(28em, calc(100% - 1em));

    // 默认贴在左下角。用 transform 而不是 top/bottom 来切换位置，
    // 因为只有 transform 可以在「左下角 ⇄ 左上角」之间平滑过渡。
    // translateY 的百分比参照元素自身高度，所以
    //   100% - 100vh + 3.5em  ⇒ 元素顶边落在视口顶部下方 3em 处
    // （.control-bar 占 0.5em 外边距 + 2em 高度，故取 3em）
    transform: translateY(0);

    pointer-events: none; // 不挡住地图拖拽，只让具体子元素接收事件

    // 过渡只在模式切换期间生效（由 JS 控制 .moving），
    // 否则消息增减导致的高度变化会让整个聊天栏跟着滑动
    // 时长需与 JS 里的 MOVE_DURATION 保持一致
    &.moving {
      transition: transform 300ms ease;
    }

    // 自由飞行：平移到左上角操作栏下方，让开左下角的自由飞行控制
    &.free-flight {
      transform: translateY(calc(100% - 100vh + 3.5em));
    }

    .chat-messages {
      display: flex;
      flex-direction: column;

      // 关键：有消息时这段会加内边距，不声明 border-box 的话会比输入框宽 1em
      box-sizing: border-box;
      width: 100%;
      max-height: 30vh;
      overflow-y: auto;
      overflow-x: hidden;

      pointer-events: none;
    }

    // 消息列表的毛玻璃底：与顶部 .control-bar 完全同一组值
    // （backdrop-filter 而不是 filter —— 后者会让元素成为 backdrop root，把自己这层糊掉）
    // 只在有消息时显示，否则按 T 打开时会先出现一条空的玻璃条
    .chat-messages.has-messages {
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      background-color: lch(46.84% 0.01 296.81 / 0.702);
      padding: 0.25em 0.5em;
    }

    // 「聊天」状态下列表可滚动（& 指向 #chat，不能写成 #chat.open &）
    &.open .chat-messages {
      pointer-events: auto;
    }

    .chat-message {
      // MC 风格：无底框，靠文字描边保证在地图上的可读性
      color: var(--theme-fg);
      text-shadow: 0.06em 0.06em 0 rgba(0, 0, 0, 0.85);
      line-height: 1.4em;
      word-break: break-word;

      // 时长需与 JS 里的 FADE_DURATION 保持一致
      opacity: 1;
      transition: opacity 500ms linear;

      &.fading {
        opacity: 0;
      }

      &.system .chat-message-text {
        color: var(--theme-fg-light);
      }

      .chat-message-name {
        &::after {
          content: ': ';
        }
      }
    }

    .chat-input-row {
      width: 100%;
      pointer-events: auto;
    }

    .chat-input {
      box-sizing: border-box;
      width: 100%;
      padding: 0.25em 0.5em;

      // 与 .control-bar / 缩放按钮一致的毛玻璃
      background-color: lch(46.84% 0.01 296.81 / 0.702);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);

      color: var(--theme-fg);
    }

    // 悬浮按钮：位置规则与 #zoom-buttons 对称（那边是 right: 0 + margin: 0.5em）
    // 毛玻璃背景与状态配色在 global.scss 里统一定义（与缩放按钮同一套规则）
    .chat-button {
      margin-top: 0.25em;

      // 与缩放按钮一致的投影。用 box-shadow 而不是 filter: drop-shadow()，
      // 因为 filter 会让元素成为 backdrop root，导致上面那层毛玻璃失效。
      box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.53);
    }
  }
</style>
