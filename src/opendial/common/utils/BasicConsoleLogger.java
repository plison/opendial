package opendial.common.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class BasicConsoleLogger extends Logger {

	protected BasicConsoleLogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	public static Logger getDefaultLogger() {

		Logger l = getLogger("");

		for (Handler handler2 : l.getHandlers()) {
			l.removeHandler(handler2);
		}

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new LogFormatter());
		l.addHandler(handler);

		return l;
	}

}


final class LogFormatter extends Formatter {

	@Override
	public String format(LogRecord arg0) {
		return "["+arg0.getClass().getSimpleName()+"] " + arg0.getLevel() + ": " + arg0.getMessage();
	}
}
