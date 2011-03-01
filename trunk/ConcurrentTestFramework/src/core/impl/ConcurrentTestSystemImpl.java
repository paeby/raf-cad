package core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import common.registers.CASRegister;
import common.registers.Register;
import common.tasks.Task;

import core.ConcurrentManagedSystem;
import core.impl.registers.CASRegsiterImpl;

public class ConcurrentTestSystemImpl implements ConcurrentManagedSystem {
	private final ExecutorService executor;

	final AtomicInteger startedTasks = new AtomicInteger();
	private final Map<Long, Integer> pids = new HashMap<Long, Integer>();
	private final Map<Integer, CASRegister> registers = new TreeMap<Integer, CASRegister>();
	
	private final Map<Long, Object> monitors = new LinkedHashMap<Long, Object>();
	final AtomicInteger activeTasks = new AtomicInteger(1);
	final AtomicReference<Object> current = new AtomicReference<Object>();
	
	final AtomicBoolean transactionActive = new AtomicBoolean();
	
	final AtomicBoolean finished = new AtomicBoolean();
	final long seed;
	final Random rand;
	
	final ArrayList<String> log = new ArrayList<String>();
	
	public ConcurrentTestSystemImpl(ExecutorService executor) {
		this.executor = executor;
		this.seed = new Random().nextLong();
		this.rand = new Random(seed);
	}
	
	@Override
	public CASRegister getCASRegister(int index) {
		synchronized (this) {
			CASRegister reg = registers.get(index);
			if (reg == null) {
				reg = new CASRegsiterImpl(this, index, 0);
				registers.put(index, reg);
			}
			return reg;			
		}
	}

	@Override
	public Register getRegister(int index) {
		return getCASRegister(index);
	}
	
	@Override
	public int getPID() {
		synchronized (this) {
			Thread cur = Thread.currentThread();
			return pids.get(cur.getId());
		}
	}

	@Override
	public void startTaskConcurrently(Task task) {
		activeTasks.incrementAndGet();
		WrappedRunnable wrapped = new WrappedRunnable(task, this);
		executor.execute(wrapped);
	}


	@Override
	public void taskStarted() {
		startedTasks.incrementAndGet();

		synchronized (this) {
			Thread cur = Thread.currentThread();
			if (!monitors.containsKey(cur.getId())) {
				if (!pids.containsKey(cur.getId())) 
					pids.put(cur.getId(), pids.size());
//				else throw new IllegalStateException();
				monitors.put(cur.getId(), new Object());
			}
			else throw new IllegalStateException();
		}
		
		waitForAction();
	}

	@Override
	public void taskFinished() {
		synchronized (this) {
			Thread cur = Thread.currentThread();
			Object monitor = monitors.remove(cur.getId());
			if (!current.compareAndSet(monitor, null))
				throw new IllegalStateException();
		}
		int active = activeTasks.decrementAndGet();
		
		if (active == 0)
			awakeNext();		
	}
	
	@Override
	public void actionCalled() {
		Object monitor = getMonitor();
		
		if (transactionActive.get()) {
			if (current.get() != monitor)
				throw new IllegalStateException();
		}
		else {
			if (!current.compareAndSet(monitor, null))
				throw new IllegalStateException();
		
			waitForAction();
		}
	}

	private void waitForAction() {
		Object monitor = getMonitor();
		int active = activeTasks.decrementAndGet();
		if (active == 0)
			awakeNext();
		
		synchronized (monitor) {
			while (current.get() != monitor) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		activeTasks.incrementAndGet();
	}
	
	@Override
	public void addLogLine(String line) {
		synchronized (this) {
			log.add("pid=" + getPID() + ":\t" + line);
		}
	}
	
	private Object getMonitor() {
		synchronized (this) {
			Thread cur = Thread.currentThread();
			return monitors.get(cur.getId());
		}
	}
	
	private void awakeNext() {
		synchronized (this) {
			int n = monitors.size();
			
			if (n == 0) {
				if (!finished.compareAndSet(false, true))
					throw new IllegalStateException();
				synchronized (finished) {
					finished.notify();
				}				
			}
			else {
				int index = rand.nextInt(n);
				for (Iterator<Object> iter = monitors.values().iterator(); iter.hasNext();) {
					Object monitor = iter.next();
					if (index == 0)
					{
						if (!current.compareAndSet(null, monitor))
							throw new IllegalStateException();
						synchronized (monitor) {
							monitor.notify();
						}
						return ;
					}
					
					index--;
				}
			}
			
		}
	}

	public void startTasks(Task[] tasks) {
		synchronized (this) {
			for (Task task : tasks)
				startTaskConcurrently(task);
		}

		startSimAndWaitToFinish();
		
//		printFinalState();
	}

	@Override
	public void startSimAndWaitToFinish() {
		int active = activeTasks.decrementAndGet();
		if (active == 0)
			awakeNext();
		
		synchronized (finished) {
			while (!finished.get()) {
				try {
					finished.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void printFinalState() {
		synchronized (this) {
			System.out.print("Final values :");
			for(CASRegister reg : registers.values())
				System.out.print("\t"+reg);
			System.out.println();
			for(String l : log) 
				System.out.println("\t" + l);
		}
	}
	
	public boolean equalFinalState(ConcurrentTestSystemImpl other) {
		synchronized (this) {
			synchronized (other) {
				if (registers.size() != other.registers.size())
					return false;
				
				for(Entry<Integer, CASRegister> entry : registers.entrySet()) {
					CASRegister otherReg = other.registers.get(entry.getKey());
					if (!entry.getValue().equals(otherReg))
						return false;
				}
				
				return true;
			}
		}
	}
	
	@Override
	public void transactionStarted() {
		if (!transactionActive.compareAndSet(false, true))
			throw new IllegalStateException();
	}
	
	@Override
	public void transactionEnded() {
		if (!transactionActive.compareAndSet(true, false))
			throw new IllegalStateException();
	}
	
	public int getStartedTasks() {
		return startedTasks.get();
	}
	
	public int getSteps() {
		synchronized (this) {
			return log.size();
		}
	}
	
//	@Override
//	public Random getRandom() {
//		return rand;
//	}
	
	@Override
	public void yield() {
		actionCalled();
	}
}