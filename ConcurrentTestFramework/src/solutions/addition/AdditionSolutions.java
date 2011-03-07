package solutions.addition;

import solutions.consensus.ConsensusSolutoins.ConsensusWaitFree;
import solutions.mutex.MutexSolutions.MutexCorrect;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.addition.Addition;
import examples.addition.AdditionTester;

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

	public static class AdditionMutex implements Addition {
		@Override
		public int addAndGet(int toAdd, ConcurrentSystem system, ProcessInfo callerInfo) {
			MutexCorrect mutex = new MutexCorrect(1);
			mutex.lock(system, callerInfo);
			CASRegister reg = system.getCASRegister(0);
			int value = reg.read()+toAdd;
			reg.write(value);
			mutex.unlock(system, callerInfo);
			return value;			
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
			
			system.getRegister((count << 8) | callerInfo.getCurrentId()).write(toAdd);
			ourCounter.write(count);
			
			while (true) {
				int consensusIndex = callerInfo.getThreadLocal(-1);
				callerInfo.putThreadLocal(-1, consensusIndex+1);
				
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
						callerInfo.putThreadLocal(i, where);

						curValue += system.getRegister((where << 8) | i).read();
						
						if (i==callerInfo.getCurrentId())
						{
							valueToReturn = curValue;
						}
					}
				}
				
				callerInfo.putThreadLocal(-2, curValue);
				
				if (valueToReturn != Integer.MIN_VALUE)
					return valueToReturn;
			}
		}
	}
	
	
	public static class AdditionWaitFreePerformant implements Addition {
		@Override
		public int addAndGet(int toAdd, ConcurrentSystem system,
				ProcessInfo callerInfo) {
			Register ourCounter = system.getRegister(callerInfo.getCurrentId());
			int count = ourCounter.read();
			count++;
			
			system.getRegister((count << 8) | callerInfo.getCurrentId()).write(toAdd);
			ourCounter.write(count);
			
			while (true) {
				int consensusIndex = callerInfo.getThreadLocal(-1);
				callerInfo.putThreadLocal(-1, consensusIndex+1);
				
				ConsensusWaitFree consensus = new ConsensusWaitFree( - consensusIndex - 1);
				int agreed = consensus.get(system, callerInfo);
				
				if (agreed == -1) {
					int proposal = 0;
					for(int i = 0;i<callerInfo.getTotalProcesses();i++) {
						if (i!=callerInfo.getCurrentId()) {
							if (system.getRegister(i).read() > callerInfo.getThreadLocal(i))
								proposal |= 1<<i;
						}
						else {
							if (count > callerInfo.getThreadLocal(i))
								proposal |= 1<<i;
							else throw new IllegalStateException();
						}
					}

					agreed = consensus.propose(proposal, system, callerInfo);
				}
						
				int curValue = callerInfo.getThreadLocal(-2);
				int valueToReturn = Integer.MIN_VALUE;
								
				for(int i = 0;i<callerInfo.getTotalProcesses();i++) {
					if ((agreed & (1<<i)) != 0) {
						int where = callerInfo.getThreadLocal(i);
						where++;
						callerInfo.putThreadLocal(i, where);

						if (i==callerInfo.getCurrentId())
						{
							curValue += toAdd;
							valueToReturn = curValue;
						}
						else curValue += system.getRegister((where << 8) | i).read();
					}
				}
				
				callerInfo.putThreadLocal(-2, curValue);
				
				if (valueToReturn != Integer.MIN_VALUE)
					return valueToReturn;
			}
		}
	}
	
	
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		AdditionTester.testAddition(new AdditionNaive());
		System.out.println("Like counter:");
		AdditionTester.testAddition(new AdditionLikeCounter());
		System.out.println("Mutex:");
		AdditionTester.testAddition(new AdditionMutex());
		System.out.println("CAS:");
		AdditionTester.testAddition(new AdditionCAS());
		System.out.println("Wait-free:");
		AdditionTester.testAddition(new AdditionWaitFree());
		System.out.println("Wait-free-perf:");
		AdditionTester.testAddition(new AdditionWaitFreePerformant());
	}
}
