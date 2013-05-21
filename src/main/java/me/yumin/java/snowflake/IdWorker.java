/** Copyright 2010-2012 Twitter, Inc.*/
package me.yumin.java.snowflake;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An object that generates IDs. This is broken into a separate class in case we
 * ever want to support multiple worker threads per process
 * 
 * @author yumin
 */
public class IdWorker {

	/**
	 * 
	 */
	private final ReentrantLock LOCK = new ReentrantLock();
	private final long workerIdBits = 5L;
	private final long datacenterIdBits = 5L;
	private final long sequenceBits = 12L;
	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	private final long workerIdShift = sequenceBits;
	private final long datacenterIdShift = sequenceBits + workerIdBits;
	private final long timestampLeftShift = sequenceBits + workerIdBits
			+ datacenterIdBits;
	private final long sequenceMask = -1L ^ (-1L << sequenceBits);
	private long epoch = 1288834974657L;

	/**
	 * 
	 */
	private static IdWorker instance = null;
	private static int isInitialized = 0;
	private long workerId = 0;
	private long datacenterId = 0;
	private long lastTimestamp = -1L;
	private long sequence = 0L;

	/**
	 * 
	 * @param workerId
	 * @param datacenterId
	 */
	private IdWorker(long workerId, long datacenterId) {

		if (workerId > this.maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format(
					"workerId can't be greater than %d or less than 0.",
					this.maxWorkerId));
		}
		if (datacenterId > this.maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format(
					"datacenterId can't be greater than %d or less than 0.",
					this.maxDatacenterId));
		}

		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	/**
	 * 
	 * @param workerId
	 * @param datacenterId
	 * @return
	 */
	public static IdWorker getInstance(long workerId, long datacenterId) {

		/*
		 * Double-Checked Locking
		 * 
		 * @see
		 * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking
		 * .html
		 */
		if (0 == isInitialized) {
			synchronized (IdWorker.class) {
				if (0 == isInitialized) {
					instance = new IdWorker(workerId, datacenterId);
					isInitialized = 1;
				}
			}
		}

		return instance;
	}

	/**
	 * 
	 * @return
	 */
	public long generateId() {

		long id = 0L;

		LOCK.lock();

		try {

			long timestamp = this.timeGen();
			if (timestamp == this.lastTimestamp) {
				this.sequence = (1 + this.sequence) & this.sequenceMask;
				if (0 == sequence) {
					timestamp = this.tilNextMillis(this.lastTimestamp);
				}
			} else {
				this.sequence = 0;
			}
			if (timestamp < this.lastTimestamp) {
				throw new RuntimeException(
						String.format(
								"clock moved backwards. refusing to generate id for %d milliseconds.",
								this.lastTimestamp - timestamp));
			}
			lastTimestamp = timestamp;
			id = ((timestamp - this.epoch) << this.timestampLeftShift)
					| (this.datacenterId << this.datacenterIdShift)
					| (this.workerId << this.workerIdShift) | this.sequence;

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			LOCK.unlock();
		}

		return id;
	}

	/**
	 * 
	 * @return
	 */
	public long getWorkerId() {

		return this.workerId;
	}

	/**
	 * 
	 * @return
	 */
	public long getDatacenterId() {

		return this.datacenterId;
	}

	/**
	 * 
	 * @return
	 */
	public long getTimestamp() {

		return System.currentTimeMillis();
	}

	/**
	 * 
	 * @param lastTimestamp
	 * @return
	 */
	private long tilNextMillis(long lastTimestamp) {

		long timestamp = this.timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = this.timeGen();
		}

		return timestamp;
	}

	/**
	 * 
	 * @return
	 */
	private long timeGen() {

		return System.currentTimeMillis();
	}
}
