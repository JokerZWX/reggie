-- 1、参数列表
-- 1.1 优惠券id
-- local 说明是局部变量
local voucherId = ARGV[1]

-- 1.2 用户id
local userId = ARGV[2]

-- 1.3 订单id
local orderId = ARGV[3]

-- 2、数据key
-- 2.1 库存key
-- Lua语言使用 ..表示拼接
local stockKey = 'seckill:stock:'.. voucherId

-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3、脚本业务
-- 3.1判断库存是否充足 get stockKey
-- 这里redis.call()得到的结果是字符串类型，需要强转一下类型 使用tonumber()
if(tonumber(redis.call('get',stockKey)) <= 0) then
    -- 3.2 库存不足，返回1
    return 1
end
-- 注意在Lua语言中 0 表示true
if(redis.call('sismember',orderKey,userId) == 1) then
    -- 3.3 用户已存在订单表中，返回2
    return 2
end

-- 4、扣减库存 incrby stockKey - 1
redis.call('incrby',stockKey,- 1)

-- 5、下单（保存用户）sadd orderKey userId
redis.call('sadd',orderKey,userId)

-- 6、发送消息到消息队列中,XADD stream.orders * k1 v1 k2 v2 .....
-- 这里用id不用orderId是因为跟订单中的属性名对应
redis.call('XADD','stream.orders','*','voucherId',voucherId,'userId',userId,'id',orderId)
-- 执行成功后返回0
return 0