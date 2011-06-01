package kids.dist.seminarski2;

import kids.dist.common.problem.ProblemInstance;
import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.DistNetworkFactory;
import kids.dist.core.network.KademliaDistNetwork;

public class DistributedHashTableTester {
	public static void testDHT(Class<? extends DistributedHashTable> solution, int numberOfBits, int numberOfThreads, boolean testObjectOverwrite, boolean testKademlia, boolean testCrashSafety) {
		if (numberOfThreads < 1)
			throw new IllegalArgumentException("Number of threads cannot be less then one: " + numberOfThreads);
		if (numberOfBits < 1)
			throw new IllegalArgumentException("Number of bits cannot be less then one: " + numberOfBits);

		if (numberOfThreads > 1000 || numberOfBits >= 16)
			throw new IllegalArgumentException("Ne budi lud");
		
		int count = 1;
		while (numberOfBits > 0) {
			count <<= 1;
			numberOfBits--;
		}
		
		if (numberOfThreads > count)
			throw new IllegalArgumentException("The number of threads cannot be greater then the count of possible ids");

		System.out.println("Testing DHT");
		int numberOfOptions = 0;
		System.out.println("  Object overwrite: " + testObjectOverwrite);
		if (testObjectOverwrite) {
			numberOfOptions++;
		}
		System.out.println("  Kademlia:         " + testKademlia);
		if (testKademlia) {
			numberOfOptions++;
		}
		System.out.println("  Crash-safety:     " + testCrashSafety);
		if (testCrashSafety) {
			numberOfOptions++;
		}
		System.out.println("Potential points:   " + (numberOfOptions == 0 ? 4 : (2 + numberOfOptions * 6)));
		System.out.println();

		DistNetworkFactory factory = new KademliaDistNetwork.Factory(numberOfThreads, !testKademlia);
		ProblemInstance<DistributedHashTable> problemInstance = new DistributedHashTableProblemInstance(count, numberOfThreads, testObjectOverwrite, testCrashSafety);
		ProblemTester.testProblem(problemInstance, solution, factory, 200, false, true);
	}

	public static void testDHT(Class<? extends DistributedHashTable> solution, boolean testObjectOverwrite, boolean testKademlia, boolean testCrashSafety) {
		testDHT(solution, 8, 16, testObjectOverwrite, testKademlia, testCrashSafety);
	}
}
