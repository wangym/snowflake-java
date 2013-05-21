/**
 * 
 */
package me.yumin.java.snowflake;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author yumin
 * 
 */
public class IdWorkerTest {

	/**
	 * 
	 */
	private final long workerMask = 0x000000000001F000L;
	private final long datacenterMask = 0x00000000003E0000L;

	@Test
	public void testGenerateAnId() {

		IdWorker s = new IdWorker(1L, 1L);
		long id = s.nextId();

		Assert.assertTrue(0L < id);
	}

	@Test
	public void testReturnAnAccurateTimestamp() {

		IdWorker s = new IdWorker(1L, 1L);
		long t = System.currentTimeMillis();

		Assert.assertTrue(50L > s.getTimestamp() - t);
	}

	@Test
	public void testReturnTheCorrectJobId() {

		IdWorker s = new IdWorker(1L, 1L);

		Assert.assertTrue(1L == s.getWorkerId());
	}

	@Test
	public void testReturnTheCorrectDcId() {

		IdWorker s = new IdWorker(1L, 1L);

		Assert.assertTrue(1L == s.getDatacenterId());
	}

	@Test
	public void testProperlyMaskWorkerId() {

		long workerId = 0x1FL;
		long datacenterId = 0L;
		IdWorker worker = new IdWorker(workerId, datacenterId);
		for (int i = 1; i <= 100; i++) {
			long id = worker.nextId();
			Assert.assertTrue(((id & workerMask) >> 12) == (workerId));
		}
	}

	@Test
	public void testProperlyMaskDcId() {

		long workerId = 0L;
		long datacenterId = 0x1FL;
		IdWorker worker = new IdWorker(workerId, datacenterId);
		long id = worker.nextId();
		Assert.assertTrue(((id & datacenterMask) >> 17) == (datacenterId));
	}
}
