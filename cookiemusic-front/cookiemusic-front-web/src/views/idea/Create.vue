<template>
  <div class="create-tab">
    <el-tabs v-model="formData.musicType">
      <el-tab-pane label="歌曲" :name="0"></el-tab-pane>
      <el-tab-pane label="纯音乐" :name="1"></el-tab-pane>
    </el-tabs>
    <div class="cost-hint">
      本次创作消耗 <span class="cost-num">{{ currentCost }}</span> 积分
    </div>
  </div>
  <div class="create-form">
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent>
      <Switch :data="[
          { label: '简单模式', value: 0 },
          { label: '高级模式', value: 1 },
        ]" v-model="formData.modeType"></Switch>
      <div class="model-panel">
        <div class="part-title">模型</div>
        <TabSelect
          :multiple="false"
          :data="currentModelOptions"
          label-key="modelLabel"
          value-key="dictCode"
          v-model="formData.model"
        ></TabSelect>
        <div class="model-desc" v-if="currentModelDesc">{{ currentModelDesc }}</div>
      </div>
      <!--input输入-->
      <template v-if="formData.modeType == 0">
        <div class="input-panel">
          <el-input clearable placeholder="请输入你的想法" v-model="formData.prompt" type="textarea" :rows="8" resize="none"
            :maxlength="500" show-word-limit>
          </el-input>
          <div class="change-btn" @click="getPrompt">
            <div class="iconfont icon-magic">变变变</div>
          </div>
        </div>
      </template>
      <template v-else>
        <div :class="[
            'advanced-panel',
            formData.musicType === 1 ? 'advanced-panel-line' : '',
          ]">
          <div class="lyric-panel">
            <div class="input-panel">
              <el-input clearable placeholder="请输入提示，或者标题" v-model="formData.prompt" type="textarea" :rows="5"
                resize="none" :maxlength="500" show-word-limit>
              </el-input>
              <div class="change-btn" @click="getPrompt">
                <div class="iconfont icon-magic">变变变</div>
              </div>
            </div>
            <div class="input-panel lyric-input" v-if="formData.musicType === 0">
              <el-input clearable placeholder="请输入歌词" v-model="formData.lyrics" type="textarea" resize="none"
                :maxlength="1500" show-word-limit>
              </el-input>
            </div>
          </div>
          <div class="setting-panel">
            <div class="part-title">曲风</div>
            <TabSelect :data="sysSetting[SYS_SETTING_KEY.music_grenre.key]" v-model="formData.musicGener"></TabSelect>
            <div class="part-title">情绪</div>
            <TabSelect :data="sysSetting[SYS_SETTING_KEY.music_emotion.key]" v-model="formData.musicEmotion">
            </TabSelect>
            <template v-if="formData.musicType === 0">
              <div class="part-title">人声</div>
              <TabSelect :multiple="false" :data="sysSetting[SYS_SETTING_KEY.music_sex.key]"
                v-model="formData.musicSex"></TabSelect>
            </template>
          </div>
        </div>
      </template>
      <div class="submit-btn" @click="createMusic">
        <el-icon class="is-loading" v-if="creating">
          <Loading style="width: 1em; height: 1em" />
        </el-icon>
        <span v-else>创作音乐</span>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import Switch from '@/component/common/Switch.vue'
import TabSelect from '@/component/common/TabSelect.vue'
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
  computed,
  watch,
} from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

import { mitter } from '@/eventbus/eventBus.js'

const SYS_SETTING_KEY = {
  //曲风
  music_grenre: {
    key: 'music_grenre',
    valueKey: 'dictCode',
  },
  //情绪
  music_emotion: {
    key: 'music_emotion',
    valueKey: 'dictCode',
  },
  //人声
  music_sex: {
    key: 'music_sex',
    valueKey: 'dictCode',
  },
  //音乐提示词
  music_prompt: {
    key: 'music_prompt',
    valueKey: 'dictCode',
  },
  //纯音乐提示词
  music_prompt_pure: {
    key: 'music_prompt_pure',
    valueKey: 'dictCode',
  },
  //音乐模型
  music_model: {
    key: 'music_model',
  },
  //纯音乐模型
  music_model_pure: {
    key: 'music_model_pure',
  },
}

const getPrompt = () => {
  let prompts = []
  if (formData.value.musicType == 0) {
    prompts = sysSetting.value[SYS_SETTING_KEY.music_prompt.key]
  } else if (formData.value.musicType == 1) {
    prompts = sysSetting.value[SYS_SETTING_KEY.music_prompt_pure.key]
  }
  if (prompts == null) {
    return
  }
  formData.value.prompt = prompts[Math.floor(Math.random() * prompts.length)]
}

const sysSetting = ref({})

const getModelDictKey = () => {
  return formData.value.musicType == 0
    ? SYS_SETTING_KEY.music_model.key
    : SYS_SETTING_KEY.music_model_pure.key
}

const currentModelOptions = computed(() => {
  const models = sysSetting.value[getModelDictKey()] || []
  const availableModels =
    formData.value.musicType == 1
      ? models.filter((item) => item.dictCode === "V3")
      : models
  return [...availableModels]
    .sort((a, b) => getModelRank(a) - getModelRank(b))
    .map((item) => {
    const modelName = formData.value.musicType == 1 ? "MusicGen Small" : item.dictCode
    return {
      ...item,
      modelLabel: `${modelName} · ${item.dictValue}积分`,
    }
    })
})

const getModelRank = (item) => {
  const modelNumber = Number(String(item.dictCode || "").replace("V", ""))
  return Number.isNaN(modelNumber) ? item.sort || 0 : modelNumber
}

