package kids.dist.solutions.twomanconsensus;

import java.util.Random;

import kids.dist.common.DistributedSystem;
import kids.dist.examples.twomanconsensus.TwoManConsensus;
import kids.dist.examples.twomanconsensus.TwoManConsensusTester;

public class TwoManConsensusSolutions {
	public static class DummyTwoManConsensus implements TwoManConsensus {
		DistributedSystem system;
		Integer value = null;
		
		@Override
		public int propose(int proposedValue) {
			if (value == null) {
				value = proposedValue;
				system.sendMessage(system.getProcessNeighbourhood()[0], 0, proposedValue);
			}
			return value;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (value == null)
				value = (Integer) message;
		}
	}
	
	public static class CoinflipTwoManConsensus implements TwoManConsensus {
		
		DistributedSystem system;
		Integer myValue = null, otherValue = null, myProposal;
		Random random = new Random();
		
		@Override
		public int propose(int proposedValue) {
			if (myValue != null)
				return myValue;
			
			myValue = proposedValue;
			myProposal = myValue;
			
			system.sendMessage(system.getProcessNeighbourhood()[0], 0, proposedValue);
			while (otherValue == null)
				system.yield();
			while (myValue != otherValue)
				system.yield();
			return myValue;
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (myValue == null) {
				// Prihvatam
				myValue = otherValue = myProposal = (Integer) message;
				system.sendMessage(from, 0, message);
			} else {
				if (otherValue == null)
					otherValue = (Integer) message;
				if (myProposal.equals(message))
					myValue = otherValue = myProposal;
				else {
					myProposal = random.nextBoolean() ? myValue : otherValue;
					system.sendMessage(from, 0, myProposal);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		TwoManConsensusTester.testTwoManConsensus(CoinflipTwoManConsensus.class);
	}
}
