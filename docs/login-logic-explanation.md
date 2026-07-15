# 前后端登录逻辑详解 / Frontend & Backend Login Logic Explanation

---

## 目录 / Table of Contents

1. [整体架构概览 / Architecture Overview](#1-整体架构概览--architecture-overview)
2. [JSESSIONID 工作原理 / How JSESSIONID Works](#2-jsessionid-工作原理--how-jsessionid-works)
3. [后端登录流程 / Backend Login Flow](#3-后端登录流程--backend-login-flow)
4. [前端登录流程 / Frontend Login Flow](#4-前端登录流程--frontend-login-flow)
5. [Remember Me 机制 / Remember Me Mechanism](#5-remember-me-机制--remember-me-mechanism)
6. [邮件验证码注册流程 / Email Verification Registration Flow](#6-邮件验证码注册流程--email-verification-registration-flow)
7. [跨域与 Cookie 传输 / CORS & Cookie Transmission](#7-跨域与-cookie-传输--cors--cookie-transmission)
8. [完整请求序列图 / Complete Request Sequence](#8-完整请求序列图--complete-request-sequence)

---

## 1. 整体架构概览 / Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│  Frontend (Vue 3 + Element Plus)                         │
│  localhost:5173                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ TheWelcome   │  │ RegisterPage │  │  AboutView     │  │
│  │ (Login Page) │  │ (Register)   │  │  (Post-login)  │  │
│  └──────┬───────┘  └──────┬───────┘  └───────┬────────┘  │
│         │                 │                   │           │
│         └─────────┬───────┘                   │           │
│                   │                           │           │
│              axios + withCredentials          │           │
│              (application/x-www-form-urlencoded)          │
└───────────────────┼───────────────────────────┼───────────┘
                    │                           │
                    ▼                           ▼
┌──────────────────────────────────────────────────────────┐
│  Backend (Spring Boot 4.1 + Spring Security)             │
│  localhost:8080                                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │              SecurityFilterChain                  │    │
│  │  ┌────────────┐  ┌──────────┐  ┌─────────────┐  │    │
│  │  │ CsrfFilter │  │ CorsFilter│  │FormLoginFilter│  │    │
│  │  │ (disabled) │  │          │  │             │  │    │
│  │  └────────────┘  └──────────┘  └─────────────┘  │    │
│  │  ┌─────────────────┐  ┌──────────────────────┐  │    │
│  │  │ RememberMeFilter│  │ SessionManagement   │  │    │
│  │  └─────────────────┘  └──────────────────────┘  │    │
│  └──────────────────────────────────────────────────┘    │
│                                                          │
│  ┌──────────────────┐  ┌────────────────────────────┐   │
│  │ AuthorizeService  │  │ UsersMapper (MyBatis-Plus) │   │
│  │ Controller        │  │ → MySQL (users table)      │   │
│  └──────────────────┘  └────────────────────────────┘   │
│  ┌──────────────────┐  ┌────────────────────────────┐   │
│  │ StringRedisTemplate│  │ MailSender (QQ SMTP)       │   │
│  │ → Redis            │  │ → email verification code  │   │
│  └──────────────────┘  └────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

| 组件 / Component | 技术栈 / Tech Stack | 说明 / Purpose |
|---|---|---|
| 前端框架 / Frontend | Vue 3 + Vite + TypeScript | SPA 应用, hash 路由 |
| UI 组件库 / UI Library | Element Plus 2.14 | 表单、按钮、消息提示 |
| HTTP 客户端 / HTTP Client | Axios (withCredentials) | 自动携带 Cookie |
| 后端框架 / Backend | Spring Boot 4.1 + Kotlin | REST API 服务 |
| 安全框架 / Security | Spring Security 6 | 认证、授权、Session 管理 |
| 数据库 / Database | MySQL (MyBatis-Plus ORM) | 用户持久化存储 |
| 缓存 / Cache | Redis | 验证码临时存储 (3分钟过期) |
| 邮件 / Email | Spring Mail (QQ SMTP) | 发送验证码 |

---

## 2. JSESSIONID 工作原理 / How JSESSIONID Works

### 2.1 什么是 JSESSIONID? / What is JSESSIONID?

**中文：**
JSESSIONID 是 Java Web 应用（Tomcat/Jetty/Servlet 容器）中用于标识用户会话 (Session) 的唯一标识符。Spring Security 基于 Servlet 的 `HttpSession` 来维持用户的登录状态。

当用户第一次访问服务器时，服务器会创建一个新的 `HttpSession` 对象，并生成一个唯一的 JSESSIONID。这个 JSESSIONID 通过 `Set-Cookie` 响应头发送给浏览器。浏览器在后续的每个请求中都会自动携带这个 Cookie，服务器通过它查找对应的 Session，从而识别用户身份。

**English:**
JSESSIONID is a unique identifier used in Java web applications (Tomcat/Jetty/Servlet containers) to identify a user's session. Spring Security relies on the Servlet `HttpSession` to maintain a user's login state.

When a user first accesses the server, the server creates a new `HttpSession` object and generates a unique JSESSIONID. This JSESSIONID is sent to the browser via the `Set-Cookie` response header. The browser automatically includes this Cookie in all subsequent requests, allowing the server to look up the corresponding Session and identify the user.

### 2.2 生命周期 / Lifecycle

```
时间线 / Timeline
────────────────────────────────────────────────────────────►

[首次请求]                    [登录成功]                  [登出/过期]
First Request               Login Success               Logout/Expiry

    │                            │                           │
    ▼                            ▼                           ▼
┌─────────┐               ┌───────────┐              ┌──────────┐
│ 浏览器   │  GET /        │  POST     │              │  POST     │
│ Browser │──────────────►│  /api/    │─────────────►│  /api/    │
│         │◄──────────────│  auth/    │◄─────────────│  auth/    │
│         │ Set-Cookie:   │  login    │ Set-Cookie:  │  logout   │
│         │ JSESSIONID=   │           │ JSESSIONID   │           │
│         │ ABC123        │           │ (same, now   │ Session   │
│         │               │           │  contains    │ destroyed │
│         │ 空 Session     │           │  auth info)  │           │
└─────────┘               └───────────┘              └──────────┘

服务端 Session 状态 / Server-side Session State:
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│ Session ID:  │     │ Session ID:     │     │ 已销毁        │
│ ABC123       │     │ ABC123          │     │ Destroyed    │
│ isAuth: false│ ──► │ isAuth: true    │ ──► │              │
│ user: null   │     │ user: ferry     │     │              │
└──────────────┘     └─────────────────┘     └──────────────┘
```

### 2.3 Cookie 属性详解 / Cookie Attributes

```
Set-Cookie: JSESSIONID=A1B2C3D4E5F6;
            Path=/;
            HttpOnly;
            Secure (仅 HTTPS);
            SameSite=Lax
```

| 属性 / Attribute | 值 / Value | 说明 / Explanation |
|---|---|---|
| **Name** | `JSESSIONID` | Servlet 容器的默认 Session Cookie 名称 |
| **Value** | 随机生成的 32 字符哈希 | 不可猜测，防 Session 劫持 |
| **Path** | `/` | Cookie 对所有路径生效 |
| **HttpOnly** | `true` | JavaScript 无法通过 `document.cookie` 读取，防止 XSS 攻击窃取 Session |
| **Secure** | `true` (HTTPS 时) | 仅通过 HTTPS 传输，防止中间人攻击 |
| **SameSite** | `Lax` (Spring Security 默认) | 防止 CSRF 攻击中跨站携带 Cookie |
| **Max-Age** | 未设置 (Session Cookie) | 浏览器关闭即删除（会话级 Cookie） |

### 2.4 Spring Security 中的 Session 认证流程 / Session Authentication in Spring Security

**中文：**

1. **用户登录** → `UsernamePasswordAuthenticationFilter` 验证用户名密码
2. **认证成功** → `SecurityContextHolder` 将 `Authentication` 对象存入当前线程
3. **Session 持久化** → `SecurityContextPersistenceFilter` 将 `SecurityContext` 写入 `HttpSession`
4. **返回 JSESSIONID** → 服务器通过 `Set-Cookie` 响应头将 JSESSIONID 返回给浏览器
5. **后续请求** → `SecurityContextPersistenceFilter` 从请求的 Cookie 中读取 JSESSIONID，找到对应 Session，恢复 `SecurityContext`
6. **身份识别完成** → Spring Security 知道"这个请求来自已登录用户 ferry"

**English:**

1. **User Login** → `UsernamePasswordAuthenticationFilter` validates username/password
2. **Auth Success** → `SecurityContextHolder` stores `Authentication` in the current thread
3. **Session Persistence** → `SecurityContextPersistenceFilter` writes `SecurityContext` to `HttpSession`
4. **Return JSESSIONID** → Server sends JSESSIONID to browser via `Set-Cookie` response header
5. **Subsequent Requests** → `SecurityContextPersistenceFilter` reads JSESSIONID from request Cookie, finds matching Session, restores `SecurityContext`
6. **Identity Complete** → Spring Security knows "this request comes from logged-in user ferry"

### 2.5 Session 有效期与并发 / Session Timeout & Concurrency

```kotlin
// 默认配置 / Default configuration (可在 application.yaml 中覆盖)
// server.servlet.session.timeout = 30m  (默认 30 分钟 / default 30 minutes)
// server.servlet.session.cookie.name = JSESSIONID
```

| 场景 / Scenario | 行为 / Behavior |
|---|---|
| 用户 30 分钟无操作 | Session 过期，下次请求需要重新登录 |
| 同一个浏览器多 Tab | 共享同一个 JSESSIONID (同一个 Session) |
| 不同浏览器 | 各自有独立的 JSESSIONID (不同 Session) |
| 服务器重启 | 所有 Session 丢失（默认内存存储），用户需重新登录 |
| 浏览器关闭再打开 | Session Cookie 被清除，但 Remember-Me Cookie 仍存在 |

### 2.6 JSESSIONID 的安全性 / Security of JSESSIONID

| 威胁 / Threat | 防护措施 / Protection |
|---|---|
| **Session 劫持 (Session Hijacking)** | HttpOnly 防止 JS 读取; Secure 防止 HTTP 明文传输; 值不可猜测 |
| **Session 固定 (Session Fixation)** | Spring Security 默认在登录成功后**更换** JSESSIONID，防止攻击者预设 Session ID |
| **CSRF 攻击** | 本项目禁用了 CSRF 保护（CSRF disabled），适合纯 API 场景 |
| **XSS 窃取 Cookie** | HttpOnly + 前端输入过滤 |
| **重放攻击** | HTTPS 加密传输 |

---

## 3. 后端登录流程 / Backend Login Flow

### 3.1 核心类关系图 / Core Class Diagram

```
                    ┌─────────────────────────┐
                    │  SecurityConfiguration   │  ← 安全配置入口
                    │  (SecurityFilterChain)   │
                    └───────────┬─────────────┘
                                │ 配置 / configures
            ┌───────────────────┼───────────────────┐
            ▼                   ▼                   ▼
┌───────────────────┐ ┌─────────────────┐ ┌─────────────────────┐
│ AuthorizeService  │ │ CorsConfig      │ │ PersistentLogin     │
│ Controller        │ │ (CORS 跨域)     │ │ Service             │
│ /api/auth/**      │ │                 │ │ (Remember Me Token) │
└────────┬──────────┘ └─────────────────┘ └─────────────────────┘
         │ 调用 / calls
         ▼
┌───────────────────┐
│ AuthorizeService  │  ← 接口 / Interface
│ (extends          │
│  UserDetailsSvc)  │
└────────┬──────────┘
         │ 实现 / implements
         ▼
┌───────────────────────────────┐
│ AuthorizeServiceImpl          │
│  - loadUserByUsername()       │  ← Spring Security 调用
│  - sendValidatedEmail()       │  ← 发送验证码
└──────────┬────────────────────┘
           │ 依赖 / depends on
           ▼
┌───────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│ UserService       │   │ StringRedisTempl  │   │ MailSender       │
│ (MyBatis-Plus)    │   │ (Redis 缓存)      │   │ (邮件发送)        │
└───────────────────┘   └──────────────────┘   └──────────────────┘
```

### 3.2 SecurityConfiguration — 安全规则配置 / Security Rules

```kotlin
// 文件位置 / File: config/SecurityConfiguration.kt

@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // === 授权规则 / Authorization Rules ===
            authorizeHttpRequests {
                authorize("/api/auth/login", permitAll)    // 登录接口公开
                authorize("/api/auth/**", permitAll)       // 所有 auth 接口公开
                authorize(anyRequest, authenticated)       // 其他接口需认证
            }

            // === 表单登录配置 / Form Login Config ===
            formLogin {
                loginProcessingUrl = "/api/auth/login"     // 登录处理 URL
                authenticationSuccessHandler = successHandler  // 成功处理器
                authenticationFailureHandler = failureHandler  // 失败处理器
            }

            // === Remember Me 配置 ===
            rememberMe {
                rememberMeParameter = "remember"            // 前端参数名
                rememberMeCookieName = "my_custom_remember_me_cookie"
                tokenRepository = persistentLoginService   // 数据库存储 Token
            }

            // === 登出配置 / Logout Config ===
            logout {
                logoutUrl = "/api/auth/logout"             // 登出 URL
            }

            // === CSRF 禁用 (API 场景) ===
            csrf { disable() }

            // === CORS 配置 ===
            cors { configurationSource = corsSource }
        }
    }

    // === 密码编码器 / Password Encoder ===
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
```

### 3.3 登录请求处理流程 / Login Request Processing Flow

```
Step 1: 前端发起请求 / Frontend sends request
   POST /api/auth/login
   Content-Type: application/x-www-form-urlencoded
   Body: username=ferry&password=123456&remember=true

Step 2: UsernamePasswordAuthenticationFilter 拦截
   ├── 从请求中提取 username 和 password
   └── 创建 UsernamePasswordAuthenticationToken (未认证状态)

Step 3: AuthenticationManager → AuthenticationProvider
   └── 调用 AuthorizeServiceImpl.loadUserByUsername("ferry")
       ├── 查询数据库: userService.findAccountByUsernameOrEmail("ferry")
       │   └── SQL: SELECT * FROM users WHERE username='ferry' OR email='ferry'
       ├── 如果找不到 → throw UsernameNotFoundException
       └── 如果找到 → 返回 User("ferry", "$2a$10$...", [])
           └── BCrypt 密码会在此步骤之后由 DaoAuthenticationProvider 自动比对

Step 4: 密码验证 / Password Verification
   ├── BCryptPasswordEncoder.matches(明文密码, 数据库密文)
   ├── 匹配成功 → 创建已认证的 Authentication 对象
   └── 匹配失败 → 调用 LoginFailureHandler

Step 5: 认证成功 → SecurityContext 存入 Session
   ├── SecurityContextPersistenceFilter 将认证信息写入 HttpSession
   ├── 触发 SessionFixationProtection: 更换新的 JSESSIONID
   └── 调用 LoginSuccessHandler.onAuthenticationSuccess()
       └── 返回 JSON: {"status":200, "success":true, "message":"login success"}

Step 6: 浏览器收到响应
   ├── Set-Cookie: JSESSIONID=<NEW_SESSION_ID>; Path=/; HttpOnly
   ├── Set-Cookie: my_custom_remember_me_cookie=<token>; Max-Age=1209600 (如果勾选remember)
   └── Response Body: {"status":200, "success":true, "message":"login success"}
```

### 3.4 关键代码解析 / Key Code Analysis

#### UserDetailsService 实现 / UserDetailsService Implementation

```kotlin
// 文件 / File: service/impl/AuthorizeServiceImpl.kt

override fun loadUserByUsername(username: String): UserDetails {
    // 按用户名或邮箱查找用户
    val user = userService.findAccountByUsernameOrEmail(username)
        ?: throw UsernameNotFoundException("User not found")

    return User(
        user.username,     // Spring Security 的 User 对象
        user.password,     // BCrypt 加密后的密码
        listOf()           // 权限列表（当前为空，可扩展 ROLE_USER）
    )
}
```

**中文：** 这个方法是 Spring Security 认证流程的核心。当用户提交登录表单时，Spring Security 自动调用它来加载用户信息。`User` (带 Uppercase U) 是 Spring Security 的内置 `UserDetails` 实现。第三个参数 `listOf()` 是权限列表（GrantedAuthority），当前为空，后续可以添加 `SimpleGrantedAuthority("ROLE_USER")` 来实现角色控制。

**English:** This method is the core of Spring Security's authentication flow. When a user submits the login form, Spring Security automatically calls this to load user information. `User` (with uppercase U) is Spring Security's built-in `UserDetails` implementation. The third parameter `listOf()` is the authority list (GrantedAuthority), currently empty; you can add `SimpleGrantedAuthority("ROLE_USER")` later for role-based access control.

#### 登录成功/失败处理器 / Success/Failure Handlers

```kotlin
// 登录成功 / Login Success: handler/LoginSuccessHandler.kt
override fun onAuthenticationSuccess(..., authentication: Authentication) {
    response.characterEncoding = Charsets.UTF_8.name()  // 设置 UTF-8 编码
    response.writer.write(JsonUtil.toJson(RestBean.success("login success")))
    // 返回统一的 JSON 格式响应
}
```

```kotlin
// 登录失败 / Login Failure: handler/LoginFailureHandler.kt
override fun onAuthenticationFailure(..., exception: AuthenticationException) {
    response.characterEncoding = Charsets.UTF_8.name()
    response.writer.write(JsonUtil.toJson(RestBean.failure(401, exception.message)))
    // 返回 401 状态码和错误信息
}
```

### 3.5 登出流程 / Logout Flow

```
Step 1: POST /api/auth/logout
Step 2: LogoutFilter 拦截请求
Step 3: 执行以下操作:
   ├── SecurityContextHolder.clearContext()  → 清除当前线程的认证信息
   ├── HttpSession.invalidate()              → 销毁 Session (JSESSIONID 失效)
   ├── SecurityContextRepository 清理
   ├── Remember-Me Cookie 清除
   └── 重定向或返回（本项目返回空响应）
Step 4: 浏览器端的 JSESSIONID Cookie 仍然存在但服务端 Session 已销毁
        → 下次请求时服务器找不到 Session → 返回 401 或重定向到登录页
```

---

## 4. 前端登录流程 / Frontend Login Flow

### 4.1 登录页面 — TheWelcome.vue

```typescript
// 文件 / File: components/TheWelcome.vue

const form = reactive({
  username: '',      // 用户输入的用户名或邮箱
  password: '',      // 密码
  remember: false,   // 是否记住我
})

const login = () => {
  if (form.username && form.username.length > 0) {
    post('/api/auth/login', {
      username: form.username,
      password: form.password,
      remember: form.remember,
    }, (message) => {
      ElMessage.success(message)    // 显示 "login success"
      router.push('/index')         // 跳转到已登录页面
    })
  } else {
    ElMessage.warning("Please enter username and password")
  }
}
```

### 4.2 HTTP 请求工具 — net/index.ts

```typescript
// 文件 / File: net/index.ts

import axios from 'axios'

function post(url: string, data: object,
              success: (message: string, status: number) => void,
              failure = defaultFailure,
              err = defaultError) {
    axios.post(url, data, {
        headers: {
            // 关键: 以表单格式发送数据（Spring Security 表单登录的要求）
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        },
        // 关键: 允许跨域携带 Cookie (JSESSIONID + Remember-Me)
        withCredentials: true
    }).then(({data}) => {
        if (data.success) {
            success(data.message, data.status)  // 登录成功回调
        } else {
            failure()                            // 业务失败回调
        }
    }).catch(err)                                // 网络异常回调
}
```

**`withCredentials: true` 的重要性 / The Importance of `withCredentials`:**

这个配置是前后端分离架构中**最容易踩的坑**：
- 设为 `true` 时，Axios 会在跨域请求中携带 Cookie（JSESSIONID）
- 设为 `false`（默认值）时，跨域请求不会携带 Cookie → 服务器每次请求都创建新 Session → 永远无法保持登录状态
- 后端必须配合设置 CORS：`allowCredentials = true`（不可用 `allowedOrigins("*")`，必须用 `allowedOriginPatterns("*")`）

This is the **most common pitfall** in separated frontend-backend architectures:
- When set to `true`, Axios includes Cookies (JSESSIONID) in cross-origin requests
- When set to `false` (default), cross-origin requests don't include Cookies → server creates a new Session on every request → login state can never be maintained
- Backend must cooperate with CORS: `allowCredentials = true` (cannot use `allowedOrigins("*")`, must use `allowedOriginPatterns("*")`)

### 4.3 前端完整登录时序 / Frontend Login Sequence

```
用户输入用户名密码 → 点击 Login 按钮
        │
        ▼
TheWelcome.vue: login()
  └── post('/api/auth/login', { username, password, remember })
        │
        ▼
net/index.ts: axios.post()
  │ Content-Type: application/x-www-form-urlencoded
  │ withCredentials: true
  │ Cookie: (如果有 JSESSIONID 则自动携带)
        │
        ▼
═══════════════════════════════════════
          网络传输 / Network
═══════════════════════════════════════
        │
        ▼
后端处理 / Backend Processing (参见 3.3 节)
        │
        ▼
═══════════════════════════════════════
          响应 / Response
═══════════════════════════════════════
        │
        ▼
axios 收到响应:
  ├── Set-Cookie: JSESSIONID=...  → 浏览器自动存储
  ├── Set-Cookie: my_custom_remember_me_cookie=... → 浏览器自动存储 (如勾选)
  └── Body: {"success": true, "message": "login success"}
        │
        ▼
net/index.ts: then() 回调
  │ data.success === true
  │ success(message, status) 被调用
        │
        ▼
TheWelcome.vue: 回调函数
  ├── ElMessage.success("login success") → 显示绿色提示
  └── router.push('/index') → 跳转到 AboutView (已登录页面)
```

---

## 5. Remember Me 机制 / Remember Me Mechanism

### 5.1 工作原理 / How It Works

**中文：**
"记住我" 功能允许用户在关闭浏览器后仍然保持登录状态。它与 JSESSIONID 的 Session 机制完全不同：

| 特性 | JSESSIONID (Session) | Remember-Me Token |
|---|---|---|
| 存储位置 | 服务器内存 | 数据库 `persistent_logins` 表 |
| 生命周期 | 浏览器关闭即失效 | 默认 2 周 (可配置) |
| Cookie 类型 | Session Cookie (无 Max-Age) | Persistent Cookie (有 Max-Age) |
| 认证方式 | Session ID → Session → User | Remember-Me Token → 数据库查询 → User |

**English:**
The "Remember Me" feature allows users to stay logged in even after closing the browser. It's completely different from the JSESSIONID Session mechanism:

| Feature | JSESSIONID (Session) | Remember-Me Token |
|---|---|---|
| Storage | Server memory | Database `persistent_logins` table |
| Lifetime | Lost on browser close | Default 2 weeks (configurable) |
| Cookie Type | Session Cookie (no Max-Age) | Persistent Cookie (has Max-Age) |
| Authentication | Session ID → Session → User | Remember-Me Token → DB lookup → User |

### 5.2 Remember-Me 认证流程 / Remember-Me Authentication Flow

```
Step 1: 用户登录时勾选 "Remember Me"
        POST /api/auth/login
        username=ferry&password=123456&remember=true
            │
            ▼
Step 2: 登录成功后, RememberMeServices.onLoginSuccess()
        ├── 生成唯一的 series 标识符
        ├── 生成随机的 token 值
        ├── 存入数据库:
        │   INSERT INTO persistent_logins (series, username, token, last_used)
        │   VALUES ('abc123', 'ferry', 'token_xyz', NOW())
        └── 设置 Cookie:
            Set-Cookie: my_custom_remember_me_cookie=YWJjMTIzOnRva2VuX3h5eg==
                        Max-Age=1209600; Path=/; HttpOnly

Step 3: 用户关闭浏览器 → JSESSIONID 丢失
        但 Remember-Me Cookie 仍在

Step 4: 用户再次打开网站 → 自动认证
        浏览器发送 Cookie: my_custom_remember_me_cookie=YWJjMTIzOnRva2VuX3h5eg==
            │
            ▼
Step 5: RememberMeAuthenticationFilter 拦截
        ├── Base64 解码 Cookie → series="abc123", token="token_xyz"
        ├── 查询数据库: SELECT * FROM persistent_logins WHERE series='abc123'
        ├── 比对 token 是否匹配
        ├── 匹配成功 → 自动创建 Authentication 对象
        ├── 更新数据库中的 token (每次使用后刷新, 防 token 被盗)
        │   UPDATE persistent_logins SET token='new_token', last_used=NOW()
        └── 设置新的 Cookie:
            Set-Cookie: my_custom_remember_me_cookie=<new_base64>
```

### 5.3 数据库表结构 / Database Table Structure

```sql
-- persistent_logins 表
CREATE TABLE persistent_logins (
    series    VARCHAR(64)  NOT NULL PRIMARY KEY,  -- 唯一标识 (主键)
    username  VARCHAR(64)  NOT NULL,              -- 用户名
    token     VARCHAR(64)  NOT NULL,              -- 认证令牌
    last_used TIMESTAMP    NOT NULL               -- 最后使用时间
);
```

| 字段 | 说明 / Explanation |
|---|---|
| `series` | 系列标识符，唯一标识一个 Remember-Me 会话。仅在用户主动登出时删除 |
| `username` | 关联的用户名 |
| `token` | 每次自动认证后**刷新**（旋转策略），旧 token 被盗也无法使用 |
| `last_used` | 记录最后使用时间，可用于清理过期 token |

### 5.4 后端实现 / Backend Implementation

```kotlin
// PersistentLoginServiceImpl 实现了 PersistentTokenRepository 接口
// 文件 / File: service/impl/PersistentLoginServiceImpl.kt

@Service
class PersistentLoginServiceImpl : PersistentLoginService,
    ServiceImpl<PersistentLoginMapper, PersistentLogin>() {

    // 登录成功后创建新 Token
    override fun createNewToken(token: PersistentRememberMeToken) {
        save(PersistentLogin(
            series = token.series,
            username = token.username,
            token = token.tokenValue,
            lastUsed = token.date.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
        ))
    }

    // 每次自动认证时更新 Token（防盗窃）
    override fun updateToken(series: String, tokenValue: String, lastUsed: Date) {
        update(KtUpdateWrapper(PersistentLogin::class.java)
            .eq(PersistentLogin::series, series)
            .set(PersistentLogin::token, tokenValue)   // 刷新 token
            .set(PersistentLogin::lastUsed, lastUsed)  // 更新时间
        )
    }

    // 根据 series 查找 Token（自动认证时调用）
    override fun getTokenForSeries(seriesId: String): PersistentRememberMeToken? {
        val entity = getOne(KtQueryWrapper(PersistentLogin::class.java)
            .eq(PersistentLogin::series, seriesId)) ?: return null
        return PersistentRememberMeToken(entity.username, entity.series,
            entity.token, /* 转换 lastUsed */)
    }

    // 用户登出时删除所有 Token
    override fun removeUserTokens(username: String) {
        remove(KtQueryWrapper(PersistentLogin::class.java)
            .eq(PersistentLogin::username, username))
    }
}
```

---

## 6. 邮件验证码注册流程 / Email Verification Registration Flow

### 6.1 流程概览 / Flow Overview

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Register │     │  发送验证码 │     │  验证邮箱  │     │  创建用户  │
│  Page     │     │  Send Code│     │  Verify   │     │  Create   │
│  (前端)    │     │  (后端)    │     │  (后端)    │     │  User     │
└─────┬─────┘     └─────┬─────┘     └─────┬─────┘     └─────┬─────┘
      │                 │                 │                   │
      │ 1. 输入邮箱      │                 │                   │
      │────────────────►│                 │                   │
      │                 │                 │                   │
      │ 2. POST /api/auth/valid-email    │                   │
      │    email=user@example.com        │                   │
      │────────────────►│                 │                   │
      │                 │                 │                   │
      │                 │ 3. 生成 6 位验证码│                   │
      │                 │    存入 Redis    │                   │
      │                 │    key: email:   │                   │
      │                 │    {sessionId}:  │                   │
      │                 │    {email}       │                   │
      │                 │    TTL: 3 min    │                   │
      │                 │                 │                   │
      │                 │ 4. 发送邮件      │                   │
      │                 │    SMTP (QQ)    │                   │
      │                 │                 │                   │
      │ 5. 显示"发送成功"  │                 │                   │
      │◄────────────────│                 │                   │
      │                 │                 │                   │
      │ 6. 用户输入验证码 + 用户名 + 密码   │                   │
      │                 │                 │                   │
      │ 7. POST /api/auth/register       │                   │
      │    username=ferry&password=xxx&   │                   │
      │    email=user@example.com&code=123456               │
      │─────────────────────────────────►│                   │
      │                 │                 │                   │
      │                 │                 │ 8. 从 Redis 取验证码│
      │                 │                 │    比对是否正确     │
      │                 │                 │                   │
      │                 │                 │ 9. 验证通过        │
      │                 │                 │    BCrypt 加密密码  │
      │                 │                 │    INSERT INTO users│
      │                 │                 │    DELETE Redis key │
      │                 │                 │                   │
      │ 10. 显示"注册成功", 跳转到登录页   │                   │
      │◄─────────────────────────────────│                   │
```

### 6.2 验证码存储策略 / Verification Code Storage Strategy

```kotlin
// 文件 / File: service/impl/AuthorizeServiceImpl.kt

override fun sendValidatedEmail(email: String, session: HttpSession): Boolean {
    // Redis Key 格式: "email: {sessionId}: {email}"
    val key = "email: ${session.id}: $email"

    // 频率限制: 如果验证码还存在且剩余时间 > 120 秒, 不允许重复发送
    if (template.hasKey(key)) {
        val expire = template.getExpire(key, TimeUnit.SECONDS)
        if (expire > 120) return false  // 至少等 60 秒后才能重发
    }

    // 生成 6 位随机验证码
    val randomCode = Random.nextInt(100000, 1000000)  // 100000 ~ 999999

    // 存入 Redis, 3 分钟过期
    template.opsForValue().set(key, randomCode.toString(), Duration.ofMinutes(3))

    // 发送邮件
    mailSender.send(SimpleMailMessage().apply {
        from = sendMail
        setTo(email)
        subject = "Verification Code"
        text = "Your verification code is $randomCode"
    })
    return true
}
```

**Redis 键设计 / Redis Key Design:**

```
Key:   email:ABC123SESSIONID:user@example.com
Value: 456789
TTL:   180 seconds (3 minutes)

过期策略 / Expiry Strategy:
┌────────────┐    ┌──────────────────────────────────────┐
│ 0-60 秒     │    │ 可重发 / can resend                   │
│ 60-120 秒   │    │ 可重发 / can resend                   │
│ 120-180 秒  │    │ 禁止重发 (需等过期) / blocked (wait)   │
│ >180 秒     │    │ Key 自动删除 / key auto-deleted       │
└────────────┘    └──────────────────────────────────────┘
```

---

## 7. 跨域与 Cookie 传输 / CORS & Cookie Transmission

### 7.1 为什么需要跨域配置? / Why Is CORS Needed?

```
前端开发服务器 / Frontend Dev Server   后端服务器 / Backend Server
     localhost:5173                         localhost:8080
          │                                       │
          │  ← 不同端口 = 跨域请求 (Cross-Origin) → │
          │     (Different ports = cross-origin)    │
```

### 7.2 后端 CORS 配置 / Backend CORS Config

```kotlin
// 文件 / File: config/CorsConfig.kt

@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val cors = CorsConfiguration().apply {
        addAllowedOriginPattern("*")   // 允许所有来源 (不能用 allowedOrigins("*") + credentials)
        allowCredentials = true        // ★ 关键: 允许携带 Cookie
        addAllowedHeader("*")          // 允许所有请求头
        addAllowedMethod("*")          // 允许所有 HTTP 方法
        addExposedHeader("*")          // 暴露所有响应头
    }
    val source = UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration("/**", cors)
    }
    return source
}
```

**中文：**
关键配置解释：
- `addAllowedOriginPattern("*")` — Spring Security 的规则：当 `allowCredentials = true` 时，不能用 `allowedOrigins("*")`，因为那会允许任意网站携带用户凭证。必须用 `allowedOriginPatterns("*")` 作为宽松配置，或者显式列出信任的域名。
- `allowCredentials = true` — 允许浏览器在跨域请求中携带 Cookie、Authorization Header 等凭证信息。

**English:**
Key configuration explained:
- `addAllowedOriginPattern("*")` — Spring Security rule: when `allowCredentials = true`, you cannot use `allowedOrigins("*")` because that would allow any website to carry user credentials. You must use `allowedOriginPatterns("*")` as a relaxed config, or explicitly list trusted domains.
- `allowCredentials = true` — allows the browser to carry credentials (Cookies, Authorization Headers) in cross-origin requests.

### 7.3 完整的 Cookie 传输条件 / Full Cookie Transmission Requirements

要使 Cookie (特别是 JSESSIONID) 在前后端分离架构中正常传输，**三个条件必须同时满足**：

For Cookies (especially JSESSIONID) to transmit normally in separated frontend-backend architecture, **all three conditions must be met simultaneously**:

```
┌─────────────────────────────────────────────────────────────────┐
│  条件 1 / Condition 1: 后端 CORS (Backend CORS)                 │
│  allowCredentials = true  +  allowedOriginPatterns (非 "*")     │
├─────────────────────────────────────────────────────────────────┤
│  条件 2 / Condition 2: 前端 Axios (Frontend Axios)              │
│  withCredentials = true                                         │
├─────────────────────────────────────────────────────────────────┤
│  条件 3 / Condition 3: Cookie 属性 (Cookie Attributes)          │
│  SameSite: Lax 或 None (不能是 Strict)                          │
│  Secure: false (HTTP 开发环境) / true (HTTPS 生产环境)           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. 完整请求序列图 / Complete Request Sequence

### 8.1 首次登录（勾选 Remember Me）/ First Login (with Remember Me)

```
浏览器 / Browser                         服务器 / Server
     │                                        │
     │──── GET / ───────────────────────────►│
     │                                        │── 创建 Session
     │◄─── 200 + JSESSIONID=AAA ────────────│
     │                                        │
     │  [用户填写表单 / User fills form]        │
     │                                        │
     │──── POST /api/auth/login ────────────►│
     │     Cookie: JSESSIONID=AAA             │
     │     username=ferry                     │
     │     password=123456                    │
     │     remember=true                      │
     │                                        │── 验证凭证
     │                                        │── Session Fixation 防护
     │                                        │── 更换 JSESSIONID
     │                                        │── 创建 Remember-Me Token
     │                                        │── INSERT persistent_logins
     │◄─── 200 ──────────────────────────────│
     │     Set-Cookie: JSESSIONID=BBB (new)   │
     │     Set-Cookie: my_custom_remember_    │
     │       me_cookie=<token>; Max-Age=...   │
     │     {"success":true,"message":"login   │
     │      success"}                         │
     │                                        │
     │  [跳转到 /index]                        │
     │                                        │
     │──── GET /api/auth/status ────────────►│
     │     Cookie: JSESSIONID=BBB             │
     │                                        │── Session 中找到认证信息
     │◄─── 200 {"success":true,...} ─────────│
```

### 8.2 关闭浏览器后再次访问（Remember-Me 自动登录）/ Revisit After Browser Close (Remember-Me Auto-Login)

```
浏览器 / Browser                         服务器 / Server
     │                                        │
     │  [浏览器重新打开 / Browser reopened]      │
     │  [JSESSIONID 已丢失 / JSESSIONID lost]   │
     │  [Remember-Me Cookie 仍在 / still here]  │
     │                                        │
     │──── GET /index ──────────────────────►│
     │     Cookie: my_custom_remember_me_     │
     │       cookie=<token>                   │
     │                                        │── JSESSIONID 不存在
     │                                        │── 创建新 Session
     │                                        │── RememberMeFilter 检测到 Cookie
     │                                        │── Base64 解码 → series + token
     │                                        │── SELECT from persistent_logins
     │                                        │── Token 匹配 → 自动认证
     │                                        │── UPDATE token (旋转)
     │◄─── 200 ──────────────────────────────│
     │     Set-Cookie: JSESSIONID=CCC (new)   │
     │     Set-Cookie: my_custom_remember_    │
     │       me_cookie=<new_token>; Max-Age=..│
     │     [页面正常显示,用户已自动登录]          │
```

### 8.3 登出 / Logout

```
浏览器 / Browser                         服务器 / Server
     │                                        │
     │──── POST /api/auth/logout ───────────►│
     │     Cookie: JSESSIONID=BBB             │
     │                                        │── Session.invalidate()
     │                                        │── DELETE from persistent_logins
     │                                        │── clear Remember-Me Cookie
     │◄─── 200 ──────────────────────────────│
     │     Set-Cookie: JSESSIONID= (expired)  │
     │     Set-Cookie: my_custom_remember_    │
     │       me_cookie= (expired)             │
     │                                        │
     │  [跳转到登录页 / Navigate to login]      │
```

---

## 附录 / Appendix: 关键配置文件一览 / Key Configuration Summary

### application.yaml (核心配置 / Core Config)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study
    username: root
    password: 1234
  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your-smtp-password
    properties:
      mail.smtp.ssl.enable: true
  # Redis 使用默认 localhost:6379 (无需显式配置)
```

### build.gradle.kts (关键依赖 / Key Dependencies)

```kotlin
// Spring Boot Starters
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.springframework.boot:spring-boot-starter-mail")
implementation("org.springframework.boot:spring-boot-starter-validation")

// ORM & Database
implementation("com.baomidou:mybatis-plus-spring-boot4-starter:3.5.13")
implementation("com.mysql:mysql-connector-j")

// JSON
implementation("com.google.code.gson:gson:2.13.1")
```

### 前端关键配置 / Frontend Key Config

```typescript
// main.js
axios.defaults.baseURL = 'http://localhost:8080'

// vite.config.js
export default defineConfig({
  base: '/separatedProjectBasedOnSession/',
  plugins: [vue(), vueDevTools({ launchEditor: 'idea' })],
  resolve: { alias: { '@': './src' } }
})
```

---

## 总结 / Summary

| 概念 / Concept | 一句话总结 / One-Line Summary |
|---|---|
| **JSESSIONID** | 服务器内存中的会话标识符，浏览器关闭即失效。关闭浏览器后打开→需要重新登录（除非有 Remember-Me） |
| **Remember-Me** | 持久化 Cookie + 数据库 Token，有效期 2 周。关闭浏览器后打开→自动登录 |
| **withCredentials** | 前端 Axios 必须设为 `true`，后端 CORS `allowCredentials` 必须设为 `true`，Cookie 才能跨域传输 |
| **Session Fixation** | 登录成功时 Spring Security 自动更换 JSESSIONID，防止攻击者预设 Session ID |
| **Token 旋转 / Token Rotation** | Remember-Me 每次自动认证后都刷新 Token 值，即使旧 Token 泄露也无法使用 |
| **BCrypt** | 密码哈希算法，每次加密结果不同（自带 Salt），相比 MD5/SHA 更安全 |
| **验证码频率限制** | Redis 存储验证码 3 分钟，前 60 秒不可重发 |
