package com.contoso.app.trident;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import backtype.storm.topology.FailedException;

public class BlobWriterState {
	// TODO: replace Redis with zookeeper to store BlobWriter State
	static public void flush() {
		Redis.flush();
	}

	static public String get(String key) {
		return Redis.get(key);
	}

	static public List<String> getList(String key, long maxLength) {
		return Redis.getList(key, maxLength);
	}

	static public void setState(String key, String value, String keyToList, List<String> stringList) {
		Redis.setState(key, value, keyToList, stringList);
	}
	
	static public void clearState(String key, String keyToList) {
		Redis.clearState(key, keyToList);
	}


	private static class Redis {
		private static final Logger logger = (Logger) LoggerFactory.getLogger(Redis.class);
		private static String host = null;
		private static String password= null;
		private static int port = -1;
		private static int timeout = -1;
		private static boolean useSSL = true;

		static {
			host = ConfigProperties.getProperty("redis.host");
			password = ConfigProperties.getProperty("redis.password");
			port = Integer.parseInt(ConfigProperties.getProperty("redis.port"));
			timeout = Integer.parseInt(ConfigProperties.getProperty("redis.timeout"));
			if(host == null)
			{
				throw new ExceptionInInitializerError("Error: host is missing" );
			}
			if(password == null)
			{
				throw new ExceptionInInitializerError("Error: password is missing" );
			}
			if(port == -1)
			{
				throw new ExceptionInInitializerError("Error: port is missing" );
			}
			if(timeout == -1)
			{
				throw new ExceptionInInitializerError("Error: timeout is missing" );
			}
		}

		private static void flush() {
			if (LogSetting.LOG_REDIS) {
				logger.info("flushDB Begin");
			}
			try (Jedis jedis = new Jedis(host, port, timeout, useSSL)) {
				jedis.auth(password);
				jedis.connect();
				if (jedis.isConnected()) {
					jedis.flushDB();
				} else {
					if (LogSetting.LOG_REDIS) {
						logger.info("Error: can't cannect to Redis !!!!!");
					}
					throw new FailedException("can't cannect to Redis");
				}
			}
			if (LogSetting.LOG_REDIS) {
				logger.info("flushDB End");
			}
		}

		private static String get(String key) {
			String value = null;
			if (LogSetting.LOG_REDIS) {
				logger.info("get Begin params: key= " + key);
			}
			if (key != null) {

				try (Jedis jedis = new Jedis(host, port, timeout, useSSL)) {
					jedis.auth(password);
					jedis.connect();
					if (jedis.isConnected()) {
						value = jedis.get(key);
					} else {
						if (LogSetting.LOG_REDIS) {
							logger.info("Error: can't cannect to Redis !!!!!");
						}
						throw new FailedException("can't cannect to Redis");
					}
				}
			}
			if (LogSetting.LOG_REDIS) {
				logger.info("get End returns " + value);
			}
			return value;
		}

		private static List<String> getList(String key, long maxLength) {
			List<String> stringList = null;
			if (LogSetting.LOG_REDIS) {
				logger.info("getList Begin with params: key= " + key + " maxLength= " + maxLength);
			}
			if (key != null && maxLength > 0) {

				try (Jedis jedis = new Jedis(host, port, timeout, useSSL)) {
					jedis.auth(password);
					jedis.connect();
					if (jedis.isConnected()) {
						stringList = jedis.lrange(key, 0, maxLength - 1);
					} else {
						if (LogSetting.LOG_REDIS) {
							logger.info("Error: can't cannect to Redis !!!!!");
						}
						throw new FailedException("can't cannect to Redis");
					}
				}
			}
			if (LogSetting.LOG_REDIS) {
				if (stringList == null || stringList.size() == 0) {
					logger.info("getList returns 0 record");
				} else {
					logger.info("getList returns " + stringList.size() + " record");
					for (String s : stringList) {
						logger.info("getList return record: " + s);
					}
				}
			}
			if (LogSetting.LOG_REDIS) {
				logger.info("getList End");
			}
			return stringList;
		}
		
		static void clearState(String key, String keyToList) {
			if (LogSetting.LOG_REDIS) {
				logger.info("clearState Begin");
				logger.info("key= " + key + " keyToList= "+keyToList);
			}

			if (key != null && keyToList != null) {
				try (Jedis jedis = new Jedis(host, port, timeout, useSSL)) {
					jedis.auth(password);
					jedis.connect();
					if (jedis.isConnected()) {
						Transaction trans = jedis.multi();
						try {
							trans.del(key);
							trans.del(keyToList);
							trans.exec();
						} catch (Exception e) {
							trans.discard();
							throw new FailedException(e.getMessage());
						}
					} else {
						if (LogSetting.LOG_REDIS) {
							logger.info("Error: can't cannect to Redis !!!!!");
						}
						throw new FailedException("can't cannect to Redis");
					}
				}
			}
			if (LogSetting.LOG_REDIS) {
				logger.info("clearState End");
			}
		}

		static void setState(String key, String value, String keyToList, List<String> stringList) {
			if (LogSetting.LOG_REDIS) {
				logger.info("setState Begin");
				logger.info("setState params: key= " + key);
				if (key == null || value == null || keyToList == null || stringList == null || stringList.isEmpty()) {
					logger.info("setState has some null parameters!");
				} else {
					logger.info("setState Begin: key= " + key + " value= " + value + " keyToList= " + keyToList);
					for (String s : stringList) {
						logger.info("setState params stringlist: " + s);
					}
				}
			}
			if (key != null && value != null && keyToList != null && stringList != null && !stringList.isEmpty()) {
				try (Jedis jedis = new Jedis(host, port, timeout, useSSL)) {
					jedis.auth(password);
					jedis.connect();
					if (jedis.isConnected()) {
						Transaction trans = jedis.multi();
						try {
							trans.set(key, value);
							trans.del(keyToList);
							for (String str : stringList) {
								trans.rpush(keyToList, str);
							}
							trans.exec();
						} catch (Exception e) {
							trans.discard();
							throw new FailedException(e.getMessage());
						}

					} else {
						if (LogSetting.LOG_REDIS) {
							logger.info("Error: can't cannect to Redis !!!!!");
						}
						throw new FailedException("can't cannect to Redis");
					}
				}
			}
			if (LogSetting.LOG_REDIS) {
				logger.info("setList End");
			}
		}
	}
}
