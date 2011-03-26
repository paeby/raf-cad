package solutions.exchanger;

import java.util.concurrent.atomic.AtomicInteger;
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
		private final AtomicInteger value1 = new AtomicInteger(-1);
		private final AtomicInteger value2 = new AtomicInteger(-1);
		
		@Override
		public int exchange(int myint) {
			if (value1.compareAndSet(-1, myint)) {
				while (value2.get() == -1)
					Thread.yield();
				return value2.get();
			} else {
				value2.set(myint);
				return value1.get();
			}
		}
	}
	
	public static final class ExchangerPark implements Exchanger {
		private final AtomicInteger value1 = new AtomicInteger(-1);
		private final AtomicInteger value2 = new AtomicInteger(-1);
		private final AtomicReference<Thread> waitingThreadRef = new AtomicReference<Thread>(null);
		
		@Override
		public int exchange(int myint) {
			Unsafe unsafe = UnsafeHelper.getUnsafe();
			if (value1.compareAndSet(-1, myint)) {
				waitingThreadRef.set(Thread.currentThread());
				unsafe.park(false, 0l);
				return value2.get();
			} else {
				value2.set(myint);
				Thread waitingThread;
				while ((waitingThread = waitingThreadRef.get()) == null)
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
