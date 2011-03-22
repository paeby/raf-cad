package examples.blockingqueue;


public final class BlockingQueueTester {
	public static void testBlockingQueue(BlockingQueue queue) {
		BlockingQueueProblemInstance instance = new BlockingQueueProblemInstance(1, 100);
		for (int i = 0; i < 10000; i++) {
			if (i % 200 == 0)
				System.out.print('.');
			if (!instance.testBlockingQueueOnce(queue)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
}
