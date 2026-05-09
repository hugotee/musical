<template>
  <div class="product-item">
    <div class="cover">
      <Cover :cover="data.cover" borderRadius="0px"></Cover>
    </div>
    <div class="product-info">
      <div class="product-name">{{ data.productName }}</div>
      <div class="price">¥ <span>{{ proxy.Utils.convert2Amount(data.price) }}</span></div>
      <div class="integral">+ {{ data.integral }} 积分</div>
      <div class="product-description">{{ data.productDescription }}</div>
    </div>
    <div class="buy-btn" @click="buy">立即购买</div>
  </div>
</template>

<script setup>
import { getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useUserInfoStore } from "@/stores/userInfoStore";
const userInfoStore = useUserInfoStore();

const props = defineProps({ data: { type: Object, default: {} } });
const emits = defineEmits(["pay"]);
const buy = () => {
  if (!userInfoStore.checkLogin()) return;
  emits("pay", props.data);
};
</script>

<style lang="scss" scoped>
.product-item {
  background: var(--cardBg); border: 1px solid var(--cardBorder);
  width: 280px; border-radius: var(--radius); overflow: hidden;
  color: #fff; position: relative; box-shadow: var(--cardShadow);
  transition: all var(--transition);
  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 24px 56px rgba(0,0,0,0.42), 0 4px 12px rgba(0,0,0,0.20);
    border-color: var(--cardBorderHover);
    .buy-btn { opacity: 1; }
  }
  .cover { height: 180px; overflow: hidden; }
  .product-info { padding: 20px 24px 90px;
    .product-name { font-size: 22px; font-weight: 700; background: linear-gradient(120deg, #64d8cb, #f0c36a); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; margin-bottom: 16px; }
    .price { font-size: 22px; font-weight: 700; color: var(--accent); span { font-size: 32px; } }
    .integral { font-size: 15px; font-weight: 600; color: var(--accentWarm); margin: 4px 0 12px; }
    .product-description { font-size: 13px; color: var(--text); line-height: 1.6; }
  }
  .buy-btn {
    position: absolute; bottom: 20px; left: 24px; right: 24px;
    text-align: center; padding: 12px; border-radius: 50px;
    background: var(--btnBg); box-shadow: var(--btnShadow);
    font-weight: 600; font-size: 15px; cursor: pointer;
    opacity: 0.92; transition: all var(--transition);
    &:hover { opacity: 1; transform: translateY(-1px); box-shadow: 0 12px 32px rgba(74,168,216,0.32); }
  }
}
@media (max-width: 500px) {
  .product-item { width: 100%; margin-bottom: 20px; }
}
</style>
