package eu.ardinsys.reflection.tool.proxy;

import eu.ardinsys.reflection.MessageTemplates;
import eu.ardinsys.reflection.tool.proxy.Selector.SelectorType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default {@link SelectorParser} implementation.
 */
public class BasicSelectorParser implements SelectorParser {
  private static final Pattern PATTERN_PROPERTY = Pattern.compile("^\\.(\\w+)");
  private static final Pattern PATTERN_KEY = Pattern.compile("^\\[(.*?)\\]");
  private static final Pattern PATTERN_INDEX = Pattern.compile("^\\[(0|[1-9][0-9]*)\\]");
  private static final Pattern PATTERN_LAST_INDEX = Pattern.compile("^\\[\\-\\]");
  private static final Pattern PATTERN_APPEND_INDEX = Pattern.compile("^\\[\\+\\]");

  /**
   * @see SelectorParser#parsePath(String)
   */
  @Override
  public List<Selector> parsePath(String path) {
    List<Selector> selectors = new ArrayList<Selector>();
    while (!path.isEmpty()) {
      Matcher matcher;
      if ((matcher = PATTERN_PROPERTY.matcher(path)).find()) {
        selectors.add(new Selector(matcher.group(1), SelectorType.PROPERTY));
      } else if ((matcher = PATTERN_LAST_INDEX.matcher(path)).find()) {
        selectors.add(new Selector(null, SelectorType.LAST_INDEX));
      } else if ((matcher = PATTERN_APPEND_INDEX.matcher(path)).find()) {
        selectors.add(new Selector(null, SelectorType.APPEND_INDEX));
      } else if ((matcher = PATTERN_INDEX.matcher(path)).find()) {
        selectors.add(new Selector(matcher.group(1), SelectorType.INDEX));
      } else if ((matcher = PATTERN_KEY.matcher(path)).find()) {
        selectors.add(new Selector(matcher.group(1), SelectorType.KEY));
      } else {
        throw new RuntimeException(
            String.format(MessageTemplates.ERROR_INVALID_PATH_SUBEXPRESSION, path));
      }

      path = path.substring(matcher.end());
    }

    return selectors;
  }
}
