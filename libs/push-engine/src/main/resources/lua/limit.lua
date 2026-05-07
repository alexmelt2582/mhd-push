-- =====================================================================
-- Redis 滑动窗口限流脚本
-- 用途：在指定时间窗口内限制最大请求次数，适用于频控和去重场景。
-- 调用方：flowcontrol / deduplication 相关模块。
-- 返回值：
--   0 -> 未超过阈值，允许本次请求进入窗口。
--   1 -> 已超过阈值，应拒绝本次请求。
-- =====================================================================

-- KEYS[1]: 限流 key
-- ARGV[1]: 限流窗口，单位毫秒
-- ARGV[2]: 当前时间戳（作为 score）
-- ARGV[3]: 阈值
-- ARGV[4]: 当前请求唯一 value

-- 1. 移除窗口开始时间之前的历史数据，保证只统计当前窗口内的请求。
redis.call('zremrangeByScore', KEYS[1], 0, ARGV[2] - ARGV[1])

-- 2. 统计当前窗口内已有请求数量。
local res = redis.call('zcard', KEYS[1])

-- 3. 如果未达到阈值，则写入本次请求并设置过期时间。
if (res == nil) or (res < tonumber(ARGV[3])) then
    redis.call('zadd', KEYS[1], ARGV[2], ARGV[4])
    redis.call('expire', KEYS[1], ARGV[1] / 1000)
    return 0
else
    return 1
end
