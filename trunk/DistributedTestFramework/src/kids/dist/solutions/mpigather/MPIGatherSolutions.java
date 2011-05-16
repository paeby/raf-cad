package kids.dist.solutions.mpigather;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.examples.mpigather.MPIGather;
import kids.dist.examples.mpigather.MPIGatherTester;

public class MPIGatherSolutions {
	public static class MPIGatherImpl implements MPIGather, InitiableSolution {

		DistributedSystem system;
		int[] neighbourhood;
		int myIndex = -1;
		int waitingFor;
		TreeMap<Integer, Object> myPackage = new TreeMap<Integer, Object>();

		@Override
		public void initialize() {
			neighbourhood = system.getProcessNeighbourhood();
			myIndex = -Arrays
					.binarySearch(neighbourhood, system.getProcessId()) - 1;

			waitingFor = 1;
			int x = neighbourhood.length + 1;
			while (x > 1) {
				if (myIndex % x == 0)
					waitingFor++;
				x /= 2;
			}
			System.out.println(myIndex + " " + waitingFor);
		}

		private void sendMyPackage() {
			system.sendMessage(neighbourhood[myIndex - myPackage.size() + 1],
					0, myPackage);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void messageReceived(int from, int type, Object message) {
			TreeMap<Integer, Object> receivedPackage = (TreeMap<Integer, Object>) message;
			for (Map.Entry<Integer, Object> elem : receivedPackage.entrySet()) {
				myPackage.put(elem.getKey(), elem.getValue());
			}
			if (waitingFor-- == 1)
				sendMyPackage();
		}

		@Override
		public void offer(int index, Object object) {
			myPackage.put(index, object);
			if (system.getProcessId() > neighbourhood[0] && waitingFor-- == 1)
				sendMyPackage();
		}

		@Override
		public Object[] gather() {
			while (myPackage.size() < neighbourhood.length + 1)
				system.yield();
			Object[] results = new Object[neighbourhood.length + 1];
			for (Map.Entry<Integer, Object> entry : myPackage.entrySet())
				results[entry.getKey()] = entry.getValue();
			return results;
		}
	}

	public static void main(String[] args) {
		MPIGatherTester.testGather(MPIGatherImpl.class);
	}
}
