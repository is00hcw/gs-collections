/*
 * Copyright 2014 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gs.collections.impl.stack.mutable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.gs.collections.api.LazyIterable;
import com.gs.collections.api.RichIterable;
import com.gs.collections.api.bag.MutableBag;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.block.function.Function0;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.block.function.primitive.BooleanFunction;
import com.gs.collections.api.block.function.primitive.ByteFunction;
import com.gs.collections.api.block.function.primitive.CharFunction;
import com.gs.collections.api.block.function.primitive.DoubleFunction;
import com.gs.collections.api.block.function.primitive.DoubleObjectToDoubleFunction;
import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatObjectToFloatFunction;
import com.gs.collections.api.block.function.primitive.IntFunction;
import com.gs.collections.api.block.function.primitive.IntObjectToIntFunction;
import com.gs.collections.api.block.function.primitive.LongFunction;
import com.gs.collections.api.block.function.primitive.LongObjectToLongFunction;
import com.gs.collections.api.block.function.primitive.ShortFunction;
import com.gs.collections.api.block.predicate.Predicate;
import com.gs.collections.api.block.predicate.Predicate2;
import com.gs.collections.api.block.procedure.Procedure;
import com.gs.collections.api.block.procedure.Procedure2;
import com.gs.collections.api.block.procedure.primitive.ObjectIntProcedure;
import com.gs.collections.api.collection.primitive.MutableBooleanCollection;
import com.gs.collections.api.collection.primitive.MutableByteCollection;
import com.gs.collections.api.collection.primitive.MutableCharCollection;
import com.gs.collections.api.collection.primitive.MutableDoubleCollection;
import com.gs.collections.api.collection.primitive.MutableFloatCollection;
import com.gs.collections.api.collection.primitive.MutableIntCollection;
import com.gs.collections.api.collection.primitive.MutableLongCollection;
import com.gs.collections.api.collection.primitive.MutableShortCollection;
import com.gs.collections.api.list.ListIterable;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.api.map.MutableMap;
import com.gs.collections.api.map.sorted.MutableSortedMap;
import com.gs.collections.api.multimap.MutableMultimap;
import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.api.partition.stack.PartitionMutableStack;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.api.set.sorted.MutableSortedSet;
import com.gs.collections.api.stack.ImmutableStack;
import com.gs.collections.api.stack.MutableStack;
import com.gs.collections.api.stack.primitive.MutableBooleanStack;
import com.gs.collections.api.stack.primitive.MutableByteStack;
import com.gs.collections.api.stack.primitive.MutableCharStack;
import com.gs.collections.api.stack.primitive.MutableDoubleStack;
import com.gs.collections.api.stack.primitive.MutableFloatStack;
import com.gs.collections.api.stack.primitive.MutableIntStack;
import com.gs.collections.api.stack.primitive.MutableLongStack;
import com.gs.collections.api.stack.primitive.MutableShortStack;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.UnmodifiableIteratorAdapter;
import com.gs.collections.impl.block.procedure.MutatingAggregationProcedure;
import com.gs.collections.impl.block.procedure.NonMutatingAggregationProcedure;
import com.gs.collections.impl.map.mutable.UnifiedMap;

/**
 * A synchronized view of a {@link MutableStack}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * standard JDK iterator or JDK 5 for loop, as per {@link Collections#synchronizedCollection(Collection)}.
 *
 * @see MutableStack#asSynchronized()
 */
