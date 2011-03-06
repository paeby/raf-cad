package examples.casn;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class CompareAndSet2ProblemInstance implements ProblemInstance<CompareAndSet2> {
	final int timesToIncrement[];
	
	/**
	 * Dužina niza prosleđenog kao parametar određuje broj niti koje se
	 * izvršavaju. Svaka od niti će inkrementirati onoliko puta kolika je
	 * apsolutna vrednost. Ovo inkrementiranje se oslanja na atomičnost cas2-a.
	 * Na kraju rada bi trebalo da vrednost countera bude jednaka sumi elemenata
	 * niza.
	 * 
	 * Unutar cas2 stoje jednake vrednosti u svakom trenutku. Takođe, postoji
	 * counter koji treba da sadrži iste vrednosti kao i cas2; ako nije tako,
	 * postoji greška.
	 * 
	 * Ukoliko dve različite niti krenu da upisuju iste stvari tokom cas-a simultano,
	 * counter će biti veći od vrednosti unutar cas2 registara, dakle cas nije atomičan
	 * 
	 * @param timesToIncrementPerThread
	 */
	public CompareAndSet2ProblemInstance(int... timesToIncrementPerThread) {
		super();
		this.timesToIncrement = timesToIncrementPerThread;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final CompareAndSet2 solution) {
		final AtomicBoolean ok = new AtomicBoolean(true);
		final AtomicInteger counter = new AtomicInteger(0);
		
		for (int threadId = 0; threadId < timesToIncrement.length; threadId++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadId, timesToIncrement.length);
			final int tId = threadId;
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(ConcurrentSystem system) {
					int toDo = timesToIncrement[tId];
					
					if (toDo == 0)
						return;
					
					int[] values;
					boolean casResult;
					do {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reading values");
						values = solution.read(managedSystem, callerInfo);
						// za slučaj da prosledi referencu koju će modifikovati,
						// ko zna šta im može pasti na pamet
						values = Arrays.copyOf(values, values.length);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " done reading values: " + Arrays.toString(values));
						
						if (values[0] != values[1]) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** inconsistent values found: " + Arrays.toString(values));
							ok.set(false);
						}
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " starting cas2 w/ parameters " + values[0] + ", " + values[1] + ", " + (values[0] + 1) + ", " + (values[1] + 1));
						casResult = solution.compareAndSet(values[0], values[1], values[0] + 1, values[1] + 1, managedSystem, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " cas2 " + (casResult ? "succeeded." : "failed."));
						if (casResult) {
							if (counter.incrementAndGet() != values[0] + 1) {								
								managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** cas not atomical: values inside should be [" + counter.get() + ", " + counter.get() + "]");
								ok.set(false);
							}
							
							toDo--;
						}
					} while (ok.get() && toDo > 0 && casResult);
				}
			});
		}
		managedSystem.startSimAndWaitToFinish();
		return ok.get();
	}
}
