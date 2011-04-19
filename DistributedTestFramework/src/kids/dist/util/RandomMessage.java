package kids.dist.util;

import java.util.concurrent.atomic.AtomicInteger;

public class RandomMessage implements Cloneable {
	private static final AtomicInteger idDispenser = new AtomicInteger();
	private final int id;
	
	public RandomMessage() {
		this.id = idDispenser.incrementAndGet();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RandomMessage other = (RandomMessage) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "[Random message #" + id + ']';
	}
}
