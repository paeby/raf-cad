package solutions.gatherer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
		
		final AtomicReferenceArray<Object> results = new AtomicReferenceArray<Object>(5);
		
		@Override
		public Object[] offer(int key, Object object) {
			results.set(key, object);
			Object[] localResults = new Object[5];
			for (int i = 0; i < 5; i++) {
				while (results.get(i) == null)
					Thread.yield();
				localResults[i] = results.get(i);
			}
			return localResults;
		}
	}
	
	public static final class GathererPark implements Gatherer {
		
		private final AtomicReferenceArray<Object> results = new AtomicReferenceArray<Object>(5);
		private final AtomicReferenceArray<Thread> parkedThreads = new AtomicReferenceArray<Thread>(5);
		private final AtomicInteger resultsRemaining = new AtomicInteger(5);
		private final AtomicReference<Object[]> returnValue = new AtomicReference<Object[]>(null);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public Object[] offer(int key, Object object) {
			results.set(key, object);
			parkedThreads.set(key, Thread.currentThread());
			
			if (resultsRemaining.decrementAndGet() == 0) {
				Object[] returnValue = new Object[5];
				for (int i = 0; i < 5; i++)
					returnValue[i] = results.get(i);
				this.returnValue.set(returnValue);
				for (int i = 0; i < 5; i++)
					if (i != key) {
						while (parkedThreads.get(i).getState() != Thread.State.WAITING)
							Thread.yield();
						unsafe.unpark(parkedThreads.get(i));
					}
			} else
				unsafe.park(false, 0l);
			
			return returnValue.get();
		}
	}
	
	public static void main(String[] args) {
		GathererTester.testGatherer(new GathererPark());
	}
}
