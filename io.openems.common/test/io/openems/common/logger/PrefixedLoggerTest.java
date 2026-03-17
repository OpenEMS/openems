package io.openems.common.logger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.slf4j.Logger;

public class PrefixedLoggerTest {
	
	private static final Exception exception = new Exception("Test");
	
	public class TestPrefixedLogger extends PrefixedLogger {
		public TestPrefixedLogger(Logger logger) {
			super(logger);
		}

		@Override
		protected String prefix(String format) {
			return "[TEST] " + format;
		}
	}
	
    @Test
    public void testInfo() {
    	final var mockLogger  = mock(Logger.class);
    	when(mockLogger.isInfoEnabled()).thenReturn(true);
    	
    	final var logger = new TestPrefixedLogger(mockLogger);
    	
    	logger.info("Hello");
        verify(mockLogger).info("[TEST] Hello");
        
        logger.info("Hello {}!", "World");
        verify(mockLogger).info("[TEST] Hello {}!", "World");
        
        logger.info("Hello {} and {}!", "Max", "Bob");
        verify(mockLogger).info("[TEST] Hello {} and {}!", "Max", "Bob");
        
        logger.info("Error", exception);
        verify(mockLogger).info("[TEST] Error", exception);
        
        logger.atInfo().setMessage("Bye {}!").addArgument("Test").log();
        verify(mockLogger).info("[TEST] Bye {}!", new Object[]{"Test"});
    }
    
    @Test
    public void testTrace() {
    	final var mockLogger  = mock(Logger.class);
    	when(mockLogger.isTraceEnabled()).thenReturn(true);
    	
    	final var logger = new TestPrefixedLogger(mockLogger);
    	
    	logger.trace("Hello");
        verify(mockLogger).trace("[TEST] Hello");
        
        logger.trace("Hello {}!", "World");
        verify(mockLogger).trace("[TEST] Hello {}!", "World");
        
        logger.trace("Hello {} and {}!", "Max", "Bob");
        verify(mockLogger).trace("[TEST] Hello {} and {}!", "Max", "Bob");
        
        logger.trace("Error", exception);
        verify(mockLogger).trace("[TEST] Error", exception);
        
        logger.atTrace().setMessage("Bye {}!").addArgument("Test").log();
        verify(mockLogger).trace("[TEST] Bye {}!", new Object[]{"Test"});
    }
    
    @Test
    public void testDebug() {
    	final var mockLogger  = mock(Logger.class);
    	when(mockLogger.isDebugEnabled()).thenReturn(true);
    	
    	final var logger = new TestPrefixedLogger(mockLogger);
    	
    	logger.debug("Hello");
        verify(mockLogger).debug("[TEST] Hello");
        
        logger.debug("Hello {}!", "World");
        verify(mockLogger).debug("[TEST] Hello {}!", "World");
        
        logger.debug("Hello {} and {}!", "Max", "Bob");
        verify(mockLogger).debug("[TEST] Hello {} and {}!", "Max", "Bob");
        
        logger.debug("Error", exception);
        verify(mockLogger).debug("[TEST] Error", exception);
        
        logger.atDebug().setMessage("Bye {}!").addArgument("Test").log();
        verify(mockLogger).debug("[TEST] Bye {}!", new Object[]{"Test"});
    }
    
    @Test
    public void testWarn() {
    	final var mockLogger  = mock(Logger.class);
    	when(mockLogger.isWarnEnabled()).thenReturn(true);
    	
    	final var logger = new TestPrefixedLogger(mockLogger);
    	
    	logger.warn("Hello");
        verify(mockLogger).warn("[TEST] Hello");
        
        logger.warn("Hello {}!", "World");
        verify(mockLogger).warn("[TEST] Hello {}!", "World");
        
        logger.warn("Hello {} and {}!", "Max", "Bob");
        verify(mockLogger).warn("[TEST] Hello {} and {}!", "Max", "Bob");
        
        logger.warn("Error", exception);
        verify(mockLogger).warn("[TEST] Error", exception);
        
        logger.atWarn().setMessage("Bye {}!").addArgument("Test").log();
        verify(mockLogger).warn("[TEST] Bye {}!", new Object[]{"Test"});
    }
    
    @Test
    public void testError() {
    	final var mockLogger  = mock(Logger.class);
    	when(mockLogger.isErrorEnabled()).thenReturn(true);
    	
    	final var logger = new TestPrefixedLogger(mockLogger);
    	
    	logger.error("Hello");
        verify(mockLogger).error("[TEST] Hello");
        
        logger.error("Hello {}!", "World");
        verify(mockLogger).error("[TEST] Hello {}!", "World");
        
        logger.error("Hello {} and {}!", "Max", "Bob");
        verify(mockLogger).error("[TEST] Hello {} and {}!", "Max", "Bob");
        
        logger.error("Error", exception);
        verify(mockLogger).error("[TEST] Error", exception);
        
        logger.atError().setMessage("Bye {}!").addArgument("Test").log();
        verify(mockLogger).error("[TEST] Bye {}!", new Object[]{"Test"});
    }
    
    

}
