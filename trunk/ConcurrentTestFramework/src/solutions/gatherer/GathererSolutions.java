package solutions.gatherer;

import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.gatherer.Gatherer;
import examples.gatherer.GathererTester;

public class GathererSolutions {
	public static final class GathererDoesntWork implements Gatherer {
		@Override
		public Object[] offer(int key, Object object) {
			return null;
		}
	}
	
	public static final class GathererBusyWait implements Gatherer {
		
		final Object[] results = new Object[5];
		
		@Override
		public Object[] offer(int key, Object object) {
			results[key] = object;
			for (int i = 0; i < 5; i++)
				while (results[i] == null)
					Thread.yield();
			return results;
		}
	}
	
	public static final class GathererPark implements Gatherer {
		
		final Object[] results = new Object[5];
		final AtomicInteger resultsRemaining = new AtomicInteger(5);
		final Thread[] parkedThreads = new Thread[5];
		final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public Object[] offer(int key, Object object) {
			results[key] = object;
			parkedThreads[key] = Thread.currentThread();
			
			if (resultsRemaining.decrementAndGet() == 0) {
				for (int i = 0; i < 5; i++)
					if (i != key) {
						while (parkedThreads[i].getState() != Thread.State.WAITING)
							Thread.yield();
						unsafe.unpark(parkedThreads[i]);
					}
			} else
				unsafe.park(false, 0l);
			
			return results;
		}
	}
	
	public static void main(String[] args) {
		GathererTester.testGatherer(new GathererPark());
	}
}
