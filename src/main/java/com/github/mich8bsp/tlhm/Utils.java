package com.github.mich8bsp.tlhm;

import akka.dispatch.Mapper;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.function.Function;

/**
 * Created by Michael Bespalov on 28-Jun-17.
 */
public class Utils {

    // wrapping function in scala Function1 because for some reason Function1 is not a functional interface
    // so we can't pass func directly into methods that require Function1 like map of Future
    public static <S, T> Mapper<S, T> getMapper(Function<S, T> func){
        return new Mapper<S, T>() {
            public T apply(S s) {
                return func.apply(s);
            }
        };
    }

    public static <T> T awaitFuture(Future<T> future, Timeout timeout){
        try{
            return Await.result(future, timeout.duration());
        } catch (Exception e){
            return null;
        }
    }
}