const currentModelInfo = computed(() => {
  return currentModelOptions.value.find((item) => item.dictCode === formData.value.model)
})

const currentModelDesc = computed(() => {
  if (formData.value.musicType == 1 && currentModelInfo.value) {
    return `本地 MusicGen Small 生成约30s纯音乐(${currentModelInfo.value.dictValue}积分/首)`
  }
  return currentModelInfo.value?.dictDesc || ""
})

const syncModelSelection = () => {
  if (currentModelOptions.value.length === 0) {
    return
  }
  if (!currentModelInfo.value) {
    formData.value.model = currentModelOptions.value[0].dictCode
  }
}

const loadSysSetting = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadSysDict,
  })
  if (!result) {
    return
  }
  for (let key in result.data) {
    if (SYS_SETTING_KEY[key].valueKey) {
      result.data[key] = result.data[key].map((item) => {
        return item[SYS_SETTING_KEY[key].valueKey]
      })
    }
  }
  sysSetting.value = result.data
  syncModelSelection()
  if (route.params.creationId) {
    return
  }
  getPrompt()
}

const currentCost = computed(() => {
  return currentModelInfo.value ? currentModelInfo.value.dictValue : 0
})

const formData = ref({
  modeType: 0,
  musicType: 0,
  model: "V3",
})
const formDataRef = ref()
const rules = {
  title: [{ required: true, message: '请输入内容' }],
}

const creating = ref(false)
const createMusic = async () => {
  if (!userInfoStore.checkLogin()) {
    return
  }

  if (creating.value) {
    return
  }
  if (!formData.value.prompt) {
    proxy.Message.warning('请输入提示词')
    return
  }
  creating.value = true
  let result = await proxy.Request({
    url: proxy.Api.createMusic,
    params: { ...formData.value },
    showLoading: false,
    timeout: 180 * 1000,
  })
  creating.value = false
  if (!result) {
    return
  }
  mitter.emit('newMusic', result.data)

  //重新加载积分
  userInfoStore.updateLastReloadTime()

  proxy.Alert({
    message: '创作成功',
  })
}

const getCreation = async () => {
  const creationId = route.params.creationId
  if (!creationId) {
    return
  }

  let result = await proxy.Request({
    url: proxy.Api.getCreation,
    params: {
      creationId,
    },
  })
  if (!result) {
    return
  }
  //初始化设置
  if (result.data.modeType == 1) {
    result.data = { ...result.data, ...JSON.parse(result.data.settings) }
  }
  formData.value = result.data
  syncModelSelection()
}

watch(
  () => formData.value.musicType,
  () => {
    syncModelSelection()
  }
)

onMounted(() => {
  loadSysSetting()
  getCreation()
})
</script>

<style lang="scss" scoped>
.create-tab {
  :deep(.el-tabs__header) {
    margin-bottom: 0px;
  }
  :deep(.el-tabs__item) {
    color: var(--text);
    font-size: 24px;
    font-weight: 800;
    padding-bottom: 10px;
  }
  :deep(.el-tabs__item.is-active) {
    color: var(--accent);
  }
  :deep(.el-tabs__active-bar) {
    background: var(--accent);
  }
  :deep(.el-tabs__nav-wrap) {
    &::after {
      background: var(--line);
    }
  }
}
.cost-hint {
  text-align: center;
  font-size: 13px;
  color: var(--text);
  margin: 6px 0 12px 0;
  .cost-num {
    color: #ffd700;
    font-weight: bold;
    font-size: 15px;
  }
}
.create-form {
  color: #fff;
  .input-panel {
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    overflow: hidden;
    :deep(.el-textarea__inner) {
      background: transparent;
      box-shadow: none;
      height: 100%;
      border-radius: 0px;
      color: var(--hiText);
    }
    :deep(.el-input__count) {
      background: none;
    }
    ::-webkit-scrollbar {
      display: none;
    }
    .change-btn {
      text-align: right;
      padding: 10px;
      color: #fff;
      cursor: pointer;
      display: flex;
      justify-content: flex-end;
      .icon-magic {
        border-radius: 999px;
        padding: 5px 10px;
        background: rgba(255, 255, 255, 0.06);
        border: 1px solid var(--line);
        font-size: 13px;
        &::before {
          margin-right: 5px;
          font-size: 16px;
        }
      }
    }
  }

  .advanced-panel {
    display: flex;
    .lyric-panel {
      width: 300px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      overflow: hidden;
      height: 100%;
      .lyric-input {
        border-radius: 0px;
        border-top: 1px solid var(--line);
        :deep(.el-textarea__inner) {
          height: calc(100vh - 515px);
          color: var(--hiText);
        }
      }
    }
    .setting-panel {
      padding: 0px 10px 0px 10px;
      flex: 1;
      width: 0;
    }
  }
  .advanced-panel-line {
    flex-direction: column;
    .lyric-panel {
      width: 100%;
    }
    .setting-panel {
      width: 100%;
    }
  }
  .part-title {
    line-height: 40px;
  }

  .model-panel {
    margin-bottom: 10px;
    .model-desc {
      margin-top: 2px;
      font-size: 13px;
      color: var(--text);
      line-height: 20px;
    }
  }

  .submit-btn {
    cursor: pointer;
    text-align: center;
    line-height: 45px;
    height: 45px;
    font-weight: bold;
    font-size: 20px;
    border-radius: 999px;
    margin-top: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--btnBg);
    box-shadow: var(--btnShadow);
    &:hover {
      opacity: 0.9;
    }
  }
}

@media (max-width: 500px) {
  .create-form {
    .advanced-panel {
      flex-direction: column;
      .lyric-panel {
        width: 100%;
      }
      .setting-panel {
        width: 100%;
      }
    }
  }
}
</style>
