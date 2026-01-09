# Patrol - 巡逻/查岗插件 (Kotlin / Bukkit & Paper)

一个给管理员使用的巡逻工具插件：

- `/patrol`：在**观察者模式**下随机传送到一名在线玩家身边进行巡逻  
- `/pmsg <玩家>`：向指定玩家发送“查岗提示”消息，并播放提示音（可配置次数/间隔）
- 支持 `/patrol reload` 热重载配置
- 每个管理员都有独立的巡逻历史列表，避免重复巡逻同一批玩家
- 自带“死锁检测”：当所有玩家都被巡逻过，会自动清空历史并开启新一轮巡逻

---

## 功能特性

### 1) 随机巡逻 `/patrol`
- 仅允许玩家执行（控制台不能执行）
- 需要权限：`patrol.use`
- **必须处于观察者模式（SPECTATOR）**
- 传送目标规则：
  - 从在线玩家中随机挑一个（排除自己）
  - 优先选择**不在巡逻历史**里的玩家
  - 如果所有人都巡逻过，会自动清空历史并重新开始（避免无目标）

### 2) 查岗提示 `/pmsg <玩家>`
- 需要权限：`patrol.admin`
- 给目标玩家发送配置里的提示消息（支持 `&` 颜色符号）
- 并按配置次数/间隔播放提示音

### 3) 配置热重载 `/patrol reload`
- 需要权限：`patrol.admin`
- 重新加载 `config.yml`

---

## 运行环境

- 推荐：Paper 服务端（更好兼容 Adventure 文本组件）
- 需要 Bukkit API（Spigot/Paper 系）
- JDK 版本请按你的服务端版本要求选择（例如 1.20+ 常用 JDK 17）

> 说明：代码使用了 `net.kyori.adventure` 组件发送消息，Paper 系通常更顺滑；如果你用的是较老/特殊的 Spigot，可能需要自行处理依赖或改为传统消息发送方式。

---

## 安装方法

1. 编译生成插件 jar（或把 jar 放入 `plugins/`）
2. 启动服务器，生成默认配置（`plugins/Patrol/config.yml`）
3. 配置权限（LuckPerms 等）
4. 重启服务器或使用 `/patrol reload`

---

## 指令列表

| 指令 | 说明 | 权限 | 备注 |
|------|------|------|------|
| `/patrol` | 随机巡逻传送到玩家 | `patrol.use` | 必须观察者模式 |
| `/patrol reload` | 重载配置文件 | `patrol.admin` | 控制台可用 |
| `/pmsg <玩家>` | 发送查岗提示+提示音 | `patrol.admin` | 玩家需在线 |

---

## 权限节点

- `patrol.use`：允许使用 `/patrol`
- `patrol.admin`：允许使用 `/patrol reload` 与 `/pmsg`

---

## 配置文件（config.yml）

你的代码中读取的配置项如下：

### settings
- `settings.expire-minutes`（默认 60）
  - 超过该分钟数未更新巡逻记录，则清空巡逻历史（相当于“超时重置”）
- `settings.history-size`（默认 10）
  - 巡逻历史最多记录多少个玩家 UUID（先进先出）

### pmsg
- `pmsg.message`（默认 `&c查岗中...`）
  - 发送给目标玩家的提示消息，支持 `&` 颜色代码
- `pmsg.sound-count`（默认 4）
  - 播放提示音次数
- `pmsg.sound-interval`（默认 10 ticks）
  - 提示音播放间隔（单位：ticks；20 ticks = 1 秒）

示例 `config.yml`（你可以作为默认配置）：

```yml
settings:
  expire-minutes: 60
  history-size: 10

pmsg:
  message: "&c查岗中..."
  sound-count: 4
  sound-interval: 10
