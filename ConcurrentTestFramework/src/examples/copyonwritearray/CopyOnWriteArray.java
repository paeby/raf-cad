package examples.copyonwritearray;

/**
 * Fiksni kapacitet od 100 elemenata, inicijalno sve nule.
 * 
 */
public interface CopyOnWriteArray {
	public int[] get();
	
	public void set(int index, int value);
}
