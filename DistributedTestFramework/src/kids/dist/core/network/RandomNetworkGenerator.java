package kids.dist.core.network;

import java.util.Random;

public class RandomNetworkGenerator {
	public static int[][] generateNeighbourhoodInfos(int nodeCount, int density) {
		Random random = new Random();
		boolean[][] areNeighbours = new boolean[nodeCount][];
		for (int i = 0; i < nodeCount; i++) {
			areNeighbours[i] = new boolean[nodeCount];
		}
		for (int thisNode = 0; thisNode < nodeCount; thisNode++) {
			int count = 0;
			for (int i = 0; i < thisNode; i++)
				if (random.nextInt(100) < density) {
					areNeighbours[thisNode][i] = true;
					areNeighbours[i][thisNode] = true;
					count++;
				}
			if (count == 0 && thisNode > 0) {
				int forcedNeighbour = random.nextInt(thisNode);
				areNeighbours[thisNode][forcedNeighbour] = true;
				areNeighbours[forcedNeighbour][thisNode] = true;
			}
		}
		int[][] neighbourhoods = new int[nodeCount][];
		for (int thisNode = 0; thisNode < nodeCount; thisNode++) {
			int count = 0;
			for (int otherNode = 0; otherNode < nodeCount; otherNode++)
				if (areNeighbours[thisNode][otherNode])
					count++;
			neighbourhoods[thisNode] = new int[count];
			count = 0;
			for (int otherNode = 0; otherNode < nodeCount; otherNode++)
				if (areNeighbours[thisNode][otherNode])
					neighbourhoods[thisNode][count++] = otherNode;
		}
		return neighbourhoods;
	}
	
	public static int[][] generateCliqueInfos(int nodeCount) {
		int[][] results = new int[nodeCount][];
		for (int thisNode = 0; thisNode < nodeCount; thisNode++) {
			results[thisNode] = new int[nodeCount - 1];
			for (int i = 0; i < thisNode;)
				results[thisNode][i] = i++;
			for (int i = thisNode; i < nodeCount - 1;)
				results[thisNode][i++] = i;
		}
		return results;
	}
}
