package com.dimension.screenroom.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServicePageController {
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return "<html><head><meta charset='UTF-8'><title>次元放映室 API</title><style>body{font-family:Arial,sans-serif;background:#fff6f8;color:#17151a;padding:48px}main{max-width:720px;margin:auto;background:white;padding:36px;border-left:5px solid #f25c86;box-shadow:0 12px 40px #e9dfe3}h1{margin-top:0}a{color:#e34d79}</style></head><body><main><h1>次元放映室 API 正常运行</h1><p>后端服务已连接数据库，前端页面请访问：</p><p><a href='http://localhost:5174/'>打开前端首页</a></p><p>可用接口：<a href='/api/anime'>番剧列表</a> · <a href='/api'>API 信息</a></p></main></body></html>";
    }
}