# 双 SessionID 工作机制详解

## 概述

该项目在登录后会产生**两个 SessionID**，分别用于不同的目的：
1. **JSESSIONID**：Spring Security 标准会话ID，用于维持单个浏览器会话的登录状态
2. **Remember-Me Token**：持久化登录令牌，用于实现"记住我"功能，跨浏览器会话保持登录

---

## 详细机制

### 1. JSESSIONID（标准会话ID）

#### 生成流程

```
请求登录 (POST /api/auth/login)
         ↓
Spring Security 验证用户身份 ✓
         ↓
HttpSession 创建 → 生成 JSESSIONID
         ↓
Response Header 中设置: Set-Cookie: JSESSIONID=xxx
         ↓
浏览器保存 Cookie
         ↓
后续请求自动在 Request Header 中发送此 Cookie
```

#### 配置位置

**文件**: `backend/src/main/kotlin/com/ferryside/config/SecurityConfiguration.kt`

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfiguration(...) {
    @Bean
    fun securityFilterChain(http: HttpSecurity, ...): SecurityFilterChain {
        http {
            authorizeHttpRequests { 
                authorize("/api/auth/login", permitAll)      // 允许未认证用户访问
                authorize(anyRequest, authenticated)          // 其他请求需要认证
            }
            
            formLogin {
                loginProcessingUrl = "/api/auth/login"       // 登录处理URL
                authenticationSuccessHandler = successHandler // 成功处理
                authenticationFailureHandler = failureHandler // 失败处理
            }
            // ... 其他配置
        }
    }
}
```

#### 生命周期

- **创建时机**：用户成功登录后
- **存储位置**：浏览器 Cookie（自动存储）
- **有效期**：单个浏览器会话内（关闭浏览器后过期）
- **作用域**：同一域名的所有请求

#### HTTP 请求/响应示例

```
登录请求 (第一次):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Request:
POST /api/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
[请求体: username=user&password=pass&remember=true]

Response:
HTTP/1.1 200 OK
Set-Cookie: JSESSIONID=ABC123DEF456; Path=/; HttpOnly
Content-Type: application/json
{"success": true, "message": "login success"}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

后续请求:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Request:
GET /api/user/profile HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=ABC123DEF456  ← 浏览器自动发送
[无需手动设置]

Response:
HTTP/1.1 200 OK
{"user": "john", "email": "john@example.com"}
```

---

### 2. Remember-Me Token（持久化登录令牌）

#### 生成流程

```
用户勾选"记住我" + 登录成功
         ↓
Spring Security RememberMe Filter 触发
         ↓
生成两部分 Token:
  ├─ series (系列ID): 唯一标识这个"记住我"记录
  └─ token (令牌值): 实际的认证令牌
         ↓
存储到数据库 (persistent_logins 表)
  ├─ series (主键)
  ├─ username (用户名)
  ├─ token (令牌值)
  └─ lastUsed (最后使用时间)
         ↓
Response Header 中设置: Set-Cookie: my_custom_remember_me_cookie=series:token
         ↓
浏览器保存 Cookie
         ↓
关闭浏览器，JSESSIONID 过期，但 Remember-Me Cookie 仍然存在
         ↓
下次访问时，Remember-Me Filter 读取 Cookie
         ↓
从数据库验证 series 和 token 的有效性
         ↓
