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
    color: var(--HiText);
    background: var(--text);
    border-radius: 12px;
    padding: 3px 10px;
    margin: 0px 8px 8px 0px;
    cursor: pointer;
    user-select: none;
    font-size: 14px;
  }
  .selected {
    background: var(--activeText);
  }
}
</style>
