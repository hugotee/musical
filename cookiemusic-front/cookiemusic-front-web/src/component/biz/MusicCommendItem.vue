<template>
  <div class="music-item">
    <div class="cover">
      <Cover :cover="data.cover" :width="220"></Cover>
      <div class="cover-overlay"></div>
      <div class="cover-actions">
        <div class="cover-btn" @click.stop><ActionGood :data="data"></ActionGood></div>
        <div class="cover-btn" @click.stop><ActionShare :data="data"></ActionShare></div>
      </div>
      <div class="cover-meta">
        <span>▶ {{ proxy.Utils.formatCount(data.playCount) }}</span>
        <span>♫ {{ proxy.Utils.seconds2Min(data.duration) }}</span>
      </div>
      <PlayBtn :data="data" @playList="playList"></PlayBtn>
    </div>
    <div class="music-info">
      <div class="music-title" @click="playMusic(true)">{{ data.musicTitle }}</div>
      <div class="music-prompt">{{ data.prompt }}</div>
      <router-link class="user-info" :to="`/user/${data.userId}`">
        <Avatar :width="24" :avatar="data.avatar"></Avatar>
        <span class="user-name">{{ data.nickName }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import ActionShare from "@/component/biz/ActionShare.vue";
import ActionGood from "@/component/biz/ActionGood.vue";
import { ref, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useMusicPlayStore } from "@/stores/musicPlay.js";
const musicPlayStore = useMusicPlayStore();

const props = defineProps({ data: { type: Object, default: {} } });
const emits = defineEmits(["playList"]);
const playMusic = (jumpDetail) => {
  emits("playList");
  musicPlayStore.play({ ...props.data });
  if (!jumpDetail) return;
  router.push(`/play/${props.data.musicId}`);
};
const playList = () => { emits("playList"); };
</script>

<style lang="scss" scoped>
.music-item {
  width: 260px; padding-right: 24px; position: relative; cursor: pointer; flex-shrink: 0;
  .cover {
    width: 100%; overflow: hidden; position: relative;
    border-radius: var(--radius);
    background: var(--cardBg);
    border: 1px solid var(--cardBorder);
    box-shadow: var(--cardShadow);
    transition: transform var(--transition), box-shadow var(--transition);
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 20px 48px rgba(0,0,0,0.40), 0 4px 12px rgba(0,0,0,0.20);
      .cover-overlay { opacity: 1; }
      .cover-actions { opacity: 1; transform: translateY(0); }
      .cover-meta { opacity: 0; }
      :deep(img) { transform: scale(1.06); }
    }
    :deep(img) { max-width: 100%; transition: transform 0.5s ease; }
    .cover-overlay {
      position: absolute; inset: 0;
      background: linear-gradient(180deg, transparent 40%, rgba(6,13,17,0.82) 100%);
      opacity: 0.6; transition: opacity var(--transition); pointer-events: none;
    }
    .cover-actions {
      position: absolute; bottom: 14px; left: 0; right: 0;
      display: flex; justify-content: center; gap: 10px;
      opacity: 0; transform: translateY(8px);
      transition: all var(--transition); z-index: 5;
      .cover-btn {
        width: 44px; height: 44px; border-radius: 50%;
        background: rgba(6,15,20,0.75); border: 1px solid var(--lineStrong);
        backdrop-filter: blur(12px);
        display: flex; align-items: center; justify-content: center;
      }
    }
    .cover-meta {
      position: absolute; bottom: 10px; left: 14px; right: 14px;
      display: flex; justify-content: space-between; color: #fff; font-size: 12px;
      z-index: 3; transition: opacity var(--transition);
    }
  }
  .music-info {
    padding: 12px 4px 0;
    .music-title {
      font-size: 15px; font-weight: 600; color: var(--HiText);
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
      transition: color var(--transition);
      &:hover { color: var(--accent); }
    }
    .music-prompt {
      font-size: 12px; color: var(--text); margin-top: 4px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .user-info {
      display: flex; align-items: center; margin-top: 10px;
      text-decoration: none; color: var(--text); gap: 8px;
      transition: color var(--transition);
      &:hover { color: var(--HiText); }
      .user-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    }
  }
}
</style>
