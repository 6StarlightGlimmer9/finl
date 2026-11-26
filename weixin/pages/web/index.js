// pages/web/index.js · Bilibili PV 预览
// 注意：web-view 需要在微信公众平台配置业务域名，个人主体小程序不可使用；
// 未配置时详情页会自动降级为复制预览链接。
Page({
  data: { url: '' },

  onLoad(options) {
    let url = (options && options.url) || ''
    let title = (options && options.title) || '番剧预览'
    try { url = decodeURIComponent(url) } catch (e) { /* 保留原值 */ }
    try { title = decodeURIComponent(title) } catch (e) { /* 保留原值 */ }
    this.setData({ url: url })
    wx.setNavigationBarTitle({ title: title })
  }
})
