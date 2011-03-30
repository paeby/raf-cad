package examples.unsafequeue;


public final class UnsafeQueueTester {
	public static void testUnsafeQueue(UnsafeQueue modelQueue) {
		UnsafeQueueProblemInstance instance = new UnsafeQueueProblemInstance(3, 100);
		for (int i = 0; i < 10000; i++) {
			if (i % 200 == 0)
				System.out.print('.');
			UnsafeQueue queue;
			try {
				queue = modelQueue.getClass().newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Klasa mora imati public default konstruktor da bi test radio", e);
			}
			if (!instance.testUnsafeQueueOnce(queue)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
}
