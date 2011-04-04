package examples.copyonwriteset;

public interface CopyOnWriteSet {
	public boolean add(int value);
	
	public boolean remove(int value);
	
	public boolean contains(int value);
}
