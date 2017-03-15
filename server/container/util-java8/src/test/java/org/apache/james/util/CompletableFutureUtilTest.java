/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.steveash.guavate.Guavate;

public class CompletableFutureUtilTest {

    @Test
    public void allOfShouldUnboxEmptyStream() {
        assertThat(
            CompletableFutureUtil.allOf(Stream.empty())
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    public void allOfShouldUnboxStream() {
        long value1 = 18L;
        long value2 = 19L;
        long value3 = 20L;
        assertThat(
            CompletableFutureUtil.allOf(
                Stream.of(
                    CompletableFuture.completedFuture(value1),
                    CompletableFuture.completedFuture(value2),
                    CompletableFuture.completedFuture(value3)))
                .join()
                .collect(Guavate.toImmutableList()))
            .containsOnly(value1, value2, value3);
    }

    @Test
    public void allOfShouldPreserveOrder() {
        long value1 = 18L;
        long value2 = 19L;
        long value3 = 20L;
        long value4 = 21L;
        long value5 = 22L;
        long value6 = 23L;
        long value7 = 24L;
        long value8 = 25L;
        long value9 = 26L;
        long value10 = 27L;
        assertThat(
            CompletableFutureUtil.allOf(
                Stream.of(
                    CompletableFuture.completedFuture(value1),
                    CompletableFuture.completedFuture(value2),
                    CompletableFuture.completedFuture(value3),
                    CompletableFuture.completedFuture(value4),
                    CompletableFuture.completedFuture(value5),
                    CompletableFuture.completedFuture(value6),
                    CompletableFuture.completedFuture(value7),
                    CompletableFuture.completedFuture(value8),
                    CompletableFuture.completedFuture(value9),
                    CompletableFuture.completedFuture(value10)))
                .join()
                .collect(Guavate.toImmutableList()))
            .containsExactly(value1, value2, value3, value4, value5, value6, value7, value8, value9, value10);
    }

    @Test
    public void allOfShouldWorkOnVeryLargeStream() {
        CompletableFutureUtil.allOf(
            IntStream.range(0, 100000)
                .boxed()
                .map(CompletableFuture::completedFuture))
            .join();
    }

    @Test
    public void mapShouldMapOnStreamInsideACompletableFuturOfStream() {
        CompletableFuture<Stream<Integer>> futurOfInteger = CompletableFuture.completedFuture(Stream.of(1, 2, 3));

        assertThat(
            CompletableFutureUtil.map(futurOfInteger, integer ->
                integer * 2)
                .join()
                .collect(Guavate.toImmutableList()))
            .containsExactly(2, 4, 6);
    }

    @Test
    public void mapShouldReturnEmptyStreamWhenGivenAnEmptyStream() {
        CompletableFuture<Stream<Integer>> futurOfInteger = CompletableFuture.completedFuture(Stream.of());

        assertThat(
            CompletableFutureUtil.map(futurOfInteger, integer ->
                integer * 2)
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    public void thenComposeOnAllShouldMapOnStreamInsideACompletableFuturOfStreamAndTransformTheResultingStreamOfCompletableFutureIntoACompletableOfStreamAndFlatIt() {
        CompletableFuture<Stream<Integer>> futurOfInteger = CompletableFuture.completedFuture(Stream.of(1, 2, 3));

        assertThat(
            CompletableFutureUtil.thenComposeOnAll(futurOfInteger, integer ->
                CompletableFuture.completedFuture(integer * 2))
                .join()
                .collect(Guavate.toImmutableList()))
            .containsExactly(2, 4, 6);
    }

    @Test
    public void thenComposeOnAllOnEmptyStreamShouldReturnAnEmptyStream() {
        CompletableFuture<Stream<Integer>> futurOfInteger = CompletableFuture.completedFuture(Stream.of());

        assertThat(
            CompletableFutureUtil.thenComposeOnAll(futurOfInteger, integer ->
                CompletableFuture.completedFuture(integer * 2))
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    public void keepValueShouldCompleteWhenTheGivenCompletableFutureEnd() {
        final AtomicInteger numOfFutureExecution = new AtomicInteger(0);

        Supplier<CompletableFuture<Void>> future = () ->
            CompletableFuture.runAsync(numOfFutureExecution::incrementAndGet);

        assertThat(
            CompletableFutureUtil.keepValue(future, 42)
                .join())
            .isEqualTo(42);

        assertThat(
            numOfFutureExecution.get())
            .isEqualTo(1);
    }

    @Test
    public void keepValueShouldReturnNullWithNullValue() {
        Supplier<CompletableFuture<Void>> future = () ->
            CompletableFuture.completedFuture(null);

        assertThat(
            CompletableFutureUtil.keepValue(future, null)
                .join())
            .isNull();
    }
}
