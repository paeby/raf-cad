package useful;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeHelper {
	public static Unsafe getUnsafe() {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Unsafe) field.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
