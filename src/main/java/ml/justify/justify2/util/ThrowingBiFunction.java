package ml.justify.justify2.util;

public interface ThrowingBiFunction<T, U, R> {
  R tryApply(T t, U u) throws Throwable;
}
