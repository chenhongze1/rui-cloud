-- 获取重置时间Lua脚本
-- KEYS[1]: 限流key

local key = KEYS[1]

-- 获取TTL（剩余过期时间）
local ttl = redis.call('TTL', key)

if ttl == -2 then
    -- key不存在
    return 0
elseif ttl == -1 then
    -- key存在但没有设置过期时间
    return 0
else
    -- 返回剩余过期时间（秒）
    return ttl
end