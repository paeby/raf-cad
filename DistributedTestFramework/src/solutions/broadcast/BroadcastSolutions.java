package solutions.broadcast;

import common.DistributedSystem;

import examples.broadcast.Broadcast;
import examples.broadcast.BroadcastTester;

public class BroadcastSolutions {
	public static class NonworkingBroadcastSolution implements Broadcast {
		volatile Object msg;
		
		@Override
		public void broadcast(Object arg0) {
			msg = arg0;
		}
		
		@Override
		public Object getBroadcastedMessage() {
			return msg;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
	}
	
	public static class DirectBroadcastSolution implements Broadcast {
		DistributedSystem system;
		Object msg;
		
		@Override
		public void broadcast(Object message) {
			this.msg = message;
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, message);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (msg == null)
				msg = message;
			else
				throw new IllegalArgumentException();
		}
		
		@Override
		public Object getBroadcastedMessage() {
			return msg;
		}
	}
	
	public static class SimpleBroadcastSolution implements Broadcast {
		DistributedSystem system;
		Object msg;
		
		@Override
		public void broadcast(Object message) {
			this.msg = message;
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, message);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (msg == null) {
				msg = message;
				broadcast(msg);
			}
		}
		
		@Override
		public Object getBroadcastedMessage() {
			return msg;
		}
	}
	
	public static void main(String[] args) {
		BroadcastTester.testBroadcast(SimpleBroadcastSolution.class);
	}
}
