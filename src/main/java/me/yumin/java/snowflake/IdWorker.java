/** Copyright 2010-2012 Twitter, Inc.*/
package me.yumin.java.snowflake;

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
	private final long twepoch = 1288834974657L;

	private final long workerIdBits = 5L;
	private final long datacenterIdBits = 5L;
	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	private final long sequenceBits = 12L;

	private final long workerIdShift = sequenceBits;
	private final long datacenterIdShift = sequenceBits + workerIdBits;
	private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
	private final long sequenceMask = -1L ^ (-1L << sequenceBits);

	private long lastTimestamp = -1L;

	/**
	 * 
	 */
	private long workerId = 0;
	private long datacenterId = 0;
	private long sequence = 0L;

	/**
	 * 
	 * @param workerId
	 * @param datacenterId
	 */
	public IdWorker(long workerId, long datacenterId) {

		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("workerId can't be greater than %d or less than 0.", maxWorkerId));
		}
		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("datacenterId can't be greater than %d or less than 0.", maxDatacenterId));
		}

		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	/**
	 * 
	 * @return
	 */
	public long getId() {

		long id = nextId();

		return id;
	}

	/**
	 * 
	 * @return
	 */
	public long getWorkerId() {

		return workerId;
	}

	/**
	 * 
	 * @return
	 */
	public long getDatacenterId() {

		return datacenterId;
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
	 * @return
	 */
	protected synchronized long nextId() {

		long id = 0L;

		long timestamp = timeGen();
		if (timestamp < lastTimestamp) {
			throw new RuntimeException(String.format("clock moved backwards. refusing to generate id for %d milliseconds.", lastTimestamp - timestamp));
		}
		if (timestamp == lastTimestamp) {
			sequence = (1 + sequence) & sequenceMask;
			if (0 == sequence) {
				timestamp = tilNextMillis(lastTimestamp);
			}
		} else {
			sequence = 0;
		}
		lastTimestamp = timestamp;
		id = ((timestamp - twepoch) << timestampLeftShift) | 
				(datacenterId << datacenterIdShift) | 
				(workerId << workerIdShift) | 
				sequence;

		return id;
	}

	/**
	 * 
	 * @param lastTimestamp
	 * @return
	 */
	protected long tilNextMillis(long lastTimestamp) {

		long timestamp = timeGen();

		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}

		return timestamp;
	}

	/**
	 * 
	 * @return
	 */
	protected long timeGen() {

		return System.currentTimeMillis();
	}
}
