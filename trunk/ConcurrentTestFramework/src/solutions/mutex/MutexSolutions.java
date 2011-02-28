package solutions.mutex;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

import examples.mutex.Mutex;
import examples.mutex.MutexTester;

public class MutexSolutions {
	private static final class MutexNaive implements Mutex {
		@Override
		public void lock(ConcurrentSystem system, ProcessInfo info) {}
		
		@Override
		public void unlock(ConcurrentSystem system, ProcessInfo info) {};
	}
	
	private static final class MutexCorrect implements Mutex {
		@Override
		public void lock(ConcurrentSystem system, ProcessInfo info) {
			CASRegister register = system.getCASRegister(0);
			while (!register.compareAndSet(0, 1));
		}
		
		@Override
		public void unlock(ConcurrentSystem system, ProcessInfo info) {
			CASRegister register = system.getCASRegister(0);
			register.write(0);
		};
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		MutexTester.testMutex(new MutexNaive());
		System.out.println("Correct:");
		MutexTester.testMutex(new MutexCorrect());
	}
}
