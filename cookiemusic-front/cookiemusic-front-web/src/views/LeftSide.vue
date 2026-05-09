<template>
  <div class="left-side-panel">
    <div class="bg"></div>
    <div class="left-side">
      <div class="logo">
        <span class="logo-icon">♪</span>
        <span>CookieMusic</span>
      </div>
      <div class="menu-list">
        <template v-for="item in menuList">
          <div :class="['menu-item', item.codes.includes(route.meta.code) ? 'active' : '']" @click="jump(item)">
            <div :class="['iconfont', 'icon-' + item.icon]"></div>
            <div class="menu-name">{{ item.name }}</div>
            <div class="menu-dot" v-if="item.codes.includes(route.meta.code)"></div>
          </div>
        </template>
      </div>
      <div class="bottom-section">
        <div class="integral-panel">
          <div class="integral-label">积分余额</div>
          <div class="integral-value">{{ userInfoStore.userInfo.integral || 0 }}</div>
          <div class="record-btn" @click="showIntegralRecord">积分记录 →</div>
        </div>
        <div class="user-info-panel">
          <div class="login-btn" @click="login" v-if="Object.keys(userInfoStore.userInfo).length == 0">
            登录 / 注册
          </div>
          <el-popover v-else popper-class="user-info-popper" placement="top" trigger="click" :show-arrow="false" :offset="5" :width="160" ref="userInfoPopoverRef">
            <template #reference>
              <div class="user-info">
                <Avatar :avatar="userInfoStore.userInfo.avatar" :width="30"></Avatar>
                <div class="user-name">{{ userInfoStore.userInfo.nickName }}</div>
              </div>
            </template>
            <div class="menu-item" @click="updatePassword">修改密码</div>
            <div class="menu-item" @click="editUserInfo">编辑资料</div>
            <div class="menu-item" @click="logout">退出登录</div>
          </el-popover>
        </div>
      </div>
    </div>
  </div>
  <EditUser ref="editUserRef"></EditUser>
  <UpdatePassword ref="updatePasswordRef"></UpdatePassword>
  <IntegralRecord ref="integralRecordRef"></IntegralRecord>
</template>

<script setup>
import IntegralRecord from '@/views/my/IntegralRecord.vue'
import UpdatePassword from '@/views/my/UpdatePassword.vue'
import EditUser from '@/views/my/EditUser.vue'
import { ref, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

const menuList = ref([
  { name: '首页', icon: 'home', codes: ['index'], path: '/' },
  { name: '创作音乐', icon: 'music', path: '/idea', codes: ['idea', 'pure'] },
  { name: '我的', icon: 'user', path: '/my', codes: ['my'] },
  { name: '充值', icon: 'buy', path: '/buy', codes: ['buy'] },
])

const jump = (item) => {
  if (item.path == '/my' && !userInfoStore.checkLogin()) return
  router.push(item.path)
}

const login = () => { userInfoStore.showLogin = true }

const userInfoPopoverRef = ref()
const logout = async () => {
  userInfoPopoverRef.value.hide()
  proxy.Confirm({
    message: '确定要退出吗?',
    okfun: async () => {
      let result = await proxy.Request({ url: proxy.Api.logout })
      if (!result) return
      userInfoStore.userInfo = {}
      userInfoStore.showLogin = false
      localStorage.removeItem('token')
    },
  })
}

const editUserRef = ref()
const updatePasswordRef = ref()
const integralRecordRef = ref()
const editUserInfo = () => { userInfoPopoverRef.value.hide(); editUserRef.value.show() }
const updatePassword = () => { userInfoPopoverRef.value.hide(); updatePasswordRef.value.show() }
const showIntegralRecord = () => { integralRecordRef.value.show() }
</script>

<style lang="scss" scoped>
.left-side-panel {
  width: 210px;
  height: 100%;
  color: #fff;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(10, 20, 28, 0.97), rgba(6, 13, 18, 0.99));
  border-right: 1px solid var(--line);
  .bg {
    position: fixed; top: 0; left: 0;
    height: 360px; width: 210px;
    background: radial-gradient(ellipse 80% 50% at 50% 0%, rgba(100,216,203,0.10), transparent);
    z-index: 0; pointer-events: none;
  }
  .left-side {
    position: relative; z-index: 1;
    width: 210px; height: 100vh;
    display: flex; flex-direction: column;
    .logo {
      display: flex; align-items: center; gap: 8px;
      font-size: 20px; font-weight: 700;
      padding: 26px 20px 22px;
      color: var(--HiText);
      letter-spacing: -0.01em;
      .logo-icon { font-size: 26px; color: var(--accent); }
    }
    .menu-list {
      flex: 1; padding: 0 10px;
      .menu-item {
        display: flex; align-items: center;
        padding: 11px 14px;
        margin-bottom: 2px;
        color: var(--text);
        cursor: pointer;
        border-radius: 12px;
        transition: all var(--transition);
        position: relative;
        .iconfont { font-size: 19px; width: 24px; text-align: center; }
        .menu-name { margin-left: 10px; font-size: 14px; font-weight: 500; }
        .menu-dot { display: none; }
        &:hover {
          color: var(--hiText);
          background: rgba(255,255,255,0.04);
        }
      }
      .active {
        color: var(--accent);
        background: rgba(100, 216, 203, 0.08);
        font-weight: 600;
        box-shadow: inset 0 0 0 1px rgba(100,216,203,0.12);
        &:hover { color: var(--accent); background: rgba(100, 216, 203, 0.11); }
      }
    }
  }
  .bottom-section {
    padding: 0 10px 14px;
    .integral-panel {
      padding: 14px 16px;
      margin-bottom: 10px;
      border-radius: 14px;
      background: rgba(100, 216, 203, 0.05);
      border: 1px solid rgba(100, 216, 203, 0.08);
      .integral-label { font-size: 11px; color: var(--mutedText); text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 4px; }
      .integral-value { font-size: 24px; font-weight: 700; color: var(--accent); }
      .record-btn { margin-top: 6px; font-size: 12px; color: var(--text); cursor: pointer; transition: color var(--transition); &:hover { color: var(--accent); } }
    }
    .user-info-panel {
      .login-btn {
        cursor: pointer; padding: 11px; text-align: center;
        border-radius: 50px;
        background: var(--btnBg);
        box-shadow: var(--btnShadow);
        font-weight: 600; font-size: 14px;
        transition: transform var(--transition), box-shadow var(--transition);
        &:hover { transform: translateY(-1px); box-shadow: 0 12px 32px rgba(74,168,216,0.28); }
      }
      .user-info {
        display: flex; align-items: center;
        background: rgba(255,255,255,0.04);
        border: 1px solid var(--line);
        border-radius: 24px;
        padding: 8px 12px;
        cursor: pointer;
        transition: border var(--transition);
        &:hover { border-color: var(--lineStrong); }
        .user-name {
          flex: 1; width: 0; margin-left: 10px;
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
          color: var(--hiText); font-size: 13px;
        }
      }
    }
  }
}
@media (max-width: 500px) {
  .left-side-panel .bg, .left-side-panel .logo { display: none; }
  .left-side-panel .left-side { height: calc(100vh - 50px); }
}
</style>
