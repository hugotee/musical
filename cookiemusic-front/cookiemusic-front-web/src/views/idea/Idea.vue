<template>
  <div class="create-body">
    <div class="create-panel">
      <Create></Create>
    </div>
    <div class="music-panel">
      <MusicList></MusicList>
    </div>
  </div>
</template>

<script setup>
import MusicList from './MusicList.vue'
import Create from './Create.vue'
import Player from '@/component/common/Player.vue'
import { ref, reactive, getCurrentInstance, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
</script>

<style lang="scss" scoped>
.create-body {
  display: flex;
  padding: 18px 18px 0;
  gap: 18px;
  .create-panel {
    width: 600px;
    background: var(--panelBg);
    padding: 18px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    box-shadow: var(--softShadow);
  }
  .music-panel {
    flex: 1;
    width: 0;
    height: calc(100vh - 106px);
    background: rgba(255, 255, 255, 0.025);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    overflow: hidden;
  }
}

@media (max-width: 900px) {
  .create-body {
    flex-direction: column;
    .create-panel,
    .music-panel {
      width: 100%;
    }
  }
}
</style>
