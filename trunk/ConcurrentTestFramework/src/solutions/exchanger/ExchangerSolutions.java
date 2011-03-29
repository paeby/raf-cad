package solutions.exchanger;

import java.util.concurrent.atomic.AtomicReference;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.exchanger.Exchanger;
import examples.exchanger.ExchangerTester;

public class ExchangerSolutions {
	public static final class ExchangerNotWorking implements Exchanger {
		
		@Override
		public int exchange(int myint) {
			return 0;
		}
	}
	
	public static final class ExchangerBusyWait implements Exchanger {
		private final AtomicReference<Integer> value1 = new AtomicReference<Integer>(null);
		private final AtomicReference<Integer> value2 = new AtomicReference<Integer>(null);
		
		@Override
		public int exchange(int myint) {
			if (value1.compareAndSet(null, myint)) {
				while (value2.get() == null)
					Thread.yield();
				return value2.get();
			} else {
				value2.set(myint);
				return value1.get();
			}
		}
	}
	
	public static final class ExchangerPark implements Exchanger {
		private final AtomicReference<Integer> value1 = new AtomicReference<Integer>(null);
		private final AtomicReference<Integer> value2 = new AtomicReference<Integer>(null);
		private final AtomicReference<Thread> waitingThreadRef = new AtomicReference<Thread>(null);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public int exchange(int myint) {
			if (value1.compareAndSet(null, myint)) {
				waitingThreadRef.set(Thread.currentThread());
				unsafe.park(false, 0l);
				return value2.get();
			} else {
				value2.set(myint);
				Thread waitingThread;
				while ((waitingThread = waitingThreadRef.get()) == null)
					Thread.yield();
				while (waitingThread.getState() != Thread.State.WAITING)
					Thread.yield();
				unsafe.unpark(waitingThread);
				return value1.get();
			}
		}
	}
	
	public static void main(String[] args) {
		ExchangerTester.testExchanger(new ExchangerPark());
	}
}
