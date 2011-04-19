package kids.dist.solutions.pingeveryone;

import kids.dist.common.DistributedSystem;
import kids.dist.examples.pingeveryone.PingEveryone;
import kids.dist.examples.pingeveryone.PingEveryoneTester;

public class PingEveryoneSolutions {
	public static final class PingEveryoneSolution implements PingEveryone {
		private DistributedSystem system;
		private int hasBeenPinged = 0;
		
		@Override
		public void pingNeighbourhood() {
			int[] neighbourhood = system.getProcessNeighbourhood();
			for (int i = 0; i < 10; i++)
				for (int j = 0; j < neighbourhood.length; j++) {
					system.sendMessage(neighbourhood[j], 0, null);
				}
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			hasBeenPinged++;
		}
		
		@Override
		public int hasBeenPinged() {
			return hasBeenPinged;
		}
	}
	
	public static void main(String[] args) {
		PingEveryoneTester.testPingEveryone(PingEveryoneSolution.class);
	}
}
