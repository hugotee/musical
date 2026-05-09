<template>
  <div class="music-detail-body">
    <BackBtn></BackBtn>
    <div class="music-panel">
      <div class="music-cover">
        <div :class="['cover-bg', musicPlayStore.playing ? 'spinning' : '']"></div>
        <div class="cover-ring">
          <Cover :width="180" :cover="musicInfo.cover" borderRadius="50%"></Cover>
        </div>
      </div>
      <div class="music-info">
        <div class="music-title">{{ musicInfo.musicTitle }}</div>
        <div class="user-info">by {{ musicInfo.nickName || '--' }}</div>
        <div class="action-panel">
          <div :class="['play-btn', musicPlayStore.playing ? 'icon-pause' : 'icon-play', 'iconfont']" @click="playMusic"></div>
          <div class="action-btn"><ActionGood :data="musicInfo"></ActionGood></div>
          <div class="action-btn"><ActionShare :data="musicInfo"></ActionShare></div>
          <el-button type="primary" size="large" round @click="createSame">做同款</el-button>
        </div>
        <div class="lyrics-panel" v-if="musicInfo.musicType === 0">
          <div class="lyrics-title">歌词</div>
          <div v-for="(item, i) in musicInfo.lyrics" :key="i"
            :class="['lyrics-item', musicPlayStore.currentPlayTime >= item.start && musicPlayStore.currentPlayTime <= item.end ? 'active' : '']">
            {{ item.text }}
          </div>
        </div>
        <div v-else class="lyrics-panel"><div class="pure-hint">纯音乐，请欣赏</div></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import ActionShare from "@/component/biz/ActionShare.vue";
import ActionGood from "@/component/biz/ActionGood.vue";
import { ref, getCurrentInstance, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useMusicPlayStore } from "@/stores/musicPlay.js";
const musicPlayStore = useMusicPlayStore();
import { mitter } from "@/eventbus/eventBus.js";

const currentMusicId = ref(route.params.musicId);
const musicInfo = ref({});
const getMusicInfo = async (autoPlay) => {
  let result = await proxy.Request({ url: proxy.Api.musicDetail, params: { musicId: currentMusicId.value } });
  if (!result) return;
  if (result.data.musicType === 0) {
    try { result.data.lyrics = JSON.parse(result.data.lyrics); } catch (e) { result.data.lyrics = []; }
  }
  musicInfo.value = result.data;
  if (!autoPlay) return;
  musicPlayStore.play({ ...result.data });
};

const playMusic = () => {
  if (musicPlayStore.currentMusic?.musicId == musicInfo.value.musicId) { mitter.emit("togglePlay"); return; }
  musicPlayStore.play({ ...musicInfo.value });
};
const createSame = () => { router.push(`/idea/${musicInfo.value.creationId}`); };

watch(() => route.params.musicId, async (newVal) => {
  if (!newVal) return; currentMusicId.value = newVal; getMusicInfo(true);
}, { immediate: true, deep: true });
watch(() => musicPlayStore.currentMusic.musicId, async (newVal) => {
  if (!newVal) return; router.push(`/play/${newVal}`);
}, { immediate: true, deep: true });
</script>

<style lang="scss" scoped>
.music-detail-body {
  padding: 28px; max-width: 900px;
  .music-panel {
    display: flex; gap: 40px;
    margin-top: 20px; padding: 40px;
    background: var(--panelBg); border: 1px solid var(--line);
    border-radius: var(--radius); box-shadow: var(--softShadow);
    .music-cover {
      width: 220px; height: 220px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center; position: relative;
      .cover-bg {
        position: absolute; inset: 0;
        border-radius: 50%;
        background: conic-gradient(from 0deg, var(--accent), #4aa8d8, #f0c36a, var(--accent));
        opacity: 0.15; filter: blur(30px);
      }
      .cover-bg.spinning { animation: spin 20s linear infinite; }
      .cover-ring { position: relative; z-index: 2; }
    }
    .music-info {
      flex: 1; min-width: 0;
      .music-title { font-size: 28px; font-weight: 800; letter-spacing: -0.01em; color: var(--HiText); }
      .user-info { margin-top: 8px; color: var(--text); font-size: 14px; }
      .action-panel { margin-top: 20px; display: flex; align-items: center; gap: 16px;
        .play-btn {
          width: 44px; height: 44px; border-radius: 50%;
          background: var(--btnBg); box-shadow: var(--btnShadow);
          display: flex; align-items: center; justify-content: center;
          font-size: 18px; cursor: pointer; color: #061014;
          transition: transform var(--transition);
          &:hover { transform: scale(1.08); }
        }
        .action-btn { cursor: pointer; }
      }
      .lyrics-panel { margin-top: 28px;
        .lyrics-title { font-size: 16px; font-weight: 700; margin-bottom: 12px; color: var(--HiText); }
        .lyrics-item { padding: 6px 0; font-size: 15px; color: var(--text); transition: all var(--transition); }
        .lyrics-item.active { color: var(--accentWarm); font-size: 16px; font-weight: 600; }
        .pure-hint { color: var(--mutedText); font-size: 15px; font-style: italic; }
      }
    }
  }
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 500px) {
  .music-detail-body { padding: 16px; .music-panel { flex-direction: column; align-items: center; text-align: center; gap: 24px; padding: 24px;
    .music-cover { width: 180px; height: 180px; }
    .music-info .action-panel { justify-content: center; }
  }}
}
</style>
