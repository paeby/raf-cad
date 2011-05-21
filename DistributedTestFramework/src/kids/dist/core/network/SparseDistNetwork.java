package kids.dist.core.network;

import java.util.Random;

import kids.dist.util.RandomIntGenerator;

public class SparseDistNetwork implements DistNetwork {
	int[] ids;
	int[][] neighbourhoods;
	
	public SparseDistNetwork(int nodeCount, int density) {
		Random random = new Random();
		
		this.ids = RandomIntGenerator.generateDifferentInts(9000, nodeCount);
		for (int i = 0; i < ids.length; i++)
			ids[i] += 1000;
		
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
		neighbourhoods = new int[nodeCount][];
		for (int thisNode = 0; thisNode < nodeCount; thisNode++) {
			int count = 0;
			for (int otherNode = 0; otherNode < nodeCount; otherNode++)
				if (areNeighbours[thisNode][otherNode])
					count++;
			neighbourhoods[thisNode] = new int[count];
			count = 0;
			for (int otherNode = 0; otherNode < nodeCount; otherNode++)
				if (areNeighbours[thisNode][otherNode])
					neighbourhoods[thisNode][count++] = ids[otherNode];
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
		final int density;
		
		public Factory(int size, int density) {
			this.size = size;
			this.density = density;
		}
		
		@Override
		public SparseDistNetwork createRandomDistNetwork() {
			return new SparseDistNetwork(size, density);
		}
	}
}
