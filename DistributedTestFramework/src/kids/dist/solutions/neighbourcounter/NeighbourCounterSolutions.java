package kids.dist.solutions.neighbourcounter;

import kids.dist.common.DistributedSystem;
import kids.dist.examples.neighbourcounter.NeighbourCounter;
import kids.dist.examples.neighbourcounter.NeighbourCounterTester;

public class NeighbourCounterSolutions {
	public static class NeighbourCounterSolutionNotWorking implements NeighbourCounter {
		DistributedSystem system;
		
		@Override
		public void pingNeighbours() {}
		
		@Override
		public int getNumberOfMessagesReceived() {
			return 0;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
	}
	
	public static class NeighbourCounterSolutionTrivial implements NeighbourCounter {
		DistributedSystem system;
		
		@Override
		public void pingNeighbours() {}
		
		@Override
		public int getNumberOfMessagesReceived() {
			return system.getProcessNeighbourhood().length;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
	}
	
	public static class NeighbourCounterSolutionExpected implements NeighbourCounter {
		DistributedSystem system;
		int count = 0;
		
		@Override
		public void pingNeighbours() {
			for (int neighbourId : system.getProcessNeighbourhood())
				system.sendMessage(neighbourId, 0, null);
		}
		
		@Override
		public int getNumberOfMessagesReceived() {
			return count;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			count++;
		}
	}
	
	public static void main(String[] args) {
		NeighbourCounterTester.testNeighbourCounter(NeighbourCounterSolutionExpected.class);
	}
}
