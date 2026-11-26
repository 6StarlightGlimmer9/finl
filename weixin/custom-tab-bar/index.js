// 自定义底部导航：图标 + 文字（图标为 CSS 矢量绘制，无需字体文件或合法域名）
Component({
  data: {
    selected: 0,
    hidden: false,
    list: [
      { pagePath: '/pages/index/index', text: '首页', icon: 'ic-home' },
      { pagePath: '/pages/anime/index', text: '番剧', icon: 'ic-anime' },
      { pagePath: '/pages/reviews/index', text: '漫评', icon: 'ic-review' },
      { pagePath: '/pages/community/index', text: '社群', icon: 'ic-community' },
      { pagePath: '/pages/profile/index', text: '我的', icon: 'ic-profile' }
    ]
  },

  methods: {
    switchTab(e) {
      const dataset = e.currentTarget.dataset
      if (!dataset.path) return
      this.setData({ selected: Number(dataset.index) })
      wx.switchTab({ url: dataset.path })
    }
  }
})
