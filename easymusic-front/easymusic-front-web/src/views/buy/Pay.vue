<template>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="620px"
    :showCancel="false"
    @close="closePay"
  >
    <div class="pay-panel">
      <!-- 商品信息 -->
      <div class="product-info-panel">
        <div class="title-info">订单详情信息</div>
        <div class="product-info">
          <div class="product-cover">
            <Cover :cover="productInfo.cover" :width="100"></Cover>
          </div>
          <div class="product-name-panel">
            <div class="product-name">{{ productInfo.productName }}</div>
            <div class="sku-name">充值积分：{{ productInfo.integral }}</div>
          </div>
          <div class="price">
            ￥<span>{{ proxy.Utils.convert2Amount(productInfo.price) }}</span>
          </div>
        </div>
      </div>

      <!-- 支付方式切换 -->
      <div class="pay-mode-tabs">
        <div :class="['mode-tab', { active: payMode === 'paycode' }]" @click="payMode = 'paycode'">
          付款码支付
        </div>
        <div :class="['mode-tab', { active: payMode === 'demo' }]" @click="payMode = 'demo'">
          演示模式
        </div>
      </div>

      <!-- 付款码支付 -->
      <template v-if="payMode === 'paycode'">
        <div class="paycode-panel">
          <div class="paycode-title">输入付款码</div>
          <div class="paycode-desc">
            请联系管理员获取付款码，每个付款码只能使用一次
          </div>
          <div class="paycode-form">
            <el-form :model="payCodeForm" :rules="payCodeRules" ref="payCodeFormRef" @submit.prevent>
              <el-form-item prop="payCode">
                <el-input
                  v-model.trim="payCodeForm.payCode"
                  placeholder="请输入8位付款码"
                  :maxlength="8"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="checkCode">
                <div class="captcha-row">
                  <el-input
                    v-model.trim="payCodeForm.checkCode"
                    placeholder="验证码"
                    :maxlength="4"
                    class="captcha-input"
                  />
                  <img
                    :src="checkCodeUrl"
                    class="captcha-img"
                    @click="loadCheckCode"
                    title="点击刷新验证码"
                  />
                </div>
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="action-panel">
          <div class="secondary-btn" @click="closePay">关闭</div>
          <div class="primary-btn" @click="submitPayCode">
            <span v-if="paying">处理中...</span>
            <span v-else>确认支付</span>
          </div>
        </div>
      </template>

      <!-- 演示模式 -->
      <template v-else>
        <div class="pseudo-pay-panel">
          <div class="pseudo-title">扫码联系管理员</div>
          <div class="pseudo-desc">
            演示模式仅供展示支付流程，不自动到账。
            如需真实充值，请切换到"付款码支付"。
          </div>
          <div class="qrcode-wrap">
            <img :src="qrcodeUrl" alt="收款码" />
          </div>
          <div class="pseudo-note">
            扫码后请备注：用户名 + 充值套餐
          </div>
        </div>
        <div class="action-panel">
          <div class="secondary-btn" @click="closePay">关闭</div>
          <div class="primary-btn" @click="confirmPseudoPay">我已联系管理员</div>
        </div>
      </template>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useUserInfoStore } from '@/stores/userInfoStore'

const { proxy } = getCurrentInstance()
const userInfoStore = useUserInfoStore()

const dialogConfig = ref({
  show: false,
  title: '购买',
})

const productInfo = ref({})
const qrcodeUrl = ref(proxy.Utils.getLocalResource('img/qrcode.png'))
const payMode = ref('paycode')
const paying = ref(false)

const pay = async (data) => {
  dialogConfig.value.show = true
  productInfo.value = data
  payMode.value = 'paycode'
  payCodeForm.payCode = ''
  payCodeForm.checkCode = ''
  await nextTick()
  loadCheckCode()
}

// ============ 付款码支付 ============
const payCodeFormRef = ref()
const payCodeForm = reactive({
  payCode: '',
  checkCode: '',
})
const payCodeRules = {
  payCode: [{ required: true, message: '请输入付款码' }],
  checkCode: [{ required: true, message: '请输入验证码' }],
}

const checkCodeUrl = ref(null)
const loadCheckCode = async () => {
  let result = await proxy.Request({
    url: proxy.Api.checkCode,
  })
  if (!result) return
  checkCodeUrl.value = result.data.checkCode
  localStorage.setItem('checkCodeKey', result.data.checkCodeKey)
}

const submitPayCode = async () => {
  const valid = await payCodeFormRef.value.validate().catch(() => false)
  if (!valid) return

  paying.value = true
  let result = await proxy.Request({
    url: proxy.Api.buyByPayCode,
    params: {
      checkCodeKey: localStorage.getItem('checkCodeKey'),
      checkCode: payCodeForm.checkCode,
      payCode: payCodeForm.payCode,
      productId: productInfo.value.productId,
    },
    showLoading: false,
  })
  paying.value = false
  if (!result) {
    loadCheckCode()
    return
  }
  proxy.Message.success('支付成功，积分已到账')
  userInfoStore.updateLastReloadTime()
  closePay()
}

