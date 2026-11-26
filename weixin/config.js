// 次元放映室 · 小程序服务地址配置
//
// 后端 SpringBoot 默认端口 8081，接口前缀 /api（无 context-path）
// 封面图 coverUrl 形如 /covers/1.jpg，由前端 Vite 的 public/covers 静态目录提供
//
// 1) 微信开发者工具模拟器：端口保持 8081 / 5174，localhost 可直接使用；
//    必须在「详情 → 本地设置」勾选「不校验合法域名、web-view、TLS 版本及 HTTPS 证书」
//    （project.config.json 与 project.private.config.json 中 urlCheck 均为 false）
//    localhost 不能登记为线上合法域名，本地研究只能关闭该校验
// 2) 真机预览：仅把 HOST 换成电脑的局域网 IP（如 http://192.168.1.8:8081），端口不变；
//    并让前端以 `npm run dev -- --host 0.0.0.0` 启动，否则图片无法加载；
//    也可在小程序「我的 → 连接设置」中直接填写地址，无需改代码
// 3) 正式发布：必须改为 HTTPS 域名，并在微信公众平台配置
//    request / image / downloadFile / web-view 合法域名
const HOST = 'http://localhost:8081'
const IMG_HOST = 'http://localhost:5174'

const API_BASE_KEY = 'screenroom-api-base'
const IMG_HOST_KEY = 'screenroom-img-host'
const DEFAULT_API_BASE = HOST + '/api'

function getBaseUrl() {
  try {
    return wx.getStorageSync(API_BASE_KEY) || DEFAULT_API_BASE
  } catch (e) {
    return DEFAULT_API_BASE
  }
}

function getImgHost() {
  try {
    return wx.getStorageSync(IMG_HOST_KEY) || IMG_HOST
  } catch (e) {
    return IMG_HOST
  }
}

function setServer(apiBase, imgHost) {
  wx.setStorageSync(API_BASE_KEY, String(apiBase || DEFAULT_API_BASE).replace(/\/+$/, ''))
  wx.setStorageSync(IMG_HOST_KEY, String(imgHost || IMG_HOST).replace(/\/+$/, ''))
}

function resetServer() {
  wx.removeStorageSync(API_BASE_KEY)
  wx.removeStorageSync(IMG_HOST_KEY)
}

module.exports = {
  DEFAULT_API_BASE,
  DEFAULT_IMG_HOST: IMG_HOST,
  PAGE_SIZE: 20,
  getBaseUrl,
  getImgHost,
  setServer,
  resetServer
}
