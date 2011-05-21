package kids.dist.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import kids.dist.common.Utils;
import kids.dist.common.problem.Solution;
import kids.dist.common.tasks.Task;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.MessageBundle;
import kids.dist.core.MessageQueue;
import kids.dist.core.impl.message.FifoMessageQueue;
import kids.dist.core.impl.message.MessageBundleImpl;
import kids.dist.core.impl.message.RandomMessageQueue;
import kids.dist.core.network.DistNetwork;

public class DistributedManagedSystemImpl implements DistributedManagedSystem {
	private final ExecutorService executor;
	
	final AtomicInteger startedTasks = new AtomicInteger();
	final Map<Long, ProcessInfo> processInfosByThreadId = new HashMap<Long, ProcessInfo>();
	final List<ProcessInfo> processInfos = new ArrayList<ProcessInfo>();
	final AtomicInteger processInfoDispenser = new AtomicInteger(0);
	
	final Map<Long, Object> monitors = new LinkedHashMap<Long, Object>();
	final AtomicInteger activeTasks = new AtomicInteger(1);
	final AtomicReference<Object> current = new AtomicReference<Object>();
	
	final AtomicBoolean transactionActive = new AtomicBoolean();
	
	final AtomicBoolean finished = new AtomicBoolean();
	final long seed;
	final Random rand;
	
	final ArrayList<String> log = new ArrayList<String>();
	final Map<InstructionType, Integer> stats = new TreeMap<InstructionType, Integer>();
	
	boolean allowMessageToAnyone;
	
	public DistributedManagedSystemImpl(ExecutorService executor, DistNetwork network) {
		this(executor, network, false);
	}
	
	public DistributedManagedSystemImpl(ExecutorService executor, DistNetwork network, boolean fifoMessageQueues) {
		this.executor = executor;
		this.seed = new Random().nextLong();
		this.rand = new Random(seed);
		int[] ids = network.getPIds();
		int[][] neighborhoods = network.getNeighborhoods();
		for (int i = 0; i < ids.length; i++) {
			ProcessInfo newInfo = new ProcessInfo(neighborhoods[i], i, ids[i], fifoMessageQueues);
			processInfos.add(newInfo);
		}
	}
	
	public boolean isAllowMessageToAnyone() {
		return allowMessageToAnyone;
	}
	
	public void setAllowMessageToAnyone(boolean allowMessageToAnyone) {
		this.allowMessageToAnyone = allowMessageToAnyone;
	}
	
	public ProcessInfo getProcessInfo() {
		return getProcessInfo(Thread.currentThread().getId());
	}
	
	public ProcessInfo getProcessInfo(long threadId) {
		synchronized (this) {
			ProcessInfo info = processInfosByThreadId.get(threadId);
			if (info == null) {
				info = processInfos.get(processInfoDispenser.getAndIncrement());
				processInfosByThreadId.put(threadId, info);
			}
			return info;
		}
	}
	
	@Override
	public int getProcessId() {
		return getProcessInfo().processId;
	}
	
	@Override
	public int[] getProcessNeighbourhood() {
		return getProcessInfo().neighbourhood;
	}
	
	@Override
	public void sendMessage(int destinationId, int type, Object message) {
		ProcessInfo myInfo = getProcessInfo();
		// Object msgClone = ObjectHelper.clone(message);
		MessageBundle msg = new MessageBundleImpl(myInfo.processId, destinationId, type, message);
		if (myInfo.processId == destinationId)
			throw new IllegalArgumentException("Process " + myInfo.processId + " pokušava da šalje poruke sam sebi");
		if (!allowMessageToAnyone && Arrays.binarySearch(myInfo.neighbourhood, destinationId) < 0)
			throw new IllegalArgumentException("Process " + myInfo.processId + " ne može da šalje poruke procesu " + destinationId + " koji mu je van susedstva");
		else {
			actionCalled();
			for (ProcessInfo info : processInfos) {
				if (info.processId == destinationId) {
					addLogLine("Process #" + myInfo.processId + " has sent a message to " + destinationId + " of type " + type + ": " + message);
					info.messageQueue.add(msg);
					return;
				}
			}
			throw new IllegalArgumentException("Process by Id " + destinationId + " unknown");
		}
	}
	
	@Override
	public void yield() {
		handleMessages();
	}
	
