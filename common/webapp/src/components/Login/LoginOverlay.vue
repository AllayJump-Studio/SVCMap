<template>
  <div id="bm-login">
    <div class="login-card">
      <h2>BlueMap</h2>
      <div class="subtitle">{{ $t("login.subtitle") }}</div>

      <p v-if="unavailable" class="message error">{{ $t("login.unavailable") }}</p>
      <p v-else-if="!hasAccounts" class="message hint">{{ $t("login.noAccountsHint") }}</p>

      <div class="tabs">
        <button type="button" :class="{active: mode === 'login'}" @click="switchMode('login')">
          {{ $t("login.loginTab") }}
        </button>
        <button type="button" :class="{active: mode === 'register'}" @click="switchMode('register')">
          {{ $t("login.registerTab") }}
        </button>
      </div>

      <form v-if="mode === 'login'" @submit.prevent="submitLogin">
        <label>
          <span>{{ $t("login.username") }}</span>
          <input v-model.trim="username" autocomplete="username" maxlength="16" required :disabled="busy" />
        </label>
        <label>
          <span>{{ $t("login.password") }}</span>
          <input v-model="password" type="password" autocomplete="current-password" required :disabled="busy" />
        </label>
        <label class="checkbox">
          <input v-model="rememberMe" type="checkbox" :disabled="busy" />
          <span>{{ $t("login.rememberMe") }}</span>
        </label>
        <button type="submit" class="submit" :disabled="busy">
          {{ busy ? $t("login.working") : $t("login.loginButton") }}
        </button>
      </form>

      <form v-else @submit.prevent="submitRegister">
        <label>
          <span>{{ $t("login.username") }}</span>
          <input v-model.trim="username" autocomplete="username" maxlength="16" required :disabled="busy" />
        </label>
        <label>
          <span>{{ $t("login.passcode") }}</span>
          <input v-model.trim="passcode" maxlength="16" required :disabled="busy" :placeholder="$t('login.passcodePlaceholder')" />
        </label>
        <label>
          <span>{{ $t("login.password") }}</span>
          <input v-model="password" type="password" autocomplete="new-password" required :disabled="busy" />
        </label>
        <label>
          <span>{{ $t("login.passwordRepeat") }}</span>
          <input v-model="passwordRepeat" type="password" autocomplete="new-password" required :disabled="busy" />
        </label>
        <p class="message hint">{{ $t("login.registerHint") }}</p>
        <button type="submit" class="submit" :disabled="busy">
          {{ busy ? $t("login.working") : $t("login.registerButton") }}
        </button>
      </form>

      <p v-if="error" class="message error">{{ error }}</p>

    </div>
  </div>
</template>

<script>
const ERROR_KEYS = {
  "bad-credentials": "login.errors.badCredentials",
  "too-many-attempts": "login.errors.tooManyAttempts",
  "no-pending-registration": "login.errors.noPending",
  "passcode-expired": "login.errors.passcodeExpired",
  "invalid-passcode": "login.errors.invalidPasscode",
  "account-exists": "login.errors.accountExists",
  "password-too-short": "login.errors.passwordTooShort",
  "bad-format": "login.errors.badFormat",
  "storage-error": "login.errors.storage",
  "not-found": "login.unavailable"
};

