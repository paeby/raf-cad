package examples.exchanger;


public class ExchangerTester {
	
	public static void testExchanger(Exchanger exchanger) {
		ExchangerProblemInstance instance = new ExchangerProblemInstance(500);
		for (int i = 0; i < 10000; i++) {
			if (i % 200 == 0)
				System.out.print('.');
			if (!instance.testExchangerOnce(exchanger)) {
				System.out.println("\nTest failed");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
	
}
