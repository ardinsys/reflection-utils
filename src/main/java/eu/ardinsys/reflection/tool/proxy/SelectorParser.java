package eu.ardinsys.reflection.tool.proxy;

import java.util.List;

/**
 * Selector path parser.
 * <p>
 * See {@link #parsePath(String)}.
 */
public interface SelectorParser {
  /**
   * Parses the given selector path. See {@link Selector}.
   *
   * @param path A string of concatenated selector expressions
   * @return A list of parsed selectors
   */
  List<Selector> parsePath(String path);
}
