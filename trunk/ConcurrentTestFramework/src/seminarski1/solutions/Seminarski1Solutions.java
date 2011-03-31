package seminarski1.solutions;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import seminarski1.IntSet;
import seminarski1.tester.IntSetTester;

public class Seminarski1Solutions {
	public static final class AlwaysEmpty implements IntSet {
		
		@Override
		public boolean addInt(int value) {
			return true;
		}
		
		@Override
		public boolean removeInt(int value) {
			return false;
		}
		
		@Override
		public boolean contains(int value) {
			return false;
		}
	}
	
	public static final class AlwaysFull implements IntSet {
		@Override
		public boolean addInt(int value) {
			return false;
		}
		
		@Override
		public boolean removeInt(int value) {
			return true;
		}
		
		@Override
		public boolean contains(int value) {
			return true;
		}
	}
	
	public static final class PositivelyWorking implements IntSet {
		private final Set<Integer> intSet = new ConcurrentSkipListSet<Integer>();
		
		@Override
		public boolean addInt(int value) {
			return intSet.add(value);
		}
		
		@Override
		public boolean removeInt(int value) {
			return intSet.remove(value);
		}
		
		@Override
		public boolean contains(int value) {
			return intSet.contains(value);
		}
	}
	
	public static void main(String[] args) {
		IntSetTester.testEverything(new PositivelyWorking());
	}
}
