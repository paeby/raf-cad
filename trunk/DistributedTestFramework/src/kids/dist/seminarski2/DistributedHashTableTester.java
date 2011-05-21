package kids.dist.seminarski2;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.DistNetworkFactory;
import kids.dist.core.network.KademliaDistNetwork;

public class DistributedHashTableTester {
	public static void testDHT(Class<? extends DistributedHashTable> solution, boolean testObjectOverwrite, boolean testKademlia, boolean testCrashSafety) {
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
		
		DistNetworkFactory factory = new KademliaDistNetwork.Factory(16, !testKademlia);
		ProblemTester.testProblem(new DistributedHashTableProblemInstance(testObjectOverwrite, testCrashSafety), solution, factory, 200, false, true);
	}
}
