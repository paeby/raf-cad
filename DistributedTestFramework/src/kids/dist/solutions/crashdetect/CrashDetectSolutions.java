package kids.dist.solutions.crashdetect;

import java.util.Arrays;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.examples.crashdetect.CrashDetect;
import kids.dist.examples.crashdetect.CrashDetectTester;

public class CrashDetectSolutions {
	public static class NonWorkingCrashDetect implements CrashDetect {
		DistributedSystem system;
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
		
		@Override
		public int getCrashedId() {
			return system.getProcessId();
		}
	}
	
	public static class BcastingCrashDetect implements CrashDetect {
		DistributedSystem system;
		boolean received;
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (type == 0) {
				system.sendMessage(from, 1, null);
			} else {
				received = true;
			}
		}
		
		@Override
		public int getCrashedId() {
			while (true)
				for (int neighbour : system.getProcessNeighbourhood()) {
					received = false;
					system.sendMessage(neighbour, 0, null);
					long time = System.currentTimeMillis();
					while (!received) {
						system.yield();
						if (System.currentTimeMillis() - time > 100)
							return neighbour;
					}
				}
		}
	}
	
	public static class RoundRobinCrashDetect implements CrashDetect, InitiableSolution {
		DistributedSystem system;
		boolean received;
		int nextOne;
		int fallenId = -1;
		
		@Override
		public void initialize() {
			int myId = system.getProcessId();
			int[] neighborhood = system.getProcessNeighbourhood();
			nextOne = -Arrays.binarySearch(neighborhood, myId) - 1;
			if (nextOne < neighborhood.length)
				nextOne = neighborhood[nextOne];
			else
				nextOne = neighborhood[0];
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (type == 0) {
				system.sendMessage(from, 1, null);
			} else if (type == 1){
				received = true;
			} else if (type == 2) {
				fallenId = (Integer) message;
			}
		}
		
		@Override
		public int getCrashedId() {
			while (true) {
				if (fallenId != -1)
					return fallenId;
				received = false;
				system.sendMessage(nextOne, 0, null);
				long time = System.currentTimeMillis();
				while (!received) {
					system.yield();
					if (fallenId != -1)
						return fallenId;
					if (System.currentTimeMillis() - time > 100) {
						for (int neighbour : system.getProcessNeighbourhood())
							system.sendMessage(neighbour, 2, nextOne);
						return nextOne;
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		CrashDetectTester.testCrashDetect(RoundRobinCrashDetect.class);
	}
}
