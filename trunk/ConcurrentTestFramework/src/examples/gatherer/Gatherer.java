package examples.gatherer;


/**
 * Pretpostavljamo da postoji uvek fiksnih 5 niti koji koriste deljeni Gatherer
 * objekat.
 */
public interface Gatherer {
	public Object[] offer(int key, Object object);
}
