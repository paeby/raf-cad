package examples.mutex;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

import core.impl.problem.ProblemTester;

public class MutexTester {
	
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
	
	public static void testMutex(Mutex mutex) {
		ProblemTester.testProblem(new MutexProblemInstance(), mutex, 100);
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		testMutex(new MutexNaive());
		System.out.println("Correct:");
		testMutex(new MutexCorrect());
	}
	
}
