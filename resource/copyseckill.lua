
--获取输入的两个变量 local是一个关键字，表示局部变量 没有声明就是全局变量
local userid=KEYS[1]; 
local prodid=KEYS[2];
--定义两个key，使用..进行拼接 
local productKey="sk:"..prodid..":product";
local usersKey="sk:"..prodid..":user'; 
--调用函数，判断是否是重复用户
local userExists=redis.call("sismember",usersKey,userid);
--判断是否存在这两个key
if tonumber(userExists)==1 then 
	  return 2;
end
local num= redis.call("get" ,productKey);
--判断库存如果小于0，就返回0，否则就减库存，加用户
if tonumber(num)<=0 then 
	  return 0; 
else 
	  redis.call("decr",productKey);
	  redis.call("sadd",usersKey,userid);
end
return 1;