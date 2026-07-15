# 项目文档导航

这个目录包含对项目 HTTP 请求流程、Session 机制和 Spring Security 配置的详细解析。

---

## 📚 文档列表

### 1. **REQUEST_REDIRECT_FLOW.md** - HTTP 请求重定向流程
**问题**：为什么在浏览器输入 `http://localhost:8080/api/auth/login/?username=admin&password=1234` 后，会出现 302 → 200 → 200 的三步请求链？

**内容**：
- 现象观察和网络堆栈追踪
- 详细的原因分析（为什么会 302 重定向）
- 完整的流程图和时序图
- 浏览器的自动重定向行为
- Spring Security 的默认登录页面机制
- 最佳实践和错误示范
- 调试建议

**适合阅读**：初学者、需要理解 HTTP 重定向机制的人

---

### 2. **FORMLOGIN_CONFIGURATION.md** - Spring Security formLogin 配置详解
**问题**：formLogin 配置是如何工作的？为什么 GET 请求会被重定向？

**内容**：
- formLogin 的核心配置说明
- loginProcessingUrl 的含义
- 三种不同的请求情况分析
- Spring Security 内部的请求处理流程
- 代码位置和源码指向
- 配置对比（前后端分离 vs 传统单体应用）
- 最佳实践和常见错误
- 常见问题解答 (FAQ)

**适合阅读**：需要自定义 Spring Security 配置的人

---

### 3. **SESSION_MECHANISM.md** - 双 SessionID 工作机制
**问题**：为什么登录后会有两个 SessionID？一个在 request header，一个在 response header？

**内容**（位于 session-state/files 目录）：
- JSESSIONID 的生成和生命周期
- Remember-Me Token 的工作原理
- 数据库表结构和实现类详解
- 完整的登录流程图
- 关闭浏览器后的会话恢复机制
- 安全考虑和建议
- 故障排查指南

**适合阅读**：想理解会话管理机制的人

---

## 🔄 完整流程梳理

### 用户在浏览器输入 URL 时发生了什么？

```
用户输入 URL: http://localhost:8080/api/auth/login/?username=admin&password=1234
                                    ↓
                          [这是 GET 请求]
                                    ↓
┌─────────────────────────────────────────────────────────┐
│ Spring Security 检查                                      │
│  ├─ 这个 URL 是 loginProcessingUrl 吗？ YES            │
│  ├─ 请求方法是 POST 吗？ NO (是 GET)                  │
│  └─ 决定：不接受 GET，重定向到 /login                  │
└─────────────────────────────────────────────────────────┘
                                    ↓
                    [步骤 1] 返回 302 Found
                    响头：Location: /login
                    响头：Set-Cookie: JSESSIONID=xxx
                                    ↓
┌─────────────────────────────────────────────────────────┐
│ 浏览器行为                                                │
│  ├─ 接收 302 状态码                                      │
│  ├─ 读取 Location 响头                                   │
│  ├─ 自动发起新请求到 /login                              │
│  └─ 自动携带刚收到的 JSESSIONID Cookie                  │
└─────────────────────────────────────────────────────────┘
                                    ↓
                    [步骤 2] 返回 200 OK
                    响头：Content-Type: text/html
                    响体：<html>... 登录表单 ...</html>
                                    ↓
┌─────────────────────────────────────────────────────────┐
│ 浏览器渲染                                                │
│  ├─ 解析 HTML                                           │
│  ├─ 加载 CSS、JavaScript 等资源                         │
│  ├─ 可能触发额外的网络请求                               │
│  └─ 最终显示登录表单                                     │
└─────────────────────────────────────────────────────────┘
                                    ↓
        [可能步骤 3] 额外的 GET /login 请求
        （来自浏览器刷新、资源加载、脚本逻辑等）
```

**详见**：`REQUEST_REDIRECT_FLOW.md`

---

## ✅ 正确的登录方式

### 方式 A：前端 Vue.js + Axios（推荐）

