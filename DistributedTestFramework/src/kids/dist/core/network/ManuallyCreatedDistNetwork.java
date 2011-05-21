package kids.dist.core.network;

public class ManuallyCreatedDistNetwork implements DistNetwork {
	final int[] ids;
	final int[][] neighbourhoods;
	
	public ManuallyCreatedDistNetwork(int[] ids, int[][] neighbourhoods) {
		super();
		this.ids = ids;
		this.neighbourhoods = neighbourhoods;
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
		final int[] ids;
		final int[][] neighbourhoods;
		
		public Factory(int[] ids, int[][] neighbourhoods) {
			super();
			this.ids = ids;
			this.neighbourhoods = neighbourhoods;
		}
		
		@Override
		public ManuallyCreatedDistNetwork createRandomDistNetwork() {
			return new ManuallyCreatedDistNetwork(ids, neighbourhoods);
		}
	}
}
