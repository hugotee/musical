<template>
  <div class="index-body">
    <div class="part-title">
      <div class="title">推荐歌曲</div>
      <div class="title-op">
        <div
          :class="[
            'iconfont icon-narrow-left',
            disableType == 1 ? 'disable' : '',
          ]"
          @click="changeCommend(1)"
        ></div>
        <div
          :class="[
            'iconfont icon-narrow-right',
            disableType == -1 ? 'disable' : '',
          ]"
          @click="changeCommend(-1)"
        ></div>
      </div>
    </div>
    <CommendList
      ref="commendListRef"
      @disableType="hotChangeTypeHandler"
    ></CommendList>
    <div class="part-title latest-title">
      <div>最新发布歌曲</div>
      <router-link to="latest" class="more"
        >更多<span class="iconfont iconfont icon-narrow-right"></span>
      </router-link>
    </div>
    <div class="latest-list">
      <LatestList :indexType="1"></LatestList>
    </div>
  </div>
</template>

<script setup>
import LatestList from "./LatestList.vue";
import CommendList from "./CommendList.vue";
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const commendListRef = ref();
const disableType = ref(1);
const hotChangeTypeHandler = (type) => {
  disableType.value = type;
};
const changeCommend = (type) => {
  commendListRef.value.change(type);
};
</script>

<style lang="scss" scoped>
.index-body {
  padding: 26px 22px 0px;
  .part-title {
    font-size: 24px;
    font-weight: 800;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    .title-op {
      display: flex;
      .iconfont {
        background: rgba(255, 255, 255, 0.06);
        border: 1px solid var(--line);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 10px;
        margin-right: 10px;
        cursor: pointer;
        transition: background 0.2s, transform 0.2s;
        &:hover {
          background: rgba(125, 226, 209, 0.13);
          transform: translateY(-1px);
        }
      }
      .disable {
        opacity: 0.5;
        cursor: not-allowed;
        &:hover {
          opacity: 0.5;
        }
      }
    }
  }
  .latest-title {
    margin-top: 24px;
    .more {
      margin-left: 20px;
      text-decoration: none;
      margin-right: 20px;
      color: #fff;
      font-size: 14px;
      padding: 5px 10px;
      display: flex;
      align-items: center;
      .iconfont {
        font-size: 12px;
        margin-left: 3px;
      }
      &:hover {
        color: var(--accent);
        background: rgba(255, 255, 255, 0.06);
        border-radius: 15px;
      }
    }
  }
  .latest-list {
    padding-right: 10px;
    margin-top: 8px;
  }
}
</style>