	@Override
	public void handleMessages() {
		ProcessInfo myInfo = getProcessInfo();
		actionCalled();
		while (!myInfo.messageQueue.isEmpty()) {
			MessageBundle message = myInfo.messageQueue.getMessage();
			addLogLine("Process #" + myInfo.processId + " has received a message from " + message.getFrom() + " of type " + message.getType() + ": " + message.getMsg());
			myInfo.solution.get().messageReceived(message.getFrom(), message.getType(), message.getMsg());
			actionCalled();
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
			ProcessInfo info = getProcessInfo();
			Thread cur = Thread.currentThread();
			if (!monitors.containsKey(cur.getId())) {
				monitors.put(cur.getId(), info.monitor);
			}
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
		
		ProcessInfo info = getProcessInfo();
		if (info.timebombTicks > 0) {
			info.timebombTicks--;
		} else if (info.timebombTicks == 0) {
			addLogLine("Process #" + info.processId + " abruptly terminated!");
			throw new FrameworkDecidedToKillProcessException();
		}
		
		if (transactionActive.get()) {
			if (current.get() != monitor)
				throw new IllegalStateException();
		} else {
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
	
	private static final int possibleLoopSize = 500000;
	
	@Override
	public void addLogLine(String line) {
		synchronized (this) {
			log.add("pid=" + getProcessId() + ":\t" + line);
			
			if (log.size() == possibleLoopSize) {
				System.out.println("POSSIBLE ENDLESS LOOP");
				printFinalState();
				System.out.println("POSSIBLE ENDLESS LOOP");
			}
		}
	}
	
	private Object getMonitor() {
		synchronized (this) {
			ProcessInfo info = getProcessInfo();
			return info.monitor;
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
			} else {
				int index = rand.nextInt(n);
				for (Iterator<Object> iter = monitors.values().iterator(); iter.hasNext();) {
					Object monitor = iter.next();
					if (index == 0) {
						if (!current.compareAndSet(null, monitor))
							throw new IllegalStateException();
						synchronized (monitor) {
							monitor.notify();
						}
						return;
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
		
		// printFinalState();
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
			System.out.println();
			
			System.out.println("Graph info:");
			for (ProcessInfo info: processInfos)
				System.out.println("Neighborhood of node #" + info.processId + ": " + Arrays.toString(info.neighbourhood));
			
			System.out.println();
			System.out.println("Message log:");
			
			if (log.size() != possibleLoopSize) {
				for (String l : log)
					System.out.println("\t" + l);
			} else {
				for (int i = 0; i < 1000; i++)
					System.out.println("\t" + log.get(i));
				System.out.println("and more...");
			}
		}
	}
	
	public int getStartedTasks() {
		return startedTasks.get();
	}
	
	public int getSteps() {
		synchronized (this) {
			return log.size();
		}
	}
	
	@Override
	public void incStat(InstructionType type) {
		Utils.increment(stats, type);
	}
	
	public Map<InstructionType, Integer> getStats() {
		return stats;
	}
	
	@Override
	public int getNumberOfNodes() {
		return processInfos.size();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (ProcessInfo info : processInfos) {
			builder.append(info.processId + " " + Arrays.toString(info.neighbourhood) + "\n");
		}
		return builder.toString();
	}
	
	@SuppressWarnings("unused")
	public class ProcessInfo implements Comparable<ProcessInfo> {
		private final int processId;
		private final int processIndex;
		private final int[] neighbourhood;
		private final MessageQueue messageQueue;
		private final Object monitor;
		private final AtomicReference<Solution> solution;
		private int timebombTicks = -1;
		
		public ProcessInfo(int[] neighbourhood, int processIndex, int pId, boolean fifoMessageQueues) {
			this.processIndex = processIndex;
			this.processId = pId;
			this.messageQueue = fifoMessageQueues ? new FifoMessageQueue() : new RandomMessageQueue();
			this.neighbourhood = neighbourhood;
			this.monitor = new Object();
			this.solution = new AtomicReference<Solution>(null);
		}
		
		@Override
		public int compareTo(ProcessInfo o) {
			if (this.processId > o.processId)
				return -1;
			else if (this.processId < o.processId)
				return 1;
			else
				return 0;
		}
	}
	
	@Override
	public void setMySolution(Solution solution) {
		if (!getProcessInfo().solution.compareAndSet(null, solution))
			throw new IllegalStateException("Solution already set!");
	}
	
	@Override
	public void setTimebombForThisThread(int ticks) {
		if (ticks == 0) {
			addLogLine("Process #" + getProcessId() + " abruptly terminated!");
			throw new FrameworkDecidedToKillProcessException();
		}
		getProcessInfo().timebombTicks = ticks;
	}
}