// ============ 演示模式 ============
const confirmPseudoPay = () => {
  proxy.Message.success('请联系管理员人工加积分')
  closePay()
}

const closePay = () => {
  dialogConfig.value.show = false
}

defineExpose({ pay })
</script>

<style lang="scss" scoped>
.pay-panel {
  color: #fff;

  .product-info-panel {
    border: 1px solid rgba(255, 255, 255, 0.18);
    border-radius: 14px;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.04);

    .title-info {
      padding: 10px 14px;
      background: rgba(255, 255, 255, 0.08);
      font-weight: 600;
    }

    .product-info {
      display: flex;
      align-items: center;
      padding: 14px;

      .product-cover {
        border-radius: 8px;
        overflow: hidden;
        flex-shrink: 0;
      }

      .product-name-panel {
        margin: 0 12px;
        flex: 1;
        width: 0;

        .product-name {
          font-weight: bold;
          font-size: 18px;
        }

        .sku-name {
          margin-top: 6px;
          font-size: 13px;
          color: var(--activeText);
        }
      }

      .price {
        color: #ffd700;
        font-size: 18px;
        span {
          font-size: 22px;
        }
      }
    }
  }

  .pay-mode-tabs {
    display: flex;
    margin-top: 14px;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.15);

    .mode-tab {
      flex: 1;
      text-align: center;
      padding: 10px 0;
      cursor: pointer;
      font-size: 14px;
      background: rgba(255, 255, 255, 0.04);
      transition: all 0.2s;
      color: #aaa;

      &.active {
        background: linear-gradient(90deg, #18b7a6, #b61fc2);
        color: #fff;
        font-weight: 600;
      }
    }
  }

  .paycode-panel {
    margin-top: 16px;
    padding: 18px 16px 14px;
    border-radius: 16px;
    background: linear-gradient(160deg, rgba(24, 120, 200, 0.15), rgba(29, 17, 63, 0.9));
    border: 1px solid rgba(255, 255, 255, 0.14);
    text-align: center;

    .paycode-title {
      font-size: 22px;
      font-weight: 700;
      letter-spacing: 1px;
      margin-bottom: 8px;
    }

    .paycode-desc {
      font-size: 13px;
      line-height: 1.7;
      color: #b7c2db;
      margin: 0 auto 14px;
      max-width: 400px;
    }

    .paycode-form {
      max-width: 320px;
      margin: 0 auto;

      .captcha-row {
        display: flex;
        gap: 10px;
        align-items: center;

        .captcha-input {
          flex: 1;
          min-width: 0;
        }

        .captcha-img {
          height: 38px;
          width: 90px;
          border-radius: 6px;
          cursor: pointer;
          flex-shrink: 0;
        }
      }
    }
  }

  .pseudo-pay-panel {
    margin-top: 16px;
    padding: 18px 16px 14px;
    border-radius: 16px;
    background: linear-gradient(160deg, rgba(27, 120, 68, 0.18), rgba(29, 17, 63, 0.9));
    border: 1px solid rgba(255, 255, 255, 0.14);
    text-align: center;

    .pseudo-title {
      font-size: 22px;
      font-weight: 700;
      letter-spacing: 1px;
      margin-bottom: 10px;
    }

    .pseudo-desc {
      font-size: 14px;
      line-height: 1.7;
      color: #dfe9ff;
      margin: 0 auto 14px;
      max-width: 500px;
    }

    .qrcode-wrap {
      width: 290px;
      margin: 0 auto;
      padding: 12px;
      border-radius: 16px;
      background: rgba(255, 255, 255, 0.96);
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.22);

      img {
        display: block;
        width: 100%;
        height: auto;
        border-radius: 10px;
        object-fit: contain;
      }
    }

    .pseudo-note {
      margin-top: 12px;
      color: #b7c2db;
      font-size: 13px;
    }
  }

  .action-panel {
    margin-top: 16px;
    display: flex;
    justify-content: center;
    gap: 14px;

    .secondary-btn,
    .primary-btn {
      min-width: 140px;
      text-align: center;
      padding: 11px 18px;
      border-radius: 22px;
      cursor: pointer;
      user-select: none;
      transition: all 0.2s ease;
    }

    .secondary-btn {
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.18);

      &:hover {
        background: rgba(255, 255, 255, 0.14);
      }
    }

    .primary-btn {
      background: linear-gradient(90deg, #18b7a6, #b61fc2);

      &:hover {
        opacity: 0.92;
      }
    }
  }
}

@media (max-width: 500px) {
  .pay-panel {
    .product-info-panel {
      .product-info {
        flex-wrap: wrap;
        gap: 10px;

        .product-name-panel {
          margin: 0;
          width: 100%;
          flex: unset;
        }
      }
    }

    .pseudo-pay-panel {
      .qrcode-wrap {
        width: 240px;
      }
    }

    .action-panel {
      flex-direction: column;

      .secondary-btn,
      .primary-btn {
        width: 100%;
      }
    }
  }
}
</style>
