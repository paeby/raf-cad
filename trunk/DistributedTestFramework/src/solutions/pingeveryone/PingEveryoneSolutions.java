package solutions.pingeveryone;

import common.DistributedSystem;

import examples.pingeveryone.PingEveryone;
import examples.pingeveryone.PingEveryoneTester;

public class PingEveryoneSolutions {
	public static final class PingEveryoneSolution implements PingEveryone {
		private DistributedSystem system;
		private boolean hasBeenPinged = false;
		
		@Override
		public void pingNeighbourhood() {
			int[] neighbourhood = system.getProcessNeighbourhood();
			for (int i = 0; i < neighbourhood.length; i++) {
				system.sendMessage(neighbourhood[i], 0, "ping!");
			}
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			hasBeenPinged = true;
		}
		
		@Override
		public boolean hasBeenPinged() {
			return hasBeenPinged;
		}	
	}
	
	public static void main(String[] args) {
		PingEveryoneTester.testPingEveryone(PingEveryoneSolution.class);
	}
}