自动创建新的 HttpSession + JSESSIONID
```

#### 配置位置

**文件**: `backend/src/main/kotlin/com/ferryside/config/SecurityConfiguration.kt`

```kotlin
rememberMe {
    rememberMeParameter = "remember"                    // 登录表单中的参数名
    rememberMeCookieName = "my_custom_remember_me_cookie" // Cookie 名称
    tokenRepository = persistentLoginService            // Token 存储库
}
```

#### 数据库表结构

**表名**: `persistent_logins`

```sql
CREATE TABLE persistent_logins (
    series VARCHAR(64) PRIMARY KEY,      -- 系列ID（唯一标识）
    username VARCHAR(64) NOT NULL,       -- 用户名
    token VARCHAR(64) NOT NULL,          -- 令牌值
    last_used TIMESTAMP NOT NULL         -- 最后使用时间
);
```

#### 实现类

**文件**: `backend/src/main/kotlin/com/ferryside/service/impl/PersistentLoginServiceImpl.kt`

```kotlin
@Service
class PersistentLoginServiceImpl : 
    PersistentLoginService, 
    ServiceImpl<PersistentLoginMapper, PersistentLogin>() {
    
    // 创建新的 Remember-Me Token
    override fun createNewToken(token: PersistentRememberMeToken) {
        save(PersistentLogin(
            series = token.series,
            username = token.username,
            token = token.tokenValue,
            lastUsed = token.date // 转换为 LocalDateTime
        ))
    }
    
    // 更新 Token（每次使用都会更新以增加安全性）
    override fun updateToken(series: String, tokenValue: String, lastUsed: Date) {
        update(KtUpdateWrapper(PersistentLogin::class.java)
            .eq(PersistentLogin::series, series)
            .set(PersistentLogin::token, tokenValue)
            .set(PersistentLogin::lastUsed, lastUsed))
    }
    
    // 读取 Token（验证有效性）
    override fun getTokenForSeries(seriesId: String): PersistentRememberMeToken? {
        val entity = getOne(
            KtQueryWrapper(PersistentLogin::class.java)
                .eq(PersistentLogin::series, seriesId)
        ) ?: return null
        
        return PersistentRememberMeToken(
            entity.username,
            entity.series,
            entity.token,
            Date.from(entity.lastUsed.atZone(ZoneId.systemDefault()).toInstant())
        )
    }
    
    // 移除用户的所有 Token（登出时）
    override fun removeUserTokens(username: String) {
        remove(KtQueryWrapper(PersistentLogin::class.java)
            .eq(PersistentLogin::username, username))
    }
}
```

#### 生命周期

- **创建时机**：用户在登录时勾选"记住我"复选框
- **存储位置**：
  - Cookie：浏览器（长期保存）
  - 数据库：`persistent_logins` 表
- **有效期**：可以跨多个浏览器会话（直到用户主动登出或 Cookie 被删除）
- **安全特性**：
  - 每次使用时 token 值会更新（防重放）
  - series ID 保持不变（用于追踪同一设备）
  - 如果检测到 token 被篡改，会删除该 series 的所有记录

---

## 前端集成

**文件**: `frontend/src/net/index.ts`

```typescript
function post(url: string, data: object, ...) {
    axios.post(url, data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        },
        withCredentials: true  // ← 关键：自动发送和接收 Cookie
    })
    .then(({data}) => {
        if(data.success) {
            success(data.message, data.status)
        } else {
            failure()
        }
    })
}
```

`withCredentials: true` 确保：
- 请求时自动发送 JSESSIONID 和 Remember-Me Cookie
- 响应时自动接收并保存 Set-Cookie 中的新 Cookie

---

## 完整登录流程图

```
用户操作
    ↓
[1] 输入用户名/密码，勾选"记住我"
    ↓ (POST /api/auth/login)
后端验证
    ↓
[2] Spring Security 验证身份 ✓
    ↓
[3a] 创建 JSESSIONID
     → HttpSession 对象
     → Set-Cookie: JSESSIONID=xxx
    ↓
[3b] 如果 remember=true，创建 Remember-Me Token
     → 生成 series 和 token
     → 存储到数据库
     → Set-Cookie: my_custom_remember_me_cookie=series:token
    ↓
[4] LoginSuccessHandler 返回成功响应
    ↓
浏览器接收
    ↓
[5] 自动保存两个 Cookie
    ├─ JSESSIONID (Session Cookie - 关闭浏览器后删除)
    └─ my_custom_remember_me_cookie (Persistent Cookie - 长期保存)
    ↓
