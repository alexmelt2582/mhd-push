-- 1. 获取当前 Key 中存储的“下一次允许处理的时间戳”
local current = redis.call('GET', KEYS[1])
local now = tonumber(ARGV[1])       -- 当前客户端时间 (ms)
local interval = tonumber(ARGV[2])  -- 每个请求需要消耗的时间间隔 (ms)，即 1000/QPS
local permits = tonumber(ARGV[3])   -- 本次请求的数量
local ttl = tonumber(ARGV[4])       -- Key 的过期时间

-- 2. 初始化：如果是第一次访问，假设上次时间是 "过去" (now - interval)
if not current then current = tostring(now - interval) end
current = tonumber(current)

-- 3. 计算需要等待的时间 (wait) 和 开始计算的时间点 (start)
local wait = 0
local start = current

-- 情况 A: 桶是空的/过期的 (current < now)
-- 意味着之前的排队已经结束了，我可以从现在开始算
if current < now then start = now end

-- 情况 B: 桶里有积压 (current > now)
-- 意味着我必须等到 current 那个时间点才能开始处理
if current > now then wait = current - now end

-- 4. 计算新的“下一次允许处理的时间点”
-- 公式：新时间点 = 开始处理的时间 + (单个请求耗时 * 请求数量)
local nextAllowed = start + interval * permits

-- 5. 更新 Redis 中的时间戳，并设置过期时间 (防止内存泄漏)
redis.call('SET', KEYS[1], tostring(nextAllowed), 'PX', ttl)

-- 6. 返回需要等待的时间
return wait