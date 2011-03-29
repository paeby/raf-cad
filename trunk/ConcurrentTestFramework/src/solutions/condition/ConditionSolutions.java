package solutions.condition;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.condition.Condition;
import examples.condition.ConditionTester;

public class ConditionSolutions {
	public static final class ConditionNotWorking implements Condition {
		@Override
		public void myWait() {}
		
		@Override
		public void myNotify() {}
		
		@Override
		public void myNotifyAll() {}
	}
	
	public static final class ConditionPositivelyWorking implements Condition {
		@Override
		public void myWait() {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		@Override
		public void myNotify() {
			synchronized (this) {
				notify();
			}
		}
		
		@Override
		public void myNotifyAll() {
			synchronized (this) {
				notifyAll();
			}
		}
	}
	
	public static final class ConditionBusyWaitNoStarvation implements Condition {
		private final AtomicInteger state = new AtomicInteger(0);
		private final AtomicInteger numOfWaitingThreads = new AtomicInteger(0);
		
		@Override
		public void myWait() {
			while (state.get() < 0)
				Thread.yield();
			numOfWaitingThreads.incrementAndGet();
			while (state.get() >= 0 && !state.compareAndSet(1, 0))
				Thread.yield();
			numOfWaitingThreads.decrementAndGet();
		}
		
		@Override
		public void myNotify() {
			while (!state.compareAndSet(0, 1))
				Thread.yield();
		}
		
		@Override
		public void myNotifyAll() {
			while (!state.compareAndSet(0, -1))
				Thread.yield();
			while (numOfWaitingThreads.get() > 0)
				Thread.yield();
			state.set(0);
		}
	}
	
	public static final class ConditionParkNoStarvation implements Condition {
		private volatile LinkedList<Thread> waitingThreads = new LinkedList<Thread>();
		private final AtomicBoolean someoneWorkingWithThreadsList = new AtomicBoolean(false);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public void myWait() {
			try {
				while (!someoneWorkingWithThreadsList.compareAndSet(false, true))
					Thread.yield();
				
				waitingThreads.addLast(Thread.currentThread());
			} finally {
				someoneWorkingWithThreadsList.set(false);
			}
			unsafe.park(false, 0);
		}
		
		@Override
		public void myNotify() {
			Thread thread = null;
			try {
				while (!someoneWorkingWithThreadsList.compareAndSet(false, true))
					Thread.yield();
				
				if (!waitingThreads.isEmpty())
					thread = waitingThreads.removeFirst();
			} finally {
				someoneWorkingWithThreadsList.set(false);
			}
			if (thread != null) {
				while (thread.getState() != Thread.State.WAITING)
					Thread.yield();
				unsafe.unpark(thread);
			}
		}
		
		@Override
		public void myNotifyAll() {
			LinkedList<Thread> oldList;
			try {
				while (!someoneWorkingWithThreadsList.compareAndSet(false, true))
					Thread.yield();
				
				oldList = waitingThreads;
				waitingThreads = new LinkedList<Thread>();
			} finally {
				someoneWorkingWithThreadsList.set(false);
			}
			for (Thread thread : oldList) {
				while (thread.getState() != Thread.State.WAITING)
					Thread.yield();
				unsafe.unpark(thread);
			}
		}
	}
	
	public static void main(String[] args) {
		ConditionTester.testCondition(new ConditionBusyWaitNoStarvation());
	}
}
