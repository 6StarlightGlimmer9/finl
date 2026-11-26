// utils/api.js · wx.request 统一封装（对齐 Web 端 src/api.js 的行为）
const config = require('../config.js')

const STORAGE_KEY = 'screenroom-user'

function getStoredUser() {
  try {
    const value = wx.getStorageSync(STORAGE_KEY)
    return value && typeof value === 'object' && value.token ? value : null
  } catch (e) {
    return null
  }
}

function setStoredUser(user) {
  if (user && user.token) wx.setStorageSync(STORAGE_KEY, user)
  else clearStoredUser()
}

function clearStoredUser() {
  try { wx.removeStorageSync(STORAGE_KEY) } catch (e) { /* ignore */ }
}

function isLoggedIn() { return !!getStoredUser() }
function isAdmin() {
  const user = getStoredUser()
  return !!user && user.role === 'ADMIN'
}

function toast(title) {
  wx.showToast({ title: String(title || ''), icon: 'none', duration: 2200 })
}

function goDetail(id) {
  if (id === undefined || id === null || id === '') return
  wx.navigateTo({ url: '/pages/detail/index?id=' + id })
}

// 自定义 TabBar 需要在每个 Tab 页 onShow 时同步选中项
function syncTabBar(page, index) {
  function apply() {
    if (page && typeof page.getTabBar === 'function' && page.getTabBar()) {
      page.getTabBar().setData({ selected: index })
      return true
    }
    return false
  }
  // 首次进入时自定义 TabBar 可能尚未挂载，稍后重试一次
  if (!apply()) setTimeout(apply, 50)
}

// 底部弹层打开时隐藏 TabBar，避免输入框与按钮被图标遮挡
function setTabBarVisible(visible, page) {
  const pages = getCurrentPages()
  const target = page || (pages.length ? pages[pages.length - 1] : null)
  function apply() {
    if (target && typeof target.getTabBar === 'function' && target.getTabBar()) {
      target.getTabBar().setData({ hidden: !visible })
      return true
    }
    return false
  }
  if (!apply()) setTimeout(apply, 50)
}

function requireLogin(message) {
  if (isLoggedIn()) return true
  wx.showModal({
    title: '需要登录',
    content: message || '登录后即可参与放映室互动',
    confirmText: '去登录',
    success: function (res) {
      if (res.confirm) wx.navigateTo({ url: '/pages/login/index' })
    }
  })
  return false
}

function buildQuery(params) {
  if (!params) return ''
  const parts = Object.keys(params)
    .filter(function (key) { return params[key] !== undefined && params[key] !== null && params[key] !== '' })
    .map(function (key) { return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]) })
  return parts.length ? '?' + parts.join('&') : ''
}

function errorMessage(res) {
  const data = res && res.data
  if (data && typeof data === 'object') {
    return data.message || data.error || ('请求失败（HTTP ' + res.statusCode + '）')
  }
  return '请求失败（HTTP ' + ((res && res.statusCode) || '未知') + '）'
}

// path 为 /api 之后的路径，如 /anime、/anime/1/comments
function request(path, options) {
  options = options || {}
  const method = (options.method || 'GET').toUpperCase()
  const header = Object.assign({}, options.header || {})
  const user = getStoredUser()
  if (user && user.token) header.Authorization = 'Bearer ' + user.token
  const hasBody = method !== 'GET' && options.data !== undefined && options.data !== null
  if (hasBody && !header['Content-Type']) header['Content-Type'] = 'application/json'

  const url = config.getBaseUrl() + path + (method === 'GET' ? buildQuery(options.query) : '')
  return new Promise(function (resolve, reject) {
    const payload = {
      url: url,
      method: method,
      header: header,
      timeout: options.timeout || 15000,
      success: function (res) {
        // 后端 AuthInterceptor 返回 401：清除本地凭证；登录/注册接口本身除外
        if (res.statusCode === 401 && !options.skipAuthExpired) {
          clearStoredUser()
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
        }
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data)
        else reject(new Error(errorMessage(res)))
      },
      fail: function (err) {
        const message = (err && err.errMsg) || ''
        if (/domain|合法域名|not in domain/i.test(message)) {
          reject(new Error('域名校验未通过，请关闭开发者工具的域名校验'))
        } else {
          reject(new Error('无法连接服务器，请稍后重试'))
        }
      }
    }
    if (method !== 'GET') payload.data = options.data
    wx.request(payload)
  })
}

function get(path, query, options) {
  return request(path, Object.assign({}, options, { method: 'GET', query: query }))
}
function post(path, data, options) {
  return request(path, Object.assign({}, options, { method: 'POST', data: data === undefined ? {} : data }))
}
function put(path, data, options) {
  return request(path, Object.assign({}, options, { method: 'PUT', data: data === undefined ? {} : data }))
}
function del(path, options) {
  return request(path, Object.assign({}, options, { method: 'DELETE' }))
}

function logout() {
  return post('/auth/logout', {}, { skipAuthExpired: true })
    .catch(function () { /* 即使服务端失败也清理本地登录态 */ })
    .then(function () { clearStoredUser() })
}

module.exports = {
  get: get,
  post: post,
  put: put,
  del: del,
  request: request,
  getStoredUser: getStoredUser,
  setStoredUser: setStoredUser,
  clearStoredUser: clearStoredUser,
  isLoggedIn: isLoggedIn,
  isAdmin: isAdmin,
  requireLogin: requireLogin,
  goDetail: goDetail,
  syncTabBar: syncTabBar,
  setTabBarVisible: setTabBarVisible,
  toast: toast,
  logout: logout
}
