package com.breakinblocks.bblink.forge.common;

import org.apache.logging.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Minecraft 1.7.10 has log4j but no SLF4J. JDA and the relay log through SLF4J (bundled and relocated in this
 * mod's jar), so this provider forwards their logs to Minecraft's log4j. Found through
 * {@code META-INF/services}.
 */
public final class Log4jServiceProvider implements SLF4JServiceProvider {
    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private final ILoggerFactory loggerFactory = name -> loggers.computeIfAbsent(name, Log4jLogger::new);
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new NOPMDCAdapter();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }

    @Override
    public void initialize() {
    }

    private static final class Log4jLogger extends LegacyAbstractLogger {
        private static final long serialVersionUID = 1L;
        private final transient org.apache.logging.log4j.Logger log4j;

        Log4jLogger(String name) {
            this.name = name;
            this.log4j = LogManager.getLogger(name);
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, @Nullable Marker marker, String messagePattern,
                                                   @Nullable Object[] arguments, @Nullable Throwable throwable) {
            String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
            switch (level) {
                case ERROR:
                    log4j.error(message, throwable);
                    break;
                case WARN:
                    log4j.warn(message, throwable);
                    break;
                case INFO:
                    log4j.info(message, throwable);
                    break;
                case DEBUG:
                    log4j.debug(message, throwable);
                    break;
                default:
                    log4j.trace(message, throwable);
                    break;
            }
        }

        @Override
        public boolean isTraceEnabled() {
            return log4j.isTraceEnabled();
        }

        @Override
        public boolean isDebugEnabled() {
            return log4j.isDebugEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return log4j.isInfoEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return log4j.isWarnEnabled();
        }

        @Override
        public boolean isErrorEnabled() {
            return log4j.isErrorEnabled();
        }
    }
}
