package eu.ardinsys.reflection.tool.dumper;

/**
 * Interface for custom type dumper implementations.
 * <p>
 * See {@link #dump(Object)}.
 * 
 * @param <T>
 *          The type of the object to format
 */
public interface CustomDumper<T> {
	/**
	 * Dumps the object to a string.
	 * 
	 * @param object
	 *          The object to dump
	 * @return The dumped <b>object</b>
	 */
	String dump(T object);
}
