package solutions.consensus;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

import examples.consensus.Consensus;

public class ConsensusSolutoins {

	public static class ConsensusWaitFree implements Consensus {
		final int registerIndex;
		
		public ConsensusWaitFree(int registerIndex) {
			this.registerIndex = registerIndex;
		}

		@Override
		public int propose(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			if (reg.compareAndSet(0, value+1)) 
				return value+1;
			else return reg.read()-1;
			// +1 and -1 are only to allow values to be in [0,MAX_VALUE] interval, without them, it works only for proposing [1,MAX_VALUE]
		}
		
		public int get(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			return reg.read() - 1;
		}
	}
}