public final class SynchronizedStack<T> implements MutableStack<T>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableStack<T> delegate;

    public SynchronizedStack(MutableStack<T> newStack)
    {
        this(newStack, null);
    }

    public SynchronizedStack(MutableStack<T> newStack, Object newLock)
    {
        if (newStack == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedStack on a null stack");
        }
        this.delegate = newStack;
        this.lock = newLock == null ? this : newLock;
    }

    /**
     * This method will take a MutableStack and wrap it directly in a SynchronizedStack.
     */
    public static <T, S extends MutableStack<T>> SynchronizedStack<T> of(S stack)
    {
        return new SynchronizedStack<T>(stack);
    }

    public T pop()
    {
        synchronized (this.lock)
        {
            return this.delegate.pop();
        }
    }

    public ListIterable<T> pop(int count)
    {
        synchronized (this.lock)
        {
            return this.delegate.pop(count);
        }
    }

    public <R extends Collection<T>> R pop(int count, R targetCollection)
    {
        synchronized (this.lock)
        {
            return this.delegate.pop(count, targetCollection);
        }
    }

    public <R extends MutableStack<T>> R pop(int count, R targetStack)
    {
        synchronized (this.lock)
        {
            return this.delegate.pop(count, targetStack);
        }
    }

    public void clear()
    {
        synchronized (this.lock)
        {
            this.delegate.clear();
        }
    }

    public void push(T item)
    {
        synchronized (this.lock)
        {
            this.delegate.push(item);
        }
    }

    public T peek()
    {
        synchronized (this.lock)
        {
            return this.delegate.peek();
        }
    }

    public ListIterable<T> peek(int count)
    {
        synchronized (this.lock)
        {
            return this.delegate.peek(count);
        }
    }

    public T peekAt(int index)
    {
        synchronized (this.lock)
        {
            return this.delegate.peekAt(index);
        }
    }

    public MutableStack<T> select(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.select(predicate);
        }
    }

    public <P> MutableStack<T> selectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.selectWith(predicate, parameter);
        }
    }

    public <R extends Collection<T>> R select(Predicate<? super T> predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.select(predicate, target);
        }
    }

    public <P, R extends Collection<T>> R selectWith(Predicate2<? super T, ? super P> predicate, P parameter, R targetCollection)
    {
        synchronized (this.lock)
        {
            return this.delegate.selectWith(predicate, parameter, targetCollection);
        }
    }

    public MutableStack<T> reject(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.reject(predicate);
        }
    }

    public <P> MutableStack<T> rejectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.rejectWith(predicate, parameter);
        }
    }

    public <R extends Collection<T>> R reject(Predicate<? super T> predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.reject(predicate, target);
        }
    }

    public <P, R extends Collection<T>> R rejectWith(Predicate2<? super T, ? super P> predicate, P parameter, R targetCollection)
    {
        synchronized (this.lock)
        {
            return this.delegate.rejectWith(predicate, parameter, targetCollection);
        }
    }

    public PartitionMutableStack<T> partition(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.partition(predicate);
        }
    }

    public <P> PartitionMutableStack<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.partitionWith(predicate, parameter);
        }
    }

    public <S> RichIterable<S> selectInstancesOf(Class<S> clazz)
    {
        synchronized (this.lock)
        {
            return this.delegate.selectInstancesOf(clazz);
        }
    }

    public <V> MutableStack<V> collect(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.collect(function);
        }
    }

    public MutableBooleanStack collectBoolean(BooleanFunction<? super T> booleanFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectBoolean(booleanFunction);
        }
    }

    public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super T> booleanFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectBoolean(booleanFunction, target);
        }
    }

    public MutableByteStack collectByte(ByteFunction<? super T> byteFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectByte(byteFunction);
        }
    }

    public <R extends MutableByteCollection> R collectByte(ByteFunction<? super T> byteFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectByte(byteFunction, target);
        }
    }

    public MutableCharStack collectChar(CharFunction<? super T> charFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectChar(charFunction);
        }
    }

    public <R extends MutableCharCollection> R collectChar(CharFunction<? super T> charFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectChar(charFunction, target);
        }
    }

    public MutableDoubleStack collectDouble(DoubleFunction<? super T> doubleFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectDouble(doubleFunction);
        }
    }

    public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super T> doubleFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectDouble(doubleFunction, target);
        }
    }

    public MutableFloatStack collectFloat(FloatFunction<? super T> floatFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectFloat(floatFunction);
        }
    }

    public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super T> floatFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectFloat(floatFunction, target);
        }
    }

    public MutableIntStack collectInt(IntFunction<? super T> intFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectInt(intFunction);
        }
    }

    public <R extends MutableIntCollection> R collectInt(IntFunction<? super T> intFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectInt(intFunction, target);
        }
    }

    public MutableLongStack collectLong(LongFunction<? super T> longFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectLong(longFunction);
        }
    }

    public <R extends MutableLongCollection> R collectLong(LongFunction<? super T> longFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectLong(longFunction, target);
        }
    }

    public MutableShortStack collectShort(ShortFunction<? super T> shortFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectShort(shortFunction);
        }
    }

    public <R extends MutableShortCollection> R collectShort(ShortFunction<? super T> shortFunction, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectShort(shortFunction, target);
        }
    }

    public <V, R extends Collection<V>> R collect(Function<? super T, ? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collect(function, target);
        }
    }

    public <P, V> MutableStack<V> collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectWith(function, parameter);
        }
    }

    public <P, V, R extends Collection<V>> R collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter, R targetCollection)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectWith(function, parameter, targetCollection);
        }
    }

    public <V> MutableStack<V> collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectIf(predicate, function);
        }
    }

    public <V, R extends Collection<V>> R collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.collectIf(predicate, function, target);
        }
    }

    public <V> MutableStack<V> flatCollect(Function<? super T, ? extends Iterable<V>> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.flatCollect(function);
        }
    }

    public <V, R extends Collection<V>> R flatCollect(Function<? super T, ? extends Iterable<V>> function, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.flatCollect(function, target);
        }
    }

    public <S, R extends Collection<Pair<T, S>>> R zip(Iterable<S> that, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.zip(that, target);
        }
    }

    public <S> MutableStack<Pair<T, S>> zip(Iterable<S> that)
    {
        synchronized (this.lock)
        {
            return this.delegate.zip(that);
        }
    }

    public <R extends Collection<Pair<T, Integer>>> R zipWithIndex(R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.zipWithIndex(target);
        }
    }

    public MutableStack<Pair<T, Integer>> zipWithIndex()
    {
        synchronized (this.lock)
        {
            return this.delegate.zipWithIndex();
        }
    }

    public int size()
    {
        synchronized (this.lock)
        {
            return this.delegate.size();
        }
    }

    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.delegate.isEmpty();
        }
    }

    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.delegate.notEmpty();
        }
    }

    public T getFirst()
    {
        synchronized (this.lock)
        {
            return this.delegate.getFirst();
        }
    }

    public T getLast()
    {
        return this.delegate.getLast();
    }

    public boolean contains(Object object)
    {
        synchronized (this.lock)
        {
            return this.delegate.contains(object);
        }
    }

    public boolean containsAllIterable(Iterable<?> source)
    {
        synchronized (this.lock)
        {
            return this.delegate.containsAllIterable(source);
        }
    }

    public boolean containsAll(Collection<?> source)
    {
        synchronized (this.lock)
        {
            return this.delegate.containsAll(source);
        }
    }

    public boolean containsAllArguments(Object... elements)
    {
        synchronized (this.lock)
        {
            return this.delegate.containsAllArguments(elements);
        }
    }

    public T detect(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.detect(predicate);
        }
    }

    public <P> T detectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.detectWith(predicate, parameter);
        }
    }

    public T detectIfNone(Predicate<? super T> predicate, Function0<? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.detectIfNone(predicate, function);
        }
    }

    public <P> T detectWithIfNone(Predicate2<? super T, ? super P> predicate, P parameter, Function0<? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.detectWithIfNone(predicate, parameter, function);
        }
    }

    public int count(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.count(predicate);
        }
    }

    public <P> int countWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.countWith(predicate, parameter);
        }
    }

    public boolean anySatisfy(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.anySatisfy(predicate);
        }
    }

    public <P> boolean anySatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.anySatisfyWith(predicate, parameter);
        }
    }

    public boolean allSatisfy(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.allSatisfy(predicate);
        }
    }

    public <P> boolean allSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.allSatisfyWith(predicate, parameter);
        }
    }

    public boolean noneSatisfy(Predicate<? super T> predicate)
    {
        synchronized (this.lock)
        {
            return this.delegate.noneSatisfy(predicate);
        }
    }

    public <P> boolean noneSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        synchronized (this.lock)
        {
            return this.delegate.noneSatisfyWith(predicate, parameter);
        }
    }

    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super T, ? extends IV> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.injectInto(injectedValue, function);
        }
    }

    public int injectInto(int injectedValue, IntObjectToIntFunction<? super T> intObjectToIntFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.injectInto(injectedValue, intObjectToIntFunction);
        }
    }

    public long injectInto(long injectedValue, LongObjectToLongFunction<? super T> longObjectToLongFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.injectInto(injectedValue, longObjectToLongFunction);
        }
    }

    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super T> floatObjectToFloatFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.injectInto(injectedValue, floatObjectToFloatFunction);
        }
    }

    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super T> doubleObjectToDoubleFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.injectInto(injectedValue, doubleObjectToDoubleFunction);
        }
    }

    public MutableList<T> toList()
    {
        synchronized (this.lock)
        {
            return this.delegate.toList();
        }
    }

    public MutableList<T> toSortedList()
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedList();
        }
    }

    public MutableList<T> toSortedList(Comparator<? super T> comparator)
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedList(comparator);
        }
    }

    public <V extends Comparable<? super V>> MutableList<T> toSortedListBy(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedListBy(function);
        }
    }

    public MutableSet<T> toSet()
    {
        synchronized (this.lock)
        {
            return this.delegate.toSet();
        }
    }

    public MutableSortedSet<T> toSortedSet()
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedSet();
        }
    }

    public MutableSortedSet<T> toSortedSet(Comparator<? super T> comparator)
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedSet(comparator);
        }
    }

    public MutableStack<T> toStack()
    {
        synchronized (this.lock)
        {
            return this.delegate.toStack();
        }
    }

    public ImmutableStack<T> toImmutable()
    {
        synchronized (this.lock)
        {
            return this.delegate.toImmutable();
        }
    }

    public <V extends Comparable<? super V>> MutableSortedSet<T> toSortedSetBy(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedSetBy(function);
        }
    }

    public MutableBag<T> toBag()
    {
        synchronized (this.lock)
        {
            return this.delegate.toBag();
        }
    }

    public <NK, NV> MutableMap<NK, NV> toMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.toMap(keyFunction, valueFunction);
        }
    }

    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedMap(keyFunction, valueFunction);
        }
    }

    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Comparator<? super NK> comparator, Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.toSortedMap(comparator, keyFunction, valueFunction);
        }
    }

    public LazyIterable<T> asLazy()
    {
        synchronized (this.lock)
        {
            return this.delegate.asLazy();
        }
    }

    public Object[] toArray()
    {
        synchronized (this.lock)
        {
            return this.delegate.toArray();
        }
    }

    public <T> T[] toArray(T[] a)
    {
        synchronized (this.lock)
        {
            return this.delegate.toArray(a);
        }
    }

    public T min(Comparator<? super T> comparator)
    {
        synchronized (this.lock)
        {
            return this.delegate.min(comparator);
        }
    }

    public T max(Comparator<? super T> comparator)
    {
        synchronized (this.lock)
        {
            return this.delegate.max(comparator);
        }
    }

    public T min()
    {
        synchronized (this.lock)
        {
            return this.delegate.min();
        }
    }

    public T max()
    {
        synchronized (this.lock)
        {
            return this.delegate.max();
        }
    }

    public <V extends Comparable<? super V>> T minBy(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.minBy(function);
        }
    }

    public <V extends Comparable<? super V>> T maxBy(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.maxBy(function);
        }
    }

    public long sumOfInt(IntFunction<? super T> intFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.sumOfInt(intFunction);
        }
    }

    public double sumOfFloat(FloatFunction<? super T> floatFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.sumOfFloat(floatFunction);
        }
    }

    public long sumOfLong(LongFunction<? super T> longFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.sumOfLong(longFunction);
        }
    }

    public double sumOfDouble(DoubleFunction<? super T> doubleFunction)
    {
        synchronized (this.lock)
        {
            return this.delegate.sumOfDouble(doubleFunction);
        }
    }

    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.delegate.makeString();
        }
    }

    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.delegate.makeString(separator);
        }
    }

    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.delegate.makeString(start, separator, end);
        }
    }

    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.delegate.appendString(appendable);
        }
    }

    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.delegate.appendString(appendable, separator);
        }
    }

    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            this.delegate.appendString(appendable, start, separator, end);
        }
    }

    public <V> MutableListMultimap<V, T> groupBy(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.groupBy(function);
        }
    }

    public <V, R extends MutableMultimap<V, T>> R groupBy(Function<? super T, ? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.groupBy(function, target);
        }
    }

    public <V> MutableListMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.groupByEach(function);
        }
    }

    public <V, R extends MutableMultimap<V, T>> R groupByEach(Function<? super T, ? extends Iterable<V>> function, R target)
    {
        synchronized (this.lock)
        {
            return this.delegate.groupByEach(function, target);
        }
    }

    public <V> MutableMap<V, T> groupByUniqueKey(Function<? super T, ? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.delegate.groupByUniqueKey(function);
        }
    }

    public RichIterable<RichIterable<T>> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.delegate.chunk(size);
        }
    }

    public void forEach(Procedure<? super T> procedure)
    {
        synchronized (this.lock)
        {
            this.delegate.forEach(procedure);
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.delegate.toString();
        }
    }

    public void forEachWithIndex(ObjectIntProcedure<? super T> objectIntProcedure)
    {
        synchronized (this.lock)
        {
            this.delegate.forEachWithIndex(objectIntProcedure);
        }
    }

    public <P> void forEachWith(Procedure2<? super T, ? super P> procedure, P parameter)
    {
        synchronized (this.lock)
        {
            this.delegate.forEachWith(procedure, parameter);
        }
    }

    public MutableStack<T> asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return this.delegate.asUnmodifiable();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        synchronized (this.lock)
        {
            return this.delegate.equals(obj);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.lock)
        {
            return this.delegate.hashCode();
        }
    }

    public MutableStack<T> asSynchronized()
    {
        synchronized (this.lock)
        {
            return this;
        }
    }

    public Iterator<T> iterator()
    {
        return new UnmodifiableIteratorAdapter<T>(this.delegate.iterator());
    }

    public <K, V> MutableMap<K, V> aggregateInPlaceBy(
            Function<? super T, ? extends K> groupBy,
            Function0<? extends V> zeroValueFactory,
            Procedure2<? super V, ? super T> mutatingAggregator)
    {
        MutableMap<K, V> map = UnifiedMap.newMap();
        this.forEach(new MutatingAggregationProcedure<T, K, V>(map, groupBy, zeroValueFactory, mutatingAggregator));
        return map;
    }

    public <K, V> MutableMap<K, V> aggregateBy(
            Function<? super T, ? extends K> groupBy,
            Function0<? extends V> zeroValueFactory,
            Function2<? super V, ? super T, ? extends V> nonMutatingAggregator)
    {
        MutableMap<K, V> map = UnifiedMap.newMap();
        this.forEach(new NonMutatingAggregationProcedure<T, K, V>(map, groupBy, zeroValueFactory, nonMutatingAggregator));
        return map;
    }
}
