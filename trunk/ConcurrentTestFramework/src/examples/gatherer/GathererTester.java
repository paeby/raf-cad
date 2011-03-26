package examples.gatherer;

public class GathererTester {
	
	public static void testGatherer(Gatherer gatherer) {
		GathererProblemInstance instance = new GathererProblemInstance(80);
		for (int i = 0; i < 10000; i++) {
			if (i % 200 == 0)
				System.out.print('.');
			if (!instance.testGathererOnce(gatherer)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
	
}
