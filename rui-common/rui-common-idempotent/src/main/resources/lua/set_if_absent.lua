-- 幂等性检查和设置脚本
-- KEYS[1]: 幂等性key
-- ARGV[1]: 过期时间（秒）

local key = KEYS[1]
local expireTime = tonumber(ARGV[1])
local currentTime = tostring(redis.call('TIME')[1])

-- 检查key是否存在
if redis.call('EXISTS', key) == 1 then
    -- key已存在，返回0表示重复请求
    return 0
else
    -- key不存在，设置key并返回1表示非重复请求
    redis.call('SETEX', key, expireTime, currentTime)
    return 1
end