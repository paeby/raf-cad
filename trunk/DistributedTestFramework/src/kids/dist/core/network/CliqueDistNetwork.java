package kids.dist.core.network;

import java.util.Arrays;

import kids.dist.util.RandomIntGenerator;

public class CliqueDistNetwork implements DistNetwork {
	int[] ids;
	int[][] neighbourhoods;
	
	public CliqueDistNetwork(int size) {
		this.ids = RandomIntGenerator.generateDifferentInts(9000, size);
		for (int i = 0; i < ids.length; i++)
			ids[i] += 1000;
		Arrays.sort(this.ids);
		
		neighbourhoods = new int[size][];
		for (int thisNode = 0; thisNode < size; thisNode++) {
			neighbourhoods[thisNode] = new int[size - 1];
			for (int i = 0; i < thisNode;)
				neighbourhoods[thisNode][i] = ids[i++];
			for (int i = thisNode; i < size - 1;)
				neighbourhoods[thisNode][i++] = ids[i];
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
		
		public Factory(int size) {
			this.size = size;
		}
		
		@Override
		public CliqueDistNetwork createRandomDistNetwork() {
			return new CliqueDistNetwork(size);
		}
	}
}
