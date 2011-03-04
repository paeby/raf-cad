package solutions.crossing;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

import examples.crossing.Crossing;
import examples.crossing.CrossingTester;

public class CrossingSolutions {
	static final class CrossingNaive implements Crossing {
		
		@Override
		public void enterCrossing(int curDirection, int totalDirections, ConcurrentSystem system, ProcessInfo callerInfo) {}
		
		@Override
		public void leaveCrossing(int curDirection, int totalDirections, ConcurrentSystem system, ProcessInfo callerInfo) {}
	}
	
	static final class CrossingPerformant implements Crossing {
		
		@Override
		public void enterCrossing(int curDirection, int totalDirections, ConcurrentSystem system, ProcessInfo callerInfo) {
			curDirection++;
			
			CASRegister reg = system.getCASRegister(0);
			
			int shift = Integer.numberOfTrailingZeros(Integer.highestOneBit(totalDirections))+1;
			int mask = (1<<shift)-1;
			while (true) {
				int value = reg.read();
				int counter = value >> shift;
				int inCrossing = value & mask;
				
				if (inCrossing == 0 || inCrossing == curDirection) {
					int newValue = ((counter+1)<<shift) | curDirection;
					if (reg.compareAndSet(value, newValue))
						return;
				}
			}
		}
		
		@Override
		public void leaveCrossing(int curDirection, int totalDirections, ConcurrentSystem system, ProcessInfo callerInfo) {
			curDirection++;

			CASRegister reg = system.getCASRegister(0);
			
			int shift = Integer.numberOfTrailingZeros(Integer.highestOneBit(totalDirections))+1;
			while (true) {
				int value = reg.read();
				int counter = value >> shift;
				
				int newValue = (counter == 1)?0:(((counter-1)<<shift) | curDirection); 
				if (reg.compareAndSet(value, newValue))
					return;

			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		CrossingTester.testCrossing(new CrossingNaive());	
		System.out.println("CrossingPerformant:");
		CrossingTester.testCrossing(new CrossingPerformant());		

	}
	
}
