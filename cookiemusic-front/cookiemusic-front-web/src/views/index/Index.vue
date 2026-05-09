<template>
  <div class="index-body">
    <div class="hero">
      <div class="hero-sub">AI 驱动的音乐创作与发现</div>
      <div class="hero-title">发现好音乐，<span>创作你的旋律</span></div>
    </div>
    <div class="section">
      <div class="section-header">
        <div class="section-title">为你推荐</div>
        <div class="section-actions">
          <div :class="['arrow-btn', { disabled: disableType == 1 }]" @click="changeCommend(1)">
            <span class="iconfont icon-narrow-left"></span>
          </div>
          <div :class="['arrow-btn', { disabled: disableType == -1 }]" @click="changeCommend(-1)">
            <span class="iconfont icon-narrow-right"></span>
          </div>
        </div>
      </div>
      <CommendList ref="commendListRef" @disableType="hotChangeTypeHandler"></CommendList>
    </div>
    <div class="section">
      <div class="section-header">
        <div class="section-title">最新发布</div>
        <router-link to="latest" class="more-link">浏览全部 →</router-link>
      </div>
      <div class="latest-grid">
        <LatestList :indexType="1"></LatestList>
      </div>
    </div>
  </div>
</template>

<script setup>
import LatestList from "./LatestList.vue";
import CommendList from "./CommendList.vue";
import { ref, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const commendListRef = ref();
const disableType = ref(1);
const hotChangeTypeHandler = (type) => { disableType.value = type; };
const changeCommend = (type) => { commendListRef.value.change(type); };
</script>

<style lang="scss" scoped>
.index-body {
  padding: 32px 28px 0;
  max-width: 1400px;
  .hero {
    margin-bottom: 36px;
    .hero-sub { font-size: 13px; color: var(--mutedText); letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 8px; }
    .hero-title { font-size: 32px; font-weight: 800; letter-spacing: -0.02em;
      span { background: linear-gradient(120deg, var(--accent), #4aa8d8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
    }
  }
  .section { margin-bottom: 36px; }
  .section-header {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px;
    .section-title { font-size: 20px; font-weight: 700; color: var(--HiText); }
    .section-actions { display: flex; gap: 8px; }
    .arrow-btn {
      width: 38px; height: 38px; border-radius: 50%;
      background: rgba(255,255,255,0.04); border: 1px solid var(--line);
      display: flex; align-items: center; justify-content: center; cursor: pointer;
      transition: all var(--transition);
      &:hover { background: rgba(100,216,203,0.10); border-color: var(--lineStrong); transform: translateY(-1px); }
      &.disabled { opacity: 0.3; cursor: not-allowed; &:hover { transform: none; background: rgba(255,255,255,0.04); } }
    }
    .more-link {
      font-size: 13px; color: var(--text); text-decoration: none;
      padding: 6px 14px; border-radius: 20px; border: 1px solid var(--line);
      transition: all var(--transition);
      &:hover { color: var(--accent); border-color: var(--lineStrong); background: rgba(100,216,203,0.06); }
    }
  }
}
</style>
