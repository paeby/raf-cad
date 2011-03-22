package examples.blockingqueue;


public final class BlockingQueueTester {
	public static void testBlockingQueue(BlockingQueue queue) {
		BlockingQueueProblemInstance instance = new BlockingQueueProblemInstance(3, 100);
		for (int i = 0; i < 1000; i++) {
			System.out.print('.');
			if (!instance.testBlockingQueueOnce(queue)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
}
