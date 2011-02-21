package examples.counter;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.Register;

import core.impl.problem.ProblemTester;

public class CounterTester {

	private static final class CounterNaive implements Counter {
		@Override
		public void inc(ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(0);
			reg.write(reg.read()+1);
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
			reg.write(reg.read()+1);
		}

		@Override
		public int getValue(ConcurrentSystem system, ProcessInfo callerInfo) {
			int sum = 0;
			for(int i =0 ;i<callerInfo.getTotalProcesses();i++) {
				Register reg = system.getRegister(i);
				sum += reg.read();
			}
			return sum;
		}
	}

	public static void main(String[] args) {
		int[][] counts = new int[][] {{1, 1}, {2, 1, 1}, {3, 2, 3, 1}};
		int[] times = new int[] {100,200,1000};
		
		for(int i = 0;i<counts.length;i++)
			if (!ProblemTester.testProblem(new CounterProblemInstance(counts[i]), new CounterCorrect(), times[i]))
				return;
	}
}