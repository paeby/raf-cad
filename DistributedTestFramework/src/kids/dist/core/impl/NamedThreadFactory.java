package kids.dist.core.impl;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class NamedThreadFactory implements ThreadFactory {			
	private final AtomicInteger count = new AtomicInteger(0);
	
	private final String namePrefix;
	private final boolean daemon;
	
	public NamedThreadFactory(String namePrefix)
	{
		this(namePrefix, true);
	}

	public NamedThreadFactory(String namePrefix, boolean daemon)
	{
		this.namePrefix = namePrefix;
		this.daemon = daemon;
	}
	
	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = new Thread(r, namePrefix + count.incrementAndGet());
		t.setDaemon(daemon);
		return t;
	}
}