export default {
  name: "LoginOverlay",
  props: {
    session: { type: Object, default: null },
    onSuccess: { type: Function, required: true }
  },
  data() {
    return {
      mode: "login",
      username: "",
      passcode: "",
      password: "",
      passwordRepeat: "",
      rememberMe: false,
      busy: false,
      error: ""
    };
  },
  computed: {
    hasAccounts() {
      return this.session ? this.session.hasAccounts !== false : true;
    },
    unavailable() {
      // the auth endpoints are not reachable at all (e.g. the account file failed to load on the server)
      return this.session === null;
    },
    minPasswordLength() {
      return (this.session && this.session.minPasswordLength) || 8;
    }
  },
  methods: {
    switchMode(mode) {
      this.mode = mode;
      this.error = "";
    },
    async submitLogin() {
      if (this.busy) return;
      this.error = "";

      this.busy = true;
      try {
        const response = await this.post("login", {
          username: this.username,
          password: this.password,
          rememberMe: this.rememberMe
        });
        if (response.ok) {
          this.onSuccess();
          return;
        }
        this.error = this.errorText(response.body);
      } catch (e) {
        this.error = this.errorText(null);
      } finally {
        this.busy = false;
      }
    },
    async submitRegister() {
      if (this.busy) return;
      this.error = "";

      if (this.password.length < this.minPasswordLength) {
        this.error = this.errorText({ error: "password-too-short" });
        return;
      }
      if (this.password !== this.passwordRepeat) {
        this.error = this.$t("login.errors.passwordMismatch");
        return;
      }

      this.busy = true;
      try {
        const response = await this.post("register", {
          username: this.username,
          passcode: this.passcode,
          password: this.password
        });
        if (response.ok) {
          this.onSuccess();
          return;
        }
        this.error = this.errorText(response.body);
      } catch (e) {
        this.error = this.errorText(null);
      } finally {
        this.busy = false;
      }
    },
    async post(endpoint, payload) {
      const response = await fetch("api/auth/" + endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json; charset=utf-8" },
        body: JSON.stringify(payload)
      });

      let body = null;
      try {
        body = await response.json();
      } catch (e) {
        body = null;
      }

      return { ok: response.ok, body };
    },
    errorText(body) {
      if (!body || !body.error) return this.$t("login.errors.unknown");
      const key = ERROR_KEYS[body.error];
      return key ? this.$t(key) : this.$t("login.errors.unknown");
    }
  }
};
</script>

<style lang="scss">
#bm-login {
  position: fixed;
  inset: 0;
  z-index: 20000;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 1em;
  overflow-y: auto;

  background-color: rgba(12, 10, 8, 0.72);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);

  font-family: "Unifont", sans-serif;
  color: var(--theme-fg);

  .login-card {
    box-sizing: border-box;
    width: min(26em, 100%);
    padding: 1.5em;

    background-color: lch(46.84% 0.01 296.81 / 0.702);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    box-shadow: 1px 1px 6px rgba(0, 0, 0, 0.6);

    h2 {
      margin: 0 0 0.25em 0;
      font-size: 1.6em;
      text-align: center;
      font-weight: normal;
    }

    .subtitle {
      margin-bottom: 1em;
      text-align: center;
      font-size: 0.85em;
      color: var(--theme-fg-light);
    }

    .tabs {
      display: flex;
      gap: 0.25em;
      margin-bottom: 1em;

      button {
        flex: 1;
        padding: 0.4em;

        font: inherit;
        font-size: 0.9em;
        cursor: pointer;

        color: var(--theme-fg);
        background-color: var(--theme-bg-hover);
        border: none;

        &.active {
          background-color: var(--theme-bg-light);
          color: #222;
          font-weight: bold;
        }
      }
    }

    form {
      display: flex;
      flex-direction: column;
      gap: 0.6em;

      label {
        display: flex;
        flex-direction: column;
        gap: 0.2em;
        font-size: 0.85em;

        > span {
          color: var(--theme-fg-light);
        }

        input {
          box-sizing: border-box;
          width: 100%;
          padding: 0.4em 0.5em;

          font: inherit;
          font-size: 1rem;
          color: var(--theme-fg);
          background-color: rgba(0, 0, 0, 0.35);
          border: solid 1px rgba(255, 255, 255, 0.25);

          &:focus {
            outline: none;
            border-color: var(--theme-bg-light);
          }
        }

        &.checkbox {
          flex-direction: row;
          align-items: center;
          gap: 0.4em;

          input {
            width: auto;
          }
        }
      }

      .submit {
        margin-top: 0.4em;
        padding: 0.5em;

        font: inherit;
        font-size: 1rem;
        cursor: pointer;

        color: #222;
        background-color: var(--theme-bg-light);
        border: none;

        &:disabled {
          opacity: 0.6;
          cursor: default;
        }
      }
    }

    .message {
      margin: 0.6em 0 0 0;
      font-size: 0.8em;
      line-height: 1.3em;
      text-align: center;

      &.hint {
        color: var(--theme-fg-light);
      }

      &.warning {
        color: #ffcf70;
      }

      &.error {
        color: #ff8a8a;
      }
    }
  }
}
</style>