```javascript
// frontend/src/net/index.ts
axios.post('/api/auth/login', 
    { username: 'admin', password: '1234' },
    { withCredentials: true }  // 自动发送/接收 Cookie
)
.then(response => {
    // JSESSIONID 已由浏览器自动保存
    // 后续请求自动携带
})
```

### 方式 B：cURL 命令行

```bash
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "username=admin&password=1234" \
     -c cookies.txt
```

### 方式 C：HTML 表单 + Spring 默认表单

直接在浏览器访问 `http://localhost:8080/login`，使用 Spring 生成的默认表单。

---

## ❌ 错误的方式（请勿使用）

```
❌ GET 请求：http://localhost:8080/api/auth/login?username=admin&password=1234
   原因：
   - 密码暴露在 URL 中（不安全！）
   - 会被保存在浏览器历史中
   - 会被记录在服务器日志中
   - 不符合 HTTP 最佳实践

✓ 应该用 POST 请求，在请求体中发送敏感信息
```

---

## 🔐 安全要点

### JSESSIONID (会话 Cookie)

| 属性 | 值 | 说明 |
|------|-----|------|
| **HttpOnly** | true | JavaScript 无法访问，防止 XSS 攻击 |
| **Secure** | true (HTTPS) | 仅在 HTTPS 连接中传输 |
| **SameSite** | Strict/Lax | 防止 CSRF 攻击 |
| **Expires** | 会话结束 | 关闭浏览器后失效 |

### Remember-Me Token

| 属性 | 特点 |
|------|------|
| **Series ID** | 相同设备上多次登录时保持不变 |
| **Token Value** | 每次使用时更新（防重放） |
| **Database** | 存储在 `persistent_logins` 表 |
| **Expiration** | 可以配置长期有效 |

### 密码传输

```
✓ 安全做法：
  ├─ 使用 HTTPS 加密连接
  ├─ POST 请求在 body 中发送
  ├─ 使用 BCryptPasswordEncoder 加密存储
  └─ 实施失败尝试限制

❌ 不安全：
  ├─ 使用 HTTP（明文传输）
  ├─ 在 URL 中传递密码
  ├─ 在日志中记录密码
  └─ 使用简单哈希（MD5、SHA1）
```

---

## 🛠️ 项目配置速查

### 后端配置位置

| 功能 | 文件位置 |
|------|---------|
| Spring Security 配置 | `backend/src/main/kotlin/com/ferryside/config/SecurityConfiguration.kt` |
| CORS 配置 | `backend/src/main/kotlin/com/ferryside/config/CorsConfig.kt` |
| 登录成功处理 | `backend/src/main/kotlin/com/ferryside/handler/LoginSuccessHandler.kt` |
| 登录失败处理 | `backend/src/main/kotlin/com/ferryside/handler/LoginFailureHandler.kt` |
| Remember-Me 实现 | `backend/src/main/kotlin/com/ferryside/service/impl/PersistentLoginServiceImpl.kt` |

### 前端配置位置

| 功能 | 文件位置 |
|------|---------|
| HTTP 请求配置 | `frontend/src/net/index.ts` |
| 路由配置 | `frontend/src/router/index.js` |
| 主入口 | `frontend/src/main.js` |

---

## 📊 关键概念速查表

### HTTP 状态码

| 状态码 | 名称 | 说明 |
|-------|------|------|
| **200** | OK | 请求成功 |
| **302** | Found | 临时重定向（浏览器自动跟随） |
| **400** | Bad Request | 请求参数错误 |
| **401** | Unauthorized | 未认证 |
| **403** | Forbidden | 已认证但无权限 |

### Cookie 属性

| 属性 | 说明 | 示例 |
|------|------|------|
| **Name** | Cookie 名称 | `JSESSIONID` |
| **Value** | Cookie 值 | `9E2D3DAACC9...` |
| **Path** | 生效路径 | `/` |
| **Domain** | 生效域名 | `localhost` |
| **Expires** | 过期时间 | `关闭浏览器时` |
| **HttpOnly** | 防 XSS | `true` |
| **Secure** | 仅 HTTPS | `true` |
| **SameSite** | 防 CSRF | `Strict/Lax` |

