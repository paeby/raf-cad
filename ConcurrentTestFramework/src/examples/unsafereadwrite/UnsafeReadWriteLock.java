package examples.unsafereadwrite;

public interface UnsafeReadWriteLock {
	public void readLock();

	public void readUnock();

	public void writeLock();

	public void writeUnlock();
}
