package com.joker.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.reggie.common.BaseContext;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.VoucherOrder;
import com.joker.reggie.mapper.VoucherOrderMapper;
import com.joker.reggie.service.ISeckillVoucherService;
import com.joker.reggie.service.IVoucherOrderService;
import com.joker.reggie.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker idWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill2.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }



    // 创建阻塞队列 当一个线程从该队列获取元素时发现没有元素，则该线程会阻塞，直到有元素，线程才会被唤醒并获取元素
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // @PostConstruct：在当前类初始化之后执行的方法
    @PostConstruct
    private void init(){
        // 在用户秒杀抢购之前执行线程任务
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }


    // 使用内部类创建线程任务
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            try {
                // 1、从队列中获取订单信息
                VoucherOrder voucherOrder = orderTasks.take();
                // 2、创建订单
                createVoucherOrder(voucherOrder);
            } catch (InterruptedException e) {
                // 使用日志记录错误信息
                log.error("处理订单异常");
            }
        }
    }

    @Override
    public R seckillVoucher(Long voucherId) {
        // 获取userId
        Long userId = BaseContext.getCurrentId();
        // 通过idWorker获取orderId
        long orderId = idWorker.nextId("order");
        // 这里不要给null值，而是给空集合 该方法参数依据Lua脚本文件传入
        Long execute = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int i = execute.intValue();
        // 2、判断结果是否为0
        if (i != 0) {
            // 2.1 不为0
            return R.error(i == 1 ? "库存不足！" : "用户已存在，不能下单了！");
        }
        // 2.2、为0，有购买资格，把下单信息保存到阻塞队列中
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3、保存orderId
        voucherOrder.setId(orderId);
        // 2.4、保存userId
        voucherOrder.setUserId(userId);
        // 2.5、保存voucherId
        voucherOrder.setVoucherId(voucherId);
        // 2.6、将下单信息放到阻塞队列中
        orderTasks.add(voucherOrder);
        // 返回订单id
        return R.success(orderId);
    }

    /**
     * 理论上方法里面的错误不会出现了，这里是做一个兜底方案
     * @param voucherOrder
     */
    private void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5、实现一人一单（即一个人只能抢一个代金券）
        // 5.1、得到用户id 这里是通过订单对象中的userId获得
        Long userId = voucherOrder.getUserId();
        // 5.2、得到代金券id
        Long voucherId = voucherOrder.getVoucherId();
        // 使用Redis分布式锁 创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 不传参数，即使用默认的waitTime等待时间(-1)，相当于不等待，失败就结束;
        // leaseTime释放时间默认为30秒
        boolean isLock = redisLock.tryLock();
        if (!isLock) {
            // 获取失败，直接返回失败信息或者重试
            log.error("请勿重复下单");
            return;
        }

        try {
            // 5.2、查询订单
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            // 5.3、判断是否已经有券
            if (count > 0) {
                // 5.4、有券返回提示信息
                log.error("请勿重复下单");
                return;
            }

            // 6、扣减库存
            // 这里为了解决库存超卖问题：使用乐观锁（在更新数据的时候才能适用）。即每次扣减库存前都查询对应的票的数量，扣减后票数也跟着修改。
            // 但是这种方法效率太低
            // 改：只要还有票，就随便减，减到没有为止
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock -1") // set stock = stock -1
                    // 对应sql：where voucher_id = voucherId and stock = voucher.getStock()
                    .eq("voucher_id", voucherId).gt("stock",0)
                    .update();
            if (!success){
                // 一般来说库存不够了，返回提示信息
                log.error("优惠券已经被抢空了，请下次再来！");
                return;
            }
            // 8、将订单写入数据库
            save(voucherOrder);
        } finally {
            redisLock.unlock();
        }
    }


}
