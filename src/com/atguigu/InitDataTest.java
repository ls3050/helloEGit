package com.atguigu;

import org.junit.Test;

import redis.clients.jedis.Jedis;

public class InitDataTest {
	
	// http://192.168.4.165:8080/MySeckill/index.jsp

	// 初始化库存的方法
	@Test
	public void test() {
		
		Jedis jedis =new Jedis("192.168.127.131",6379);
		
		System.out.println(jedis.ping());
		
		String productKey="sk:"+1001+":product";
		String userKey="sk:"+1001+":user";
		
		jedis.set(productKey, "10");
		
		jedis.del(userKey);
		
		String string = jedis.get(productKey);
		
		System.out.println(string);
		
		jedis.close();
	}
	
	@Test
	public void test1() {
		
		Jedis jedis =new Jedis("192.168.127.131",6379);
		
		System.out.println(jedis.ping());
		
		String qtkey="sk:"+1001+":qt";
		String usersKey="sk:"+1001+":usr";
		
		jedis.set(qtkey, "300");
		
		jedis.del(usersKey);
		
		String string = jedis.get(qtkey);
		
		
		System.out.println(string);
		
		jedis.close();
	}
	
	

}
