package kids.dist.solutions.consensus;

import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.examples.consensus.Consensus;
import kids.dist.examples.consensus.ConsensusTester;

public class ConsensusSolutions {
	public static class DummyConsensus implements Consensus {
		DistributedSystem system;
		int value;
		
		@Override
		public int propose(int value) {
			this.value = value;
			return value;
		}
		
		public void messageReceived(int from, int type, Object message) {

		}
	}
	
	public static class CheatingConsensus implements Consensus, InitiableSolution {
		DistributedSystem system;
		static int value, n = 0;
		
		@Override
		public void initialize() {
			if ((n++ % (system.getProcessNeighbourhood().length + 1)) == 0)
				CheatingConsensus.value = Integer.MIN_VALUE;
		}
		
		@Override
		public int propose(int value) {
			if (CheatingConsensus.value == Integer.MIN_VALUE)
				CheatingConsensus.value = value;
			return CheatingConsensus.value;
		}
		
		public void messageReceived(int from, int type, Object message) {}
	}
	
	public static class MustBeWorkingConsensus implements Consensus, InitiableSolution {
		DistributedSystem system;
		int value = Integer.MAX_VALUE;
		int receivedMsgs;
		
		public void initialize() {
			this.receivedMsgs = system.getProcessNeighbourhood().length + 1;
		}
		
		@Override
		public int propose(int value) {
			if (this.value > value) {
				this.value = value;
			}
			receivedMsgs--;
			
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, value);
			
			while (receivedMsgs > 0)
				system.yield();
			
			return this.value;
		}
		
		public void messageReceived(int from, int type, Object message) {
			if (value > (Integer) message)
				value = (Integer) message;
			receivedMsgs--;
		}
	}
	
	public static class CoinFlipConsensus implements Consensus, InitiableSolution {
		DistributedSystem system;
		TreeMap<Integer, Integer> alives = new TreeMap<Integer, Integer>();
		LinkedList<TreeMap<Integer, Integer>> receivedForTheNextRounds = new LinkedList<TreeMap<Integer, Integer>>();
		Random random = new Random();
		int awaitingMessages;
		int round = 0;
		
		@Override
		public void initialize() {
			awaitingMessages = system.getProcessNeighbourhood().length;
		}
		
		@Override
		public int propose(int value) {
			int myId = system.getProcessId();
			boolean amIAlive;
			if (amIAlive = alives.isEmpty())
				alives.put(myId, value);
			
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, round, amIAlive ? value : null);
			
			while (awaitingMessages > 0)
				system.yield();
			
			int numOfAlives = alives.size();
			while (true) {
				if (alives.size() == 1)
					return alives.firstEntry().getValue();
				
				round++;
				
				if (alives.size() > 0)
					numOfAlives = alives.size();
				awaitingMessages = numOfAlives;
				
				if (!receivedForTheNextRounds.isEmpty())
					for (Map.Entry<Integer, Integer> entry : receivedForTheNextRounds.removeFirst().entrySet()) {
						awaitingMessages--;
						if (entry.getValue() == null)
							alives.remove(entry.getKey());
						else
							alives.put(entry.getKey(), entry.getValue());
					}
				
				if (amIAlive) {
					awaitingMessages--;
					if (random.nextInt(numOfAlives) == 0) {
						alives.put(myId, value);
						for (int neighbour : system.getProcessNeighbourhood())
							system.sendMessage(neighbour, round, value);
					} else {
						alives.remove(myId);
						for (int neighbour : system.getProcessNeighbourhood())
							system.sendMessage(neighbour, round, null);
					}
				}
				
				while (awaitingMessages > 0)
					system.yield();
				
				if (!alives.isEmpty() && !alives.containsKey(myId))
					amIAlive = false;
			}
		}
		
		public void messageReceived(int from, int type, Object message) {
			if (type == round) {
				if (message == null)
					alives.remove(from);
				else
					alives.put(from, (Integer) message);
				awaitingMessages--;
			} else if (type > round) {
				int index = type - round - 1;
				if (receivedForTheNextRounds.size() == index)
					receivedForTheNextRounds.add(new TreeMap<Integer, Integer>());
				receivedForTheNextRounds.get(index).put(from, (Integer) message);
			} else
				throw new IllegalStateException("Type < round? " + type + " " + round);
		}
	}
	
	public static void main(String[] args) {
		ConsensusTester.testConsensusInClique(CoinFlipConsensus.class);
	}
}
