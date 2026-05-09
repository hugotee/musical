<template>
  <transition name="player-fade">
    <div class="player" v-if="musicPlayStore.currentMusic.musicId">
      <div class="music-info-panel">
        <div class="cover">
          <Cover :cover="musicPlayStore.currentMusic.cover" :lazy="false"></Cover>
          <PlayBtn :data="musicPlayStore.currentMusic" :showBorder="false"></PlayBtn>
        </div>
        <div class="music-info">
          <div class="music-title" @click="playMusic()">{{ musicPlayStore.currentMusic.musicTitle }}</div>
          <router-link class="music-author" :to="`/user/${musicPlayStore.currentMusic.userId}`">
            {{ musicPlayStore.currentMusic.nickName }}
          </router-link>
        </div>
      </div>
      <Player></Player>
      <div class="op-panel">
        <ActionGood :data="musicPlayStore.currentMusic"></ActionGood>
        <ActionShare :data="musicPlayStore.currentMusic"></ActionShare>
      </div>
    </div>
  </transition>
</template>

<script setup>
import ActionShare from '@/component/biz/ActionShare.vue'
import ActionGood from '@/component/biz/ActionGood.vue'
import Player from '@/component/common/Player.vue'
import { getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
import { useMusicPlayStore } from '@/stores/musicPlay.js'
const musicPlayStore = useMusicPlayStore()
const playMusic = () => { router.push(`/play/${musicPlayStore.currentMusic.musicId}`) }
</script>

<style lang="scss" scoped>
.player {
  z-index: 500;
  position: fixed; bottom: 0;
  left: 210px; right: 0;
  background: rgba(9, 18, 26, 0.94);
  backdrop-filter: blur(20px) saturate(1.2);
  border-top: 1px solid var(--line);
  box-shadow: 0 -20px 60px rgba(0, 0, 0, 0.35);
  height: 72px;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  .music-info-panel {
    display: flex; align-items: center; width: 280px;
    .cover { width: 48px; height: 48px; position: relative; border-radius: 8px; overflow: hidden; flex-shrink: 0; }
    .music-info {
      margin-left: 12px; display: flex; flex-direction: column; justify-content: center; min-width: 0;
      .music-title {
        cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
        font-size: 13px; font-weight: 600;
        transition: color var(--transition);
        &:hover { color: var(--accent); }
      }
      .music-author {
        margin-top: 3px; opacity: 0.6; font-size: 12px;
        text-decoration: none; color: var(--text);
        transition: opacity var(--transition);
        &:hover { opacity: 1; }
      }
    }
  }
  .op-panel {
    display: flex; gap: 18px; align-items: center;
    .iconfont { cursor: pointer; font-size: 22px; transition: color var(--transition); }
  }
}
.player-fade-enter-active, .player-fade-leave-active { transition: transform 0.3s ease, opacity 0.3s ease; }
.player-fade-enter-from, .player-fade-leave-to { transform: translateY(100%); opacity: 0; }

@media (max-width: 500px) {
  .player {
    left: 0; width: 100vw;
    .op-panel { display: none; }
    .music-info-panel { width: 160px; }
  }
}
</style>