### Spring Security 过滤器链

```
HttpServletRequest
        ↓
[1] DefaultLoginPageGeneratingFilter  ← 处理 GET /login
        ↓
[2] UsernamePasswordAuthenticationFilter ← 处理 POST /api/auth/login
        ↓
[3] SecurityContextPersistenceFilter ← 管理 Session
        ↓
[4] AuthorizationFilter ← 检查权限
        ↓
      Controller
```

---

## 🐛 调试指南

### 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 登录后请求返回 401 | JSESSIONID 丢失/过期 | 检查 withCredentials 配置 |
| 跨域请求失败 | CORS 未配置 | 检查 CorsConfig.kt |
| Remember-Me 不工作 | 数据库表不存在 | 创建 persistent_logins 表 |
| 密码不匹配 | BCrypt 加密问题 | 检查密码存储和验证逻辑 |
| 浏览器无法保存 Cookie | Cookie 属性设置 | 检查 Secure、HttpOnly 等 |

### 调试工具

1. **浏览器开发者工具** (F12)
   - Network 标签：查看 HTTP 请求/响应
   - Storage → Cookies：查看已保存的 Cookie
   - Application：查看本地存储等

2. **后端日志**
   ```yaml
   logging:
     level:
       org.springframework.security: DEBUG
   ```

3. **Postman / Insomnia**
   - 测试 API 端点
   - 模拟不同的请求方式
   - 手动管理 Cookie

---

## 🚀 快速开始

### 1. 运行后端
```bash
cd backend
./gradlew bootRun
# 服务运行在 http://localhost:8080
```

### 2. 运行前端
```bash
cd frontend
npm install
npm run dev
# 开发服务运行在 http://localhost:5173
```

### 3. 测试登录
```javascript
// 在浏览器控制台运行
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  credentials: 'include',  // 等价于 withCredentials: true
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  },
  body: 'username=admin&password=1234'
})
.then(r => r.json())
.then(d => console.log(d))
```

---

## 📖 深入学习资源

### Spring Security
- [官方文档 - Form Login](https://docs.spring.io/spring-security/site/docs/current/reference/html5/#servlet-authentication-form)
- [官方文档 - Remember Me](https://docs.spring.io/spring-security/site/docs/current/reference/html5/#servlet-authentication-rememberme)
- [Spring Security 源码](https://github.com/spring-projects/spring-security)

### HTTP 和 Cookie
- [RFC 7231 - HTTP/1.1](https://tools.ietf.org/html/rfc7231)
- [RFC 6265 - HTTP State Management Mechanism](https://tools.ietf.org/html/rfc6265)
- [MDN - HTTP Cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)

### 安全最佳实践
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)

---

## 📝 文档修改历史

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-07-15 | 1.0 | 初版：添加 3 份详细文档 |

---

## ❓ 常见问题快速链接

- [为什么会出现 302 重定向？](REQUEST_REDIRECT_FLOW.md#原因分析)
- [formLogin 的工作原理？](FORMLOGIN_CONFIGURATION.md#formlogin-的核心要点)
- [双 SessionID 的作用？](SESSION_MECHANISM.md#完整登录流程图)
- [如何正确登录？](REQUEST_REDIRECT_FLOW.md#解决方案和最佳实践)
- [安全性如何保证？](SESSION_MECHANISM.md#安全考虑)

---

**快速导航**：
- 👉 [REQUEST_REDIRECT_FLOW.md](REQUEST_REDIRECT_FLOW.md) - 了解 HTTP 重定向流程
- 👉 [FORMLOGIN_CONFIGURATION.md](FORMLOGIN_CONFIGURATION.md) - 了解 Spring Security 配置
- 👉 [SESSION_MECHANISM.md](../../../.copilot/session-state/5743900f-8471-43d0-9678-602c596704ea/files/SESSION_MECHANISM.md) - 了解会话管理机制
