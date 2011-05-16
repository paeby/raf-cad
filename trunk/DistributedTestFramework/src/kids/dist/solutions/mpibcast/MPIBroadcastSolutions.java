package kids.dist.solutions.mpibcast;

import java.util.Arrays;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.examples.mpibcast.MPIBroadcast;
import kids.dist.examples.mpibcast.MPIBroadcastTester;

public class MPIBroadcastSolutions {
	public static class MPIBroadcastImpl implements MPIBroadcast, InitiableSolution {
		DistributedSystem system;
		Object message;
		int[] neighbourhood;
		int myIndex = -1;
		
		@Override
		public void initialize() {
			neighbourhood = system.getProcessNeighbourhood();
			myIndex = -Arrays.binarySearch(neighbourhood, system.getProcessId()) - 1;
		}
		
		@Override
		public void broadcast(Object message) {
			messageReceived(-1, system.getProcessNeighbourhood().length + 1, message);
		}
		
		@Override
		public Object getBroadcastedMessage() {
			return message;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			this.message = message;
			while (type > 1) {
				type /= 2;
				if (myIndex + type <= neighbourhood.length)
					system.sendMessage(neighbourhood[myIndex + type - 1], type, message);
				else
					system.sendMessage(neighbourhood[myIndex + type - neighbourhood.length - 1], type, message);
				messageReceived(-1, type, message);
			}
		}
	}
	
	public static void main(String[] args) {
		MPIBroadcastTester.testMPIBroadcast(MPIBroadcastImpl.class);
	}
}
