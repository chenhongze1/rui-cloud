-- 限流Lua脚本
-- KEYS[1]: 限流key
-- ARGV[1]: 限流次数
-- ARGV[2]: 限流时间窗口（秒）
-- ARGV[3]: 当前时间戳（毫秒）

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current_time = tonumber(ARGV[3])

-- 获取当前计数
local current_count = redis.call('GET', key)

if current_count == false then
    -- 第一次访问，设置计数为1，并设置过期时间
    redis.call('SET', key, 1)
    redis.call('EXPIRE', key, window)
    return 1
else
    current_count = tonumber(current_count)
    if current_count < limit then
        -- 未达到限制，计数加1
        redis.call('INCR', key)
        return 1
    else
        -- 达到限制，返回0
        return 0
    end
end