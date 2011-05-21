package kids.dist.core.network;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

import kids.dist.util.RandomIntGenerator;

public class KademliaDistNetwork implements DistNetwork {
	static Random RANDOM = new Random();
	int[][] neighbourhoods;
	int[] ids;
	
	public KademliaDistNetwork(int nodeCount, boolean knowEveryone) {
		{
			if (nodeCount % 2 != 0)
				throw new IllegalArgumentException("Number of nodes must be an even number");
			
			this.ids = new int[nodeCount];
			int[] generatedIds = RandomIntGenerator.generateDifferentInts(128, nodeCount/2);
			Arrays.sort(generatedIds);
			int i = 0;
			for (int generatedId: generatedIds) {
				this.ids[i++] = (generatedId << 1);
				this.ids[i++] = (generatedId << 1) | 0x01;
			}
		}
		
		neighbourhoods = new int[nodeCount][];
		if (knowEveryone) {
			for (int i = 0; i < nodeCount; i++) {
				neighbourhoods[i] = new int[nodeCount - 1];
				for (int j = 0; j < i; j++)
					neighbourhoods[i][j] = ids[j];
				for (int j = i; j < neighbourhoods[i].length; j++)
					neighbourhoods[i][j] = ids[j + 1];
			}
		} else {
			boolean[] idExists = new boolean[256];
			for (Integer id : ids)
				idExists[id] = true;
			
			for (int idIndex = 0; idIndex < ids.length; idIndex++) {
				int id = ids[idIndex];
				LinkedList<Integer> neighbours = new LinkedList<Integer>();
				for (int mask = 0x80; mask != 0x00; mask >>>= 1) {
					int firstPossibleId = createFirstPossibleId(id, mask);
					int lastPossibleId = createLastPossibleId(id, mask);
					
					int countExist = 0;
					for (int i = firstPossibleId; i <= lastPossibleId; i++)
						if (idExists[i])
							countExist++;
					
					if (countExist == 0)
						continue;
					
					if (lastPossibleId == firstPossibleId) {
						neighbours.add(firstPossibleId);
					} else {
						int node1 = RANDOM.nextInt(countExist), node2;
						do {
							node2 = RANDOM.nextInt(countExist);
						} while (node2 == node1);
						
						for (int i = firstPossibleId; i <= lastPossibleId; i++) {
							if (idExists[i]) {
								if (node1-- == 0) {
									neighbours.add(i);
								}
								if (node2-- == 0) {
									neighbours.add(i);
								}
							}
						}
					}
					
				}
				neighbourhoods[idIndex] = copyToIntArray(neighbours);
				Arrays.sort(neighbourhoods[idIndex]);
			}
		}
	}
	
	static int createFirstPossibleId(int id, int mask) {
		return (id ^ mask) & ~(mask - 1);
	}
	
	static int createLastPossibleId(int id, int mask) {
		return (id ^ mask) | (mask - 1);
	}
	
	static void testFirstLastId() {
		int randomId = RANDOM.nextInt(256);
		System.out.println(Integer.toBinaryString(randomId));
		
		for (int mask = 0x80; mask != 0x00; mask >>>= 1) {
			System.out.println(Integer.toBinaryString(mask) + ": " + Integer.toBinaryString(createFirstPossibleId(randomId, mask)) + " - " + Integer.toBinaryString(createLastPossibleId(randomId, mask)));
		}
	}
	
	@Override
	public int[] getPIds() {
		return ids;
	}
	
	@Override
	public int[][] getNeighborhoods() {
		return neighbourhoods;
	}
	
	public static final class Factory implements DistNetworkFactory {
		final int size;
		final boolean knowEveryone;
		
		public Factory(int size, boolean knowEveryone) {
			this.size = size;
			this.knowEveryone = knowEveryone;
		}
		
		@Override
		public KademliaDistNetwork createRandomDistNetwork() {
			return new KademliaDistNetwork(size, knowEveryone);
		}
	}
	
	static int[] copyToIntArray(Collection<Integer> collection) {
		int[] result = new int[collection.size()];
		int index = 0;
		for (int id : collection)
			result[index++] = id;
		return result;
	}
}
