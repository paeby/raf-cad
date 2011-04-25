package kids.dist.solutions.nodecounter;

import kids.dist.common.DistributedSystem;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.examples.nodecounter.NodeCounter;
import kids.dist.examples.nodecounter.NodeCounterTester;

public class NodeCounterSolutions {
	public static class IncorrectNodeCounter implements NodeCounter {
		DistributedSystem system;
		
		@Override
		public int getNumberOfNodes() {
			return system.getProcessNeighbourhood().length + 1;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
	}
	
	public static class MustBeCorrectNodeCounter implements NodeCounter {
		DistributedManagedSystem system;
		
		@Override
		public int getNumberOfNodes() {
			return system.getNumberOfNodes();
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
	}
	
	public static void main(String[] args) {
		NodeCounterTester.testNodeCounter(MustBeCorrectNodeCounter.class);
	}
}
