<template>
  <div class="music-item">
    <div class="cover">
      <Cover :cover="data.cover" borderRadius="50%"></Cover>
      <PlayBtn :data="data" @playList="playList"></PlayBtn>
    </div>
    <div class="music-info">
      <div class="music-title" @click="playMusic(true)">
        {{ data.musicTitle }}
      </div>
      <div class="music-prompt">{{ data.prompt }}</div>
      <div class="user-info">
        <div class="user-avatar">
          <router-link :to="`/user/${data.userId}`">
            <Avatar :avatar="data.avatar" :width="20"></Avatar>
          </router-link>
        </div>
        <router-link :to="`/user/${data.userId}`" class="user-name">{{
          data.nickName
        }}</router-link>
        <div class="iconfont icon-play">{{ data.playCount }}</div>
        <div class="iconfont icon-time">
          {{ proxy.Utils.seconds2Min(data.duration) }}
        </div>
      </div>
    </div>
    <div class="op-panel">
      <div class="opbtn opbtn-good">
        <ActionGood :data="data"></ActionGood>
      </div>
      <div class="opbtn opbtn-share">
        <ActionShare :data="data"></ActionShare>
      </div>
    </div>
  </div>
</template>

<script setup>
import ActionShare from "@/component/biz/ActionShare.vue";
import ActionGood from "@/component/biz/ActionGood.vue";
import { ref, reactive, getCurrentInstance, nextTick, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useMusicPlayStore } from "@/stores/musicPlay.js";
const musicPlayStore = useMusicPlayStore();

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
});
const emits = defineEmits(["playList"]);
const playMusic = (jumpDetail) => {
  emits("playList");
  musicPlayStore.play({ ...props.data });
  if (!jumpDetail) {
    return;
  }
  router.push(`/play/${props.data.musicId}`);
};

const playList = () => {
  emits("playList");
};
</script>

<style lang="scss" scoped>
.music-item {
  display: flex;
  padding: 10px 20px 10px 10px;
  min-width: 320px;
  min-height: 112px;
  background: var(--cardBg);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.18);
  transition: background 0.2s, border-color 0.2s, transform 0.2s;
  &:hover {
    background: var(--cardBgHover);
    border-color: var(--lineStrong);
    transform: translateY(-2px);
  }
  .cover {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 12px;
    width: 100px;
    height: 100px;
    padding: 10px;
    cursor: pointer;
    position: relative;
    img {
      max-width: 100%;
      border-radius: 50%;
    }
  }
  .music-info {
    margin: 0px 15px;
    flex: 1;
    width: 0;
    color: #ffff;
    display: flex;
    flex-direction: column;
    justify-content: center;
    .music-title {
      display: inline-block;
      color: #fff;
      font-size: 16px;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      cursor: pointer;
      &:hover {
        color: var(--accent);
      }
    }
    .music-prompt {
      font-size: 12px;
      font-weight: 500;
      color: var(--text);
      opacity: 1;
      margin-top: 5px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .user-info {
      display: flex;
      align-items: center;
      margin-top: 10px;
      .user-avatar {
        margin-right: 5px;
        opacity: 1;
      }
      .user-name {
        font-size: 14px;
        margin-right: 10px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: var(--text);
        opacity: 1;
        text-decoration: none;
        color: #fff;
      }
      .iconfont {
        opacity: 0.72;
        &::before {
          margin-right: 4px;
        }
      }
      .icon-play {
        font-size: 12px;
        margin-right: 10px;
      }
      .icon-time {
        font-size: 14px;
      }
    }
  }
  .op-panel {
    display: flex;
    justify-content: space-between;
    color: #fff;
    align-items: center;
    width: 80px;
    .opbtn {
      cursor: pointer;
      color: var(--text);
      &:hover {
        color: var(--accent);
      }
    }
  }
}
@media (max-width: 500px) {
  .music-item {
    padding: 3px;
    .user-name {
      display: none;
    }
  }
}
</style>
