package examples.unsafequeue;


public final class UnsafeQueueTester {
	public static void testUnsafeQueue(UnsafeQueue queue) {
		UnsafeQueueProblemInstance instance = new UnsafeQueueProblemInstance(1, 100);
		for (int i = 0; i < 10000; i++) {
			if (i % 200 == 0)
				System.out.print('.');
			if (!instance.testUnsafeQueueOnce(queue)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
}
