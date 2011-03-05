package common;

import java.util.Map;

public class Utils {

	
	public static <T> void increment(Map<T, Integer> map, T type) {
		add(map, type, 1);
	}

	public static <T> void add(Map<T, Integer> map, T type, int value) {
		Integer i = map.get(type);
		if (i == null) 
			i = 0;
		map.put(type, i+value);
	}
}
