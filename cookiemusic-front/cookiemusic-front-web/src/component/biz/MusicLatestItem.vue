<template>
  <div class="music-item">
    <div class="cover">
      <Cover :cover="data.cover" borderRadius="50%"></Cover>
      <PlayBtn :data="data" @playList="playList"></PlayBtn>
    </div>
    <div class="music-info">
      <div class="music-title" @click="playMusic(true)">{{ data.musicTitle }}</div>
      <div class="music-prompt">{{ data.prompt }}</div>
      <div class="user-info">
        <router-link :to="`/user/${data.userId}`" class="user-name">{{ data.nickName }}</router-link>
        <span class="meta">▶ {{ proxy.Utils.formatCount(data.playCount) }}</span>
        <span class="meta">♫ {{ proxy.Utils.seconds2Min(data.duration) }}</span>
      </div>
    </div>
    <div class="op-panel">
      <div class="opbtn"><ActionGood :data="data"></ActionGood></div>
      <div class="opbtn"><ActionShare :data="data"></ActionShare></div>
    </div>
  </div>
</template>

<script setup>
import ActionShare from "@/component/biz/ActionShare.vue";
import ActionGood from "@/component/biz/ActionGood.vue";
import { getCurrentInstance } from "vue";
import { useRouter } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
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
  display: flex; align-items: center;
  padding: 12px 18px; gap: 16px;
  background: var(--cardBg); border: 1px solid var(--cardBorder);
  border-radius: var(--radius);
  box-shadow: var(--cardShadow);
  transition: all var(--transition);
  &:hover {
    background: var(--cardBgHover); border-color: var(--cardBorderHover);
    transform: translateY(-2px);
    .op-panel { opacity: 1; }
  }
  .cover {
    width: 72px; height: 72px; flex-shrink: 0;
    border-radius: 50%; position: relative; cursor: pointer;
    background: rgba(255,255,255,0.04);
    display: flex; align-items: center; justify-content: center;
    :deep(img) { border-radius: 50%; width: 72px; height: 72px; object-fit: cover; }
  }
  .music-info {
    flex: 1; min-width: 0;
    display: flex; flex-direction: column; justify-content: center; gap: 4px;
    .music-title {
      font-size: 14px; font-weight: 600; color: var(--HiText);
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer;
      transition: color var(--transition);
      &:hover { color: var(--accent); }
    }
    .music-prompt {
      font-size: 12px; color: var(--text);
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .user-info {
      display: flex; align-items: center; gap: 10px;
      .user-name { font-size: 12px; color: var(--text); text-decoration: none; transition: color var(--transition); &:hover { color: var(--accent); } }
      .meta { font-size: 11px; color: var(--mutedText); }
    }
  }
  .op-panel {
    display: flex; gap: 6px; opacity: 0.4; transition: opacity var(--transition); flex-shrink: 0;
    .opbtn { cursor: pointer; color: var(--text); transition: color var(--transition); &:hover { color: var(--accent); } }
  }
}
@media (max-width: 500px) {
  .music-item { padding: 8px; gap: 10px; .cover { width: 60px; height: 60px; } }
}
</style>
