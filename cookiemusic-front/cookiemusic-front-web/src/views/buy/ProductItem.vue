<template>
  <div class="product-item">
    <div class="cover">
      <Cover :cover="data.cover" borderRadius="0px"></Cover>
    </div>
    <div class="product-info">
      <div class="product-name">{{ data.productName }}</div>
      <div class="price">¥ {{ proxy.Utils.convert2Amount(data.price) }}</div>
      <div class="integral">充值积分：{{ data.integral }}</div>
      <div class="product-description">{{ data.productDescription }}</div>
    </div>
    <div class="buy-btn" @click="buy">购买</div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
})

const emits = defineEmits(['pay'])
const buy = () => {
  if (!userInfoStore.checkLogin()) {
    return
  }
  emits('pay', props.data)
}
</script>

<style lang="scss" scoped>
.product-item {
  background: var(--cardBg);
  border: 1px solid var(--line);
  width: 300px;
  height: 500px;
  border-radius: var(--radius);
  overflow: hidden;
  color: #fff;
  position: relative;
  box-shadow: var(--softShadow);
  transition: background 0.2s, border-color 0.2s, transform 0.2s;
  .cover {
    height: 200px;
    overflow: hidden;
  }
  .product-info {
    padding: 20px;
    .product-name {
      font-size: 22px;
      background: linear-gradient(104deg, #f5fbfa, #aeece2 53%, #f0c36a);
      background-clip: text;
      -webkit-text-fill-color: transparent;
      font-weight: bold;
      margin-bottom: 20px;
    }
    .price {
      font-size: 20px;
      font-weight: 500;
      color: var(--accent);
    }
    .integral {
      line-height: 48px;
      font-weight: 700;
      background: linear-gradient(105deg, #7de2d1, #f0c36a);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .product-description {
      font-size: 14px;
      color: var(--text);
    }
  }

  .buy-btn {
    position: absolute;
    bottom: 20px;
    right: 10px;
    left: 10px;
    text-align: center;
    padding: 10px;
    border-radius: 999px;
    background: var(--btnBg);
    color: #061014;
    font-weight: 800;
    margin-top: 10px;
    cursor: pointer;
    box-shadow: var(--btnShadow);
  }
  &:hover {
    background: var(--cardBgHover);
    border-color: var(--lineStrong);
    transform: translateY(-4px);
  }
}

@media (max-width: 500px) {
  .product-item {
    width: 100%;
    margin-right: 0px;
    margin-bottom: 20px;
  }
}
</style>
