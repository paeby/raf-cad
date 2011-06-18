package sem2.milosstankovicTTT;

public class KademliaUtil {

	private KademliaUtil() {
	}

	protected static int NUMBER_BITS = 8;

	public static int getDistance(int id, int wantedId) {
		return id ^ wantedId;
	}

	public static int getBucket(int id, int wantedId) {
		int dis = getDistance(id, wantedId);
		for (int i = NUMBER_BITS - 1; i >= 0; i--) {
			if ((dis & (1 << i)) != 0) {
				return i + 1;
			}
		}
		return 0;
	}
}
