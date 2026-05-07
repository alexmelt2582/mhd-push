-- =====================================================================
-- Redis 漏桶限流脚本
-- 用途：通过“下一次允许处理时间点”实现匀速放行，适合按账号或租户平滑输出。
-- 返回值：
--   wait 毫秒数 -> 调用方需要等待多久才能继续处理。
-- =====================================================================

-- 1. 读取当前 key 中存储的“下一次允许处理的时间戳”。
local current = redis.call('GET', KEYS[1])
local now = tonumber(ARGV[1])
local interval = tonumber(ARGV[2])
local permits = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

-- 2. 初始化：第一次访问时假设上次可处理时间已经过去。
if not current then current = tostring(now - interval) end
current = tonumber(current)

local wait = 0
local start = current

-- 3. 如果当前没有积压，则从 now 开始计算。
if current < now then start = now end

-- 4. 如果当前还有积压，则调用方需要等待到 current。
if current > now then wait = current - now end

-- 5. 计算新的“下一次允许处理时间”。
local nextAllowed = start + interval * permits

-- 6. 更新 Redis 中的时间戳，并设置过期时间防止内存泄漏。
redis.call('SET', KEYS[1], tostring(nextAllowed), 'PX', ttl)

-- 7. 返回需要等待的毫秒数。
return wait
