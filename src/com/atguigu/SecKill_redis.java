package com.atguigu;

import java.io.IOException;
import java.util.List;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/*
 *   1. 秒杀的实现
 *   		要求： 一个用户只能秒杀成功一次！
 *   		  	需要存储秒杀成功名单的key-value:
 *   					key ： 	prodid有关
 *   					value:  set
 *   			需要存储产品库存的Key-value
 *   					Key ：    prodid
 *   					value ： string
 *   
 *   2.  秒杀的流程：
 *   		① 判断用户之前是否已经秒成功过
 *   				sismember();
 *   					如果存在，返回false；
 *   
 *   		② 检查商品库存
 *   				get();
 *   					库存状态：
 *   						①Null
 *   							商家还没用初始化库存
 *   						②=0  秒杀完毕
 *   						返回false
 *   						③库存>0 进行秒杀
 *  		③ 秒杀：
 *  				将用户的名，加入到成功的名单中！   sadd()
 *  				库存减1，  decr()
 *  
 *   3. 压力测试 ：  ab(apache benckmark)
 *   		
 *   	ab -n 2000 -c 200 -p /root/postArg  -T "application/x-www-form-urlencoded"  http://192.168.4.165:8080/MySeckill/doseckill
 *   					
 *   	编写post请求携带参数的文件：  
 *   			prodid=1001&	
 *   
 *   4. 高并发的超卖问题！
 *   			
 *   		通过加锁！ Redis是加乐观锁！
 *   
 *   		加锁的时机： 在第一次对key进行读写操作之前，必须watch!
 *   
 *   		防止别人打断秒杀操作，需要进行Multi组队！
 *   
 *   5. 总结： ① 使用synchronized,使用java技术解决，悲观锁，性能慢，造成不公平！
 *   		 ② redis的乐观锁，性能稍快，先加锁，不一定先执行成功！不公平！
 *   			
 *   			乐观锁在高并发情况下，会造成资源的浪费！ 
 *   
 *   6. 如何解决不公平的问题！
 *   
 *   		保证： 先加锁的，一定先执行成功！
 *   
 *   			将从加锁到后续执行期间的所有的命令，变成一条命令（一个原子） 只要redis执行到这条命令，只有执行完，才会处理别人的命令！
 *   
 *   		事务：  将多个命令组合在一起进行执行，有可能别别人打断
 *   
 *   		通过Lua脚本实现！ 先加锁，一定先执行成功！ 解决不公平问题！
 *   
 *   		
 *   
 *   		
 *   
 *   
 */
public class SecKill_redis {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SecKill_redis.class);

	// 秒杀的实际方法
	public static boolean doSecKill(String uid, String prodid) throws IOException {
		
	
			// 生成Key
			String productKey = "sk:" + prodid + ":product";
			String userKey = "sk:" + prodid + ":user";
			
			// 获取Jedis
			JedisPool jedisPool = JedisPoolUtil.getJedisPoolInstance();
			
			Jedis jedis = jedisPool.getResource();
			
			// 验证用户是否已经秒杀成功
			if (jedis.sismember(userKey, uid)) {
				
				System.err.println("当前===" + uid + "===已经秒杀过！");
				
				jedis.close();
				
				return false;
				
			}
			;
			
			
			// 使用watch加锁
			jedis.watch(productKey);
			
			// 判断库存的状态
			
			String product_value = jedis.get(productKey);
			
			if (product_value == null) {
				
				System.err.println("当前===" + prodid + "===尚未上架！");
				
				jedis.close();
				
				return false;
				
			}
			
			int store = Integer.parseInt(product_value);
			
			if (store <= 0) {
				
				System.err.println("当前===" + prodid + "===已经无货！！");
				
				jedis.close();
				
				return false;
				
			}
			
			//================进入秒杀流程===================
			
			Transaction transaction = jedis.multi();
			
			transaction.sadd(userKey, uid);
			
			transaction.decr(productKey);
			
			// 执行 : 只会记录执行成功的命令的返回值
			List<Object> result = transaction.exec();
			
			if (result == null || result.size() != 2) {
				
				System.err.println("当前===" + uid + "===秒杀失败！！");
				
				jedis.close();
				
				return false;
			}
			
			System.out.println("恭喜==="+uid+"===秒杀成功！");
			
			jedis.close();
			
			return true;
			
		}
		

}
