package examples.unsafereadwrite;

public interface UnsafeReadWriteLock {
	public void lockRead();
	
	public void unlockRead();
	
	public void lockWrite();
	
	public void unlockWrite();
}
