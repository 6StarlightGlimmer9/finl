// app.js
App({
  globalData: {
    // 作品列表在多个页面复用，进入小程序后只拉取一次
    animeList: null,
    // 首页快捷入口跳转到「番剧」Tab 时传递的筛选条件
    catalogFilter: null
  },

  onLaunch() {
    // 登录态由 utils/api.js 中的 storage 读写，页面 onShow 时自行刷新
  }
})
