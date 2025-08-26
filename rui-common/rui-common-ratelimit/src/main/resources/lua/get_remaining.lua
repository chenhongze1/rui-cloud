-- 获取剩余次数Lua脚本
-- KEYS[1]: 限流key

local key = KEYS[1]

-- 获取当前计数
local current_count = redis.call('GET', key)

if current_count == false then
    -- 没有记录，返回-1表示无限制
    return -1
else
    -- 返回当前计数
    return tonumber(current_count)
end