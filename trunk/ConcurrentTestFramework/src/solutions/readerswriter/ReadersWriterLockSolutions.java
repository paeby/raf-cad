package solutions.readerswriter;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

import examples.readerswriter.ReadersWriterLock;
import examples.readerswriter.ReadersWriterLockTester;

public class ReadersWriterLockSolutions {
	static final class ReadersWriterNaive implements ReadersWriterLock {
		
		@Override
		public void lockRead(ConcurrentSystem system, ProcessInfo info) {}
		
		@Override
		public void lockWrite(ConcurrentSystem system, ProcessInfo info) {}
		
		@Override
		public void unlockRead(ConcurrentSystem system, ProcessInfo info) {}
		
		@Override
		public void unlockWrite(ConcurrentSystem system, ProcessInfo info) {}
		
	}
	
	static final class ReadersWriterMutex implements ReadersWriterLock {
		private void lock(ConcurrentSystem system, ProcessInfo info) {
			CASRegister register = system.getCASRegister(-666);
			while (!register.compareAndSet(0, 1))
				;
		}
		
		private void unlock(ConcurrentSystem system, ProcessInfo info) {
			CASRegister register = system.getCASRegister(-666);
			register.write(0);
		};
		
		@Override
		public void lockRead(ConcurrentSystem system, ProcessInfo info) {
			lock(system, info);
		}
		
		@Override
		public void lockWrite(ConcurrentSystem system, ProcessInfo info) {
			lock(system, info);
		}
		
		@Override
		public void unlockRead(ConcurrentSystem system, ProcessInfo info) {
			unlock(system, info);
		}
		
		@Override
		public void unlockWrite(ConcurrentSystem system, ProcessInfo info) {
			unlock(system, info);
		}
	}
	
	static final class ReadersWriterLockCorrect implements ReadersWriterLock {
		
		final Mutex noWriters, noReaders, counterMutex;
		
		// noReaders je u registru #2
		
		public ReadersWriterLockCorrect() {
			this.noWriters = new Mutex(0);
			this.noReaders = new Mutex(1);
			this.counterMutex = new Mutex(2);
		}
		
		@Override
		public void lockWrite(ConcurrentSystem system, ProcessInfo info) {
			noWriters.lock(system, info);
			noReaders.lock(system, info);
			noReaders.unlock(system, info);
		}
		
		@Override
		public void unlockWrite(ConcurrentSystem system, ProcessInfo info) {
			noWriters.unlock(system, info);
		}
		
		@Override
		public void lockRead(ConcurrentSystem system, ProcessInfo info) {
			noWriters.lock(system, info);
			CASRegister nReaders = system.getCASRegister(2);
			int prev = nReaders.read();
			while (!nReaders.compareAndSet(prev, prev + 1))
				prev = nReaders.read();
			if (prev == 0)
				noReaders.lock(system, info);
			noWriters.unlock(system, info);
		}
		
		@Override
		public void unlockRead(ConcurrentSystem system, ProcessInfo info) {
			CASRegister nReaders = system.getCASRegister(2);
			int current = nReaders.read();
			while (!nReaders.compareAndSet(current, current - 1))
				current = nReaders.read();
			if (current == 1)
				noReaders.unlock(system, info);
		}
		
		private static final class Mutex {
			final int reg;
			
			public Mutex(int reg) {
				super();
				this.reg = reg;
			}
			
			public void lock(ConcurrentSystem system, ProcessInfo info) {
				CASRegister register = system.getCASRegister(reg);
				while (!register.compareAndSet(0, 1))
					;
			}
			
			public void unlock(ConcurrentSystem system, ProcessInfo info) {
				CASRegister register = system.getCASRegister(reg);
				register.write(0);
			};
		}
	}
	
	static final class ReadersWriterLockPerformant implements ReadersWriterLock {

		@Override
		public void lockRead(ConcurrentSystem system, ProcessInfo info) {
			CASRegister reg = system.getCASRegister(0);
			while (true)
			{
				int cur = reg.read();
				if (cur >= 0)
					if (reg.compareAndSet(cur, cur+1))
						return;
			}
		}
		
		@Override
		public void unlockRead(ConcurrentSystem system, ProcessInfo info) {
			CASRegister reg = system.getCASRegister(0);
			while (true)
			{
				int cur = reg.read();
				if (reg.compareAndSet(cur, cur-1))
					return;
			}
	
		}

		@Override
		public void lockWrite(ConcurrentSystem system, ProcessInfo info) {
			CASRegister reg = system.getCASRegister(0);
			while (true)
			{
				if (reg.compareAndSet(0, -1))
					return;
			}
		}



		@Override
		public void unlockWrite(ConcurrentSystem system, ProcessInfo info) {
			CASRegister reg = system.getCASRegister(0);
			while (true)
			{
				if (reg.compareAndSet(-1, 0))
					return;
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		ReadersWriterLockTester.testReadersWriterLock(new ReadersWriterNaive());
		System.out.println("PlainOldMutex:");
		ReadersWriterLockTester.testReadersWriterLock(new ReadersWriterMutex());
		System.out.println("\n\nCorrect ReadersWriterLock:");
		ReadersWriterLockTester.testReadersWriterLock(new ReadersWriterLockCorrect());		
		System.out.println("\n\nReadersWriterLockPerformant:");
		ReadersWriterLockTester.testReadersWriterLock(new ReadersWriterLockPerformant());		

	}
	
}
