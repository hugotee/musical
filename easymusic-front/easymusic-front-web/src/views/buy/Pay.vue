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

      <div class="pseudo-pay-panel">
        <div class="pseudo-title">扫码联系管理员</div>
        <div class="pseudo-desc">
          当前版本不接入微信商户，扫码后请联系后台管理员人工加积分。
          此页面仅用于伪支付演示，不支持自动跳转、自动到账和支付回调。
        </div>
        <div class="qrcode-wrap">
          <img :src="qrcodeUrl" alt="收款码" />
        </div>
        <div class="pseudo-note">
          扫码后请备注：用户名 + 充值套餐；管理员收到后会在后台手动给你增加积分。
        </div>
        <div class="hint-list">
          <div>1. 扫码联系管理员，不走自动支付流程</div>
          <div>2. 备注你的账号信息，方便后台核对</div>
          <div>3. 管理员确认后手动增加积分</div>
        </div>
      </div>

      <div class="action-panel">
        <div class="secondary-btn" @click="closePay">关闭</div>
        <div class="primary-btn" @click="confirmPseudoPay">我已联系管理员</div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, getCurrentInstance, nextTick } from 'vue'

const { proxy } = getCurrentInstance()

const dialogConfig = ref({
  show: false,
  title: '购买',
})

const productInfo = ref({})
const qrcodeUrl = ref(proxy.Utils.getLocalResource('img/qrcode.png'))

const pay = async (data) => {
  dialogConfig.value.show = true
  productInfo.value = data
  await nextTick()
}

const closePay = () => {
  dialogConfig.value.show = false
}

const confirmPseudoPay = () => {
  proxy.Message.success('请联系管理员人工加积分')
  closePay()
}

defineExpose({
  pay,
})
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

    .hint-list {
      margin-top: 10px;
      text-align: left;
      max-width: 460px;
      margin-left: auto;
      margin-right: auto;
      color: #e3ebff;
      font-size: 13px;
      line-height: 1.8;
      padding: 10px 12px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.08);
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
