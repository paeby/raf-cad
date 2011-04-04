package useful;


public class ObjectHelper {
	@SuppressWarnings("unchecked")
	public static <T> T copy(T object) {
		try {
			return (T) object.getClass().newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Da bi tester radio klasa mora imati public default konstruktor!", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Da bi tester radio klasa mora imati public default konstruktor!", e);
		}
	}
}
