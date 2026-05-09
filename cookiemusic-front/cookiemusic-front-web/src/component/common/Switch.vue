<template>
  <div class="switch-panel">
    <div
      :class="[
        'switch-item',
        modelValue == item.value ? 'switch-item-active' : '',
      ]"
      v-for="item in data"
      @click="changeModel(item)"
    >
      {{ item.label }}
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const prpos = defineProps({
  data: {
    type: Array,
    default: [],
  },
  modelValue: {
    type: [Boolean, Number, String],
  },
});

const emit = defineEmits(["update:modelValue", "change"]);
const changeModel = (item) => {
  if (prpos.modelValue === item.value) {
    return;
  }
  emit("update:modelValue", item.value);
  emit("change", item);
};
</script>

<style lang="scss" scoped>
.switch-panel {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--line);
  border-radius: 26px;
  margin: 10px 0px;
  width: 250px;
  line-height: 35px;
  cursor: pointer;
  padding: 3px;
  &:hover {
    background: rgba(255, 255, 255, 0.07);
  }
  .switch-item {
    width: 50%;
    color: var(--text);
    text-align: center;
    user-select: none;
    border-radius: 22px;
    transition: background 0.2s, color 0.2s;
  }
  .switch-item-active {
    background: var(--btnBg);
    color: #061014;
    font-weight: 700;
    box-shadow: var(--btnShadow);
  }
}
</style>
