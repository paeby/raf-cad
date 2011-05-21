package kids.dist.solutions.seminarski2.test;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.DistNetworkFactory;
import kids.dist.core.network.KademliaDistNetwork;

public class KademliaNodeFinderSolution implements KademliaNodeFinder, InitiableSolution {
	DistributedSystem system;
	int myId;
	int[] neighbourhood;
	List<Integer>[] buckets;
	int requestIdDispenser = 0;
	
	TreeSet<Integer> waitingForAnswersFrom = new TreeSet<Integer>();
	TreeSet<Integer> closest;
	TreeSet<Integer> deadIds = new TreeSet<Integer>();
	
	@Override
	@SuppressWarnings("unchecked")
	public void initialize() {
		myId = system.getProcessId();
		neighbourhood = system.getProcessNeighbourhood();
		
		buckets = new List[8];
		for (int i = 0; i < 8; i++) {
			buckets[i] = new LinkedList<Integer>();
		}
		buckets[0].add(myId);
		for (int neighbour : neighbourhood) {
			int bucketIndex = getBestBucketIndex(neighbour);
			buckets[bucketIndex].add(neighbour);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void messageReceived(int from, int type, Object message) {
		if (type == 0)
			system.sendMessage(from, 1, getBestMatchingIdsForId((Integer) message));
		else if (type == 1) {
			closest.addAll((Collection<Integer>) message);
			waitingForAnswersFrom.remove(from);
		} else if (type == -1) {
			int deadId = (Integer) message;
			if (!deadIds.add(deadId)) {
				buckets[getBestBucketIndex(deadId)].remove(deadId);
				for (int neighbour : neighbourhood)
					system.sendMessage(neighbour, -1, deadId);
			}
		}
	}
	
	@Override
	public int findNodeClosestTo(int id) {
		//		System.out.println("TRAZHIM!");
		//		System.out.println("myId:          " + its(myId));
		//		System.out.println("searching for: " + its(id));
		//		System.out.println(" *** ");
		//		for (int i = 0; i < buckets.length; i++) {
		//			System.out.print("Bucket #" + i + ": ");
		//			for (int bucketValue : buckets[i])
		//				System.out.print(its(bucketValue) + " ");
		//			System.out.println();
		//		}
		//		System.out.println(" *** ");
		
		List<Integer> closestList = getBestMatchingIdsForId(id);
		if (closestList.size() == 1)
			return closestList.get(0);
		
		Comparator<Integer> comparatorForThisId = createKademliaDistanceComparator(id);
		closest = new TreeSet<Integer>(comparatorForThisId);
		closest.addAll(closestList);
		
		int oldBestAnswer = -1;
		while (!closest.first().equals(oldBestAnswer)) {
			//			System.out.print("Closest so far: ");
			//			for (int closestValue : closest)
			//				System.out.print(its(closestValue) + " ");
			//			System.out.println("for prev best " + oldBestAnswer);
			
			oldBestAnswer = closest.first();
			
			waitingForAnswersFrom.clear();
			waitingForAnswersFrom.addAll(closest);
			
			for (int closestId : closest)
				if (closestId == myId)
					waitingForAnswersFrom.remove(myId);
				else
					system.sendMessage(closestId, 0, id);
			
			long startingTime = System.currentTimeMillis();
			while (!waitingForAnswersFrom.isEmpty()) {
				if (System.currentTimeMillis() - startingTime > 100) {
					for (int stillWaitingId : waitingForAnswersFrom) {
						int bucketIndex = getBestBucketIndex(stillWaitingId);
						buckets[bucketIndex].remove(stillWaitingId);
						deadIds.add(stillWaitingId);
						for (int neighbour : neighbourhood)
							system.sendMessage(neighbour, -1, stillWaitingId);
					}
					break;
				}
				system.yield();
			}
			
			while (closest.size() > 2)
				closest.pollLast();
		}
		return closest.first();
	}
	
	List<Integer> getBestMatchingIdsForId(int id) {
		int bucketIndex = 0;
		while (true) {
			bucketIndex = 0;
			int difference = id ^ myId;
			while ((difference >>>= 1) > 0)
				bucketIndex++;
			
			if (!buckets[bucketIndex].isEmpty())
				break;
			
			int mask = 1;
			while (bucketIndex-- > 0)
				mask <<= 1;
			id ^= mask;
		}
		
		return buckets[bucketIndex];
		//		if (bucketIndex == 0) {
		//			return new int[] { ((myId ^ id) < (myId ^ 1 ^ id)) ? myId : (myId ^ 1) };
		//		} else {
		//			return buckets[bucketIndex];
		//		}
	}
	
	int getBestBucketIndex(int forId) {
		int difference = forId ^ myId;
		int bucketIndex = 0;
		while ((difference >>>= 1) > 0) {
			bucketIndex++;
		}
		return bucketIndex;
	}
	
	static Comparator<Integer> createKademliaDistanceComparator(final int id) {
		return new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return (o1 ^ id) - (o2 ^ id);
			}
		};
	}
	
	static String its(int id) {
		String s = Integer.toBinaryString(id);
		while (s.length() < 8)
			s = '0' + s;
		return s;
	}
	
	public static void main(String[] args) {
		DistNetworkFactory factory = new KademliaDistNetwork.Factory(8, false);
		
		//		int[] ids = new int[] { 30, 31, 46, 47, 48, 49, 120, 121 };
		//		int[][] neighbourhoods = new int[8][];
		//		neighbourhoods[0] = new int[] { 31, 48, 49, 120, 121 };
		//		neighbourhoods[1] = new int[] { 30, 46, 47, 120, 121 };
		//		neighbourhoods[2] = new int[] { 30, 31, 47, 48, 49, 120, 121 };
		//		neighbourhoods[3] = new int[] { 30, 31, 46, 48, 49, 120, 121 };
		//		neighbourhoods[4] = new int[] { 30, 31, 46, 47, 49, 120, 121 };
		//		neighbourhoods[5] = new int[] { 30, 31, 46, 47, 48, 120, 121 };
		//		neighbourhoods[6] = new int[] { 30, 31, 121 };
		//		neighbourhoods[7] = new int[] { 30, 31, 120 };
		//		za myid 47, lookingfor 176
		//		DistNetworkFactory factory = new ManuallyCreatedDistNetwork.Factory(ids, neighbourhoods);
		
		ProblemTester.testProblem(new KademliaNodeFinderProblemInstance(true), KademliaNodeFinderSolution.class, factory, 800, false, true);
	}
}
