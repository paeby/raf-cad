package solutions.counter;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.counter.Counter;
import examples.counter.CounterTester;

public class CounterSolutions {
	private static final class CounterNaive implements Counter {
		@Override
		public void inc(ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(0);
			reg.write(reg.read() + 1);
		}
		
		@Override
		public int getValue(ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(0);
			return reg.read();
		}
	}
	
	private static final class CounterTransaction implements Counter {
		@Override
		public void inc(ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(0);
			system.transactionStarted();
			reg.write(reg.read() + 1);
			system.transactionEnded();
		}
		
		@Override
		public int getValue(ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(0);
			return reg.read();
		}
	}
	
	private static final class CounterCorrect implements Counter {
		@Override
		public void inc(ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(callerInfo.getCurrentId());
			reg.write(reg.read() + 1);
		}
		
		@Override
		public int getValue(ConcurrentSystem system, ProcessInfo callerInfo) {
			int sum = 0;
			for (int i = 0; i < callerInfo.getTotalProcesses(); i++) {
				Register reg = system.getRegister(i);
				sum += reg.read();
			}
			return sum;
		}
	}
	
	
	
	public static class CounterCAS implements Counter {
		final int registerIndex;
		
		public CounterCAS(int registerIndex) {
			super();
			this.registerIndex = registerIndex;
		}

		@Override
		public void inc(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			while (true) {
				int value = reg.read();
				if (reg.compareAndSet(value, value+1))
					return;
			}
		}
		
		@Override
		public int getValue(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			return reg.read();
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		CounterTester.testCounter(new CounterNaive());
		System.out.println("Correct:");
		CounterTester.testCounter(new CounterCorrect());
		System.out.println("Transaction:");
		CounterTester.testCounter(new CounterTransaction());
		System.out.println("CAS:");
		CounterTester.testCounter(new CounterCAS(0));
	}
	

}
