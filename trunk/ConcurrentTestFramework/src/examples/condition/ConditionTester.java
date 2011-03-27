package examples.condition;


public class ConditionTester {
	
	public static void testCondition(Condition condition) {
		ConditionProblemInstance instance = new ConditionProblemInstance(20, 2);
		for (int i = 0; i < 200; i++) {
			if (i % 10 == 0)
				System.out.print('.');
			if (!instance.testConditionOnce(condition)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
	
}
