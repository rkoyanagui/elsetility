package com.rkoyanagui.wrappers;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;
import org.awaitility.core.ConditionTimeoutException;

public class CallableWrapper
{
  protected static final String BLEW_MAX_ATTEMPTS_MSG =
      "Reached max no. of attempts (%d). Will not retry.";

  protected CallableWrapper()
  {
  }

  public static <T> Callable<T> wrap(
      final Callable<T> supplier,
      final Runnable orElseDo,
      final LongAdder counter,
      final Integer maxAttempts)
  {
    return () ->
    {
      final int count = counter.intValue();
      counter.increment();
      if (count < maxAttempts)
      {
        if (count > 0)
        {
          // If this is not the very first try, so it is the second or third or so on try,
          // then it means the last try failed, either because of an exception when
          // supplying the last value, or because the value failed the test.
          // Which means it is the right time to perform some kind of action
          // that may turn things around, right before a new value is supplied.
          orElseDo.run();
        }
        // If the count is below the maximum number of attempts,
        // then tries to supply a new value for evaluation.
        return supplier.call();
      }
      else
      {
        throw new ConditionTimeoutException(String.format(BLEW_MAX_ATTEMPTS_MSG, maxAttempts));
      }
    };
  }
}