后续请求
    ↓
[6a] 浏览器自动在 Request Header 中发送两个 Cookie
[6b] Spring Security 验证 JSESSIONID（首选）
[6c] 如果 JSESSIONID 无效，尝试验证 Remember-Me Token
    ↓
[7] 返回用户请求的资源
```

---

## 关闭浏览器后的行为

### 场景：用户关闭浏览器，第二天重新访问网站

```
Day 1 - 登录后
  JSESSIONID (Session Cookie)
    ├─ 浏览器会话中有效
    └─ 关闭浏览器 → 自动删除 ✗
  
  my_custom_remember_me_cookie (Persistent Cookie)
    ├─ 存储在浏览器本地
    └─ 关闭浏览器 → 仍然存在 ✓

Day 2 - 重新打开浏览器访问网站
  ↓
Request Cookie: my_custom_remember_me_cookie=series:token
  ↓
Spring RememberMe Filter 处理
  ├─ 从 Cookie 解析 series 和 token
  ├─ 从数据库查询该 series 对应的记录
  ├─ 验证 token 是否匹配
  ├─ 如果匹配 ✓
  │   ├─ 更新数据库中的 token（重新生成新值）
  │   ├─ 创建新的 HttpSession
  │   ├─ 自动生成新的 JSESSIONID
  │   └─ Set-Cookie: JSESSIONID=new_value
  │       Set-Cookie: my_custom_remember_me_cookie=series:new_token
  │
  └─ 如果不匹配 ✗
      ├─ 可能 Cookie 被篡改
      ├─ 删除数据库中该 series 的所有记录
      └─ 用户需要重新登录
```

---

## 安全考虑

### JSESSIONID
- ✓ 仅在当前浏览器会话有效
- ✓ HttpOnly 标志防止 JavaScript 访问
- ✓ Secure 标志（HTTPS 时）防止中间人窃听
- ✗ 关闭浏览器后失效

### Remember-Me Token
- ✓ Token 值每次使用都更新（防重放攻击）
- ✓ Series ID 用于追踪设备（检测异常登录）
- ✓ 用户登出时删除所有 Token
- ✗ 长期有效，需定期审查
- ⚠ Cookie 本身也需要 Secure 和 HttpOnly 标志

### 建议
1. 定期更新 Token 过期时间
2. 提供"踢出所有设备"的选项
3. 在敏感操作前重新验证身份
4. 监控 persistent_logins 表中的异常活动

---

## 故障排查

| 问题 | 原因 | 解决方案 |
|------|------|--------|
| 登录成功但后续请求返回 401 | JSESSIONID 丢失或过期 | 检查 `withCredentials: true` 配置 |
| "记住我" 功能不工作 | 数据库表不存在或 ORM 配置错误 | 确保 `persistent_logins` 表已创建 |
| 重启浏览器后仍然登录，但刷新页面失败 | RememberMe Token 无效 | 检查数据库中 series/token 是否匹配 |
| 多个标签页中一个登出，其他标签页也失效 | JSESSIONID 共享 | 正常行为，因为它们是同一个 Session |
| 无法在不同设备上自动登录 | 这是设计特性 | Remember-Me Token 仅在同一浏览器中有效 |

---

## 总结

| 特性 | JSESSIONID | Remember-Me Token |
|------|-----------|-------------------|
| **用途** | 维持当前浏览器会话 | 跨会话持久化登录 |
| **存储** | 内存 + Cookie | 数据库 + Cookie |
| **有效期** | 单个会话 | 可跨多个会话 |
| **自动更新** | 服务器主动 | 每次验证时 |
| **主要用途** | 正常登录状态 | "记住我"功能 |
| **安全等级** | 高（短期） | 中（需定期审查） |

该双 ID 设计既保证了安全性，又提供了用户友好的"记住我"体验。
