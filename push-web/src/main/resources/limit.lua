-- 滑动窗口限流 Lua 脚本
-- 功能：在指定时间窗口内限制最大请求次数，防止刷消息
-- 示例：用户五分钟之内收到的消息过滤：
-- KEYS[1]: SW_5eb63bbbe01eeed093cb22bb8f5acdc3（使用消息模板+用户ID+发送内容模型 三者md5加密之后值）
-- ARGV[1]: 5000（5秒）
-- ARGV[2]: 1700000000000 （当前时间）
-- ARGV[3]: 5（数量）
-- ARGV[4]: 8cm75beea01ddds746cb22bb8f5acdc3（当前请求的唯一 ID）

--KEYS[1]: 限流 key
--ARGV[1]: 限流窗口,毫秒
--ARGV[2]: 当前时间戳（作为score）
--ARGV[3]: 阈值
--ARGV[4]: score 对应的唯一value
-- 1\. 移除开始时间窗口之前的数据
redis.call('zremrangeByScore', KEYS[1], 0, ARGV[2]-ARGV[1])
-- 2\. 统计当前元素数量
local res = redis.call('zcard', KEYS[1])
-- 3\. 是否超过阈值
if (res == nil) or (res < tonumber(ARGV[3])) then
    redis.call('zadd', KEYS[1], ARGV[2], ARGV[4])
    redis.call('expire', KEYS[1], ARGV[1]/1000)
    return 0
else
    return 1
end
