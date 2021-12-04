package com.rkoyanagui.wrappers;

import java.util.concurrent.atomic.LongAdder;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssertionWrapper
{
  private static final Logger LOG = LoggerFactory.getLogger(AssertionWrapper.class);
  protected static final String BLEW_MAX_ATTEMPTS_MSG =
      "Reached max no. of attempts (%d). Will not retry.";

  public static ThrowingRunnable wrap(
      final ThrowingRunnable assertion,
      final Runnable orElseDo,
      final LongAdder counter,
      final Integer maxAttempts)
  {
    return () ->
    {
      final int count = counter.intValue();
      counter.increment();
      LOG.debug("Attempt no.={}, max_attempts={}", count, maxAttempts);
      if (count < maxAttempts)
      {
        LOG.debug("Retrying...");
        if (count > 0)
        {
          // If this is not the very first try, so it is the second or third or so on try,
          // then it means the last try failed, either because of an exception when
          // supplying the last value, or because the value failed the test.
          // Which means it is the right time to perform some kind of action
          // that may turn things around, right before a new value is supplied.
          LOG.debug("Performing 'orElseDo' action...");
          try
          {
            orElseDo.run();
          }
          catch (Exception x)
          {
            LOG.debug("Error when performing 'orElseDo' action.", x);
            throw x;
          }
        }
        // If the count is below the maximum number of attempts,
        // then tries to supply a new value for evaluation.
        try
        {
          assertion.run();
        }
        catch (Exception x)
        {
          LOG.debug("Error when trying to supply a new value for evaluation.", x);
          throw x;
        }
      }
      else
      {
        final String msg = String.format(BLEW_MAX_ATTEMPTS_MSG, maxAttempts);
        LOG.debug(msg);
        throw new ConditionTimeoutException(msg);
      }
    };
  }
}
