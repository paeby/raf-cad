package solutions.addition;

import solutions.consensus.ConsensusSolutoins.ConsensusWaitFree;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.addition.Addition;

public class AdditionSolutions {
	public static class AdditionNaive implements Addition {
		@Override
		public int addAndGet(int toAdd, ConcurrentSystem system,
				ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(0);
			int value = reg.read()+toAdd;
			reg.write(value);
			return value;
		}
	}
	
	private static final class AdditionLikeCounter implements Addition {
		@Override
		public int addAndGet(int toAdd, ConcurrentSystem system, ProcessInfo callerInfo) {
			add(toAdd, system, callerInfo);
			return getValue(system, callerInfo);
		}
		
		
		public void add(int toAdd, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(callerInfo.getCurrentId());
			reg.write(reg.read() + toAdd);
		}
		
		
		public int getValue(ConcurrentSystem system, ProcessInfo callerInfo) {
			int sum = 0;
			for (int i = 0; i < callerInfo.getTotalProcesses(); i++) {
				Register reg = system.getRegister(i);
				sum += reg.read();
			}
			return sum;
		}
	}
	
	
	public static class AdditionCAS implements Addition {
		@Override
		public int addAndGet(int toAdd, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(0);
			while (true) {
				int value = reg.read();
				if (reg.compareAndSet(value, value + toAdd))
					return value + toAdd;
			}
		}
	}
	
	public static class AdditionWaitFree implements Addition {
		@Override
		public int addAndGet(int toAdd, ConcurrentSystem system,
				ProcessInfo callerInfo) {
			Register ourCounter = system.getRegister(callerInfo.getCurrentId());
			int count = ourCounter.read();
			count++;
			ourCounter.write(count);
			
			system.getRegister((count << 8) | callerInfo.getCurrentId()).write(toAdd);
			
			while (true) {
				int consensusIndex = callerInfo.getThreadLocal(-1);
				
				ConsensusWaitFree consensus = new ConsensusWaitFree( - consensusIndex - 1);
				int agreed = consensus.get(system, callerInfo);
				
				if (agreed == -1) {
					int proposal = 0;
					for(int i = 0;i<callerInfo.getTotalProcesses();i++) {
						if (system.getRegister(i).read() > callerInfo.getThreadLocal(i))
							proposal |= 1<<i;
					}

					agreed = consensus.propose(proposal, system, callerInfo);
				}
						
				int curValue = callerInfo.getThreadLocal(-2);
				int valueToReturn = Integer.MIN_VALUE;
								
				for(int i = 0;i<callerInfo.getTotalProcesses();i++) {
					if ((agreed & (1<<i)) != 0) {
						int where = callerInfo.getThreadLocal(i);
						where++;
						
						curValue += system.getRegister((where << 8) | i).read();
						
						if (i==callerInfo.getCurrentId())
						{
							valueToReturn = curValue;
						}
					}
				}
				
				if (valueToReturn != Integer.MIN_VALUE)
					return valueToReturn;
			}
		}
	}
}
