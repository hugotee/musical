<template>
  <div class="tab-select">
    <div
      v-for="item in data"
      :key="getItemValue(item)"
      :class="['tab', isSelected(item) ? 'selected' : '']"
      @click="selectTab(item)"
    >
      {{ getItemLabel(item) }}
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: [Array, String],
    default: () => [],
  },
  multiple: {
    type: Boolean,
    default: true,
  },
  labelKey: {
    type: String,
    default: "",
  },
  valueKey: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["update:modelValue"]);

const isObjectItem = (item) => item !== null && typeof item === "object";

const getItemValue = (item) => {
  return props.valueKey && isObjectItem(item) ? item[props.valueKey] : item;
};

const getItemLabel = (item) => {
  if (props.labelKey && isObjectItem(item) && item[props.labelKey]) {
    return item[props.labelKey];
  }
  return getItemValue(item);
};

const isSelected = (item) => {
  const value = getItemValue(item);
  if (props.multiple) {
    const selectedValues = Array.isArray(props.modelValue) ? props.modelValue : [];
    return selectedValues.includes(value);
  }
  return props.modelValue === value;
};

const selectTab = (item) => {
  const value = getItemValue(item);
  if (props.multiple) {
    const newValue = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
    const index = newValue.indexOf(value);
    // 切换选中状态
    if (index > -1) {
      newValue.splice(index, 1);
    } else {
      newValue.push(value);
    }
    emit("update:modelValue", newValue);
  } else {
    emit("update:modelValue", value);
  }
};
</script>

<style lang="scss" scoped>
.tab-select {
  display: flex;
  flex-wrap: wrap;
  .tab {
    color: var(--text);
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--line);
    border-radius: 999px;
    padding: 6px 12px;
    margin: 0px 8px 8px 0px;
    cursor: pointer;
    user-select: none;
    font-size: 14px;
    transition: background 0.2s, color 0.2s, border-color 0.2s, transform 0.2s;
    &:hover {
      color: var(--HiText);
      border-color: var(--lineStrong);
      transform: translateY(-1px);
    }
  }
  .selected {
    color: #061014;
    background: var(--btnBg);
    border-color: transparent;
    font-weight: 700;
    box-shadow: var(--btnShadow);
  }
}
</style>
