// pages/login/index.js · 账号登录与注册（与 Web 端共用 /api/auth/*）
const api = require('../../utils/api.js')

Page({
  data: {
    mode: 'login',
    form: { username: '', password: '', email: '' },
    loading: false,
    error: ''
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode, error: '' })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    const patch = { error: '' }
    patch['form.' + field] = e.detail.value
    this.setData(patch)
  },

  submit() {
    const that = this
    const form = this.data.form
    const username = String(form.username || '').trim()
    const password = String(form.password || '')
    const email = String(form.email || '').trim()
    if (!username || !password) { this.setData({ error: '请填写账号与密码' }); return }
    if (this.data.mode === 'register' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.setData({ error: '请填写有效的邮箱' }); return
    }
    const payload = this.data.mode === 'register'
      ? { username: username, password: password, email: email }
      : { username: username, password: password }
    const path = this.data.mode === 'register' ? '/auth/register' : '/auth/login'

    this.setData({ loading: true, error: '' })
    api.post(path, payload, { skipAuthExpired: true }).then(function (res) {
      api.setStoredUser(res)
      that.setData({ loading: false })
      wx.showToast({ title: '欢迎回到放映室', icon: 'success' })
      setTimeout(function () {
        if (getCurrentPages().length > 1) wx.navigateBack()
        else wx.switchTab({ url: '/pages/index/index' })
      }, 700)
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '操作失败' })
    })
  }
})
