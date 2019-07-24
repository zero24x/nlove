package jsmith.nknsdk.examples;

import java.io.File;

import org.joda.time.Duration;
import org.slf4j.Marker;

import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.TPLoggerFactory;
import com.darkyen.tproll.logfunctions.DateTimeFileCreationStrategy;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFileHandler;
import com.darkyen.tproll.logfunctions.LogFunctionMultiplexer;
import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.util.TimeFormatter;

/**
 *
 */
public class LogUtils {

	public static void setupLogging(byte level) {
		switch (level) {
		case TPLogger.TRACE:
			TPLogger.TRACE();
			break;
		case TPLogger.DEBUG:
			TPLogger.DEBUG();
			break;
		case TPLogger.INFO:
			TPLogger.INFO();
			break;
		case TPLogger.WARN:
			TPLogger.WARN();
			break;
		case TPLogger.ERROR:
			TPLogger.ERROR();
			break;
		default:
			TPLogger.INFO();
		}

		FileLogFunction fileLog = new FileLogFunction(new TimeFormatter.AbsoluteTimeFormatter(),
				new LogFileHandler(new File("logs"), new DateTimeFileCreationStrategy(DateTimeFileCreationStrategy.DEFAULT_DATE_FILE_NAME_FORMATTER, true,
						DateTimeFileCreationStrategy.DEFAULT_LOG_FILE_EXTENSION, 512 * 1000, Duration.standardDays(60)), true),
				true);

		if (level == TPLogger.DEBUG) {
			TPLogger.setLogFunction(new LogFilter(new LogFunctionMultiplexer(SimpleLogFunction.CONSOLE_LOG_FUNCTION, fileLog)));
		} else {
			TPLogger.setLogFunction(new LogFilter(fileLog));
		}

		TPLoggerFactory.USE_SHORT_NAMES = false;

		TPLogger.attachUnhandledExceptionLogger();
	}

	private static class LogFilter extends LogFunction {

		private final LogFunction parent;

		public LogFilter(LogFunction parent) {
			this.parent = parent;
		}

		@Override
		public void log(String name, long time, byte level, Marker marker, CharSequence content) {
			String shortname = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
			shortname += " @" + Thread.currentThread().getName();
			if (level <= TPLogger.DEBUG) {
				if (name.startsWith("jsmith.nknsdk.")) { // Filter by package
					parent.log(shortname, time, level, marker, content);
				}
			} else {
				parent.log(shortname, time, level, marker, content);
			}
		}

		@Override
		public boolean isEnabled(byte level, Marker marker) {
			return parent.isEnabled(level, marker);
		}
	}
}
