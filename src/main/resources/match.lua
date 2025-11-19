-- Lua Script for atomic match processing
-- Args: userA, userB
-- Returns: 1 if success, 0 if failed

local userA = ARGV[1]
local userB = ARGV[2]

local statusKeyA = "user:" .. userA .. ":status"
local statusKeyB = "user:" .. userB .. ":status"
local matchedWithKeyA = "user:" .. userA .. ":matchedWith"
local matchedWithKeyB = "user:" .. userB .. ":matchedWith"
local queueKey = "matching:queue"

-- Check if both users are in WAITING status
local statusA = redis.call("GET", statusKeyA)
local statusB = redis.call("GET", statusKeyB)

if statusA ~= "WAITING" or statusB ~= "WAITING" then
    return 0
end

-- Set both users to MATCHED
redis.call("SET", statusKeyA, "MATCHED")
redis.call("SET", statusKeyB, "MATCHED")

-- Set matchedWith
redis.call("SET", matchedWithKeyA, userB)
redis.call("SET", matchedWithKeyB, userA)

-- Remove both users from queue
redis.call("ZREM", queueKey, userA)
redis.call("ZREM", queueKey, userB)

return 1

