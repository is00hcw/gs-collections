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

package com.gs.collections.impl.list.mutable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;

import com.gs.collections.api.LazyIterable;
import com.gs.collections.api.annotation.Beta;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.block.function.Function0;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.block.function.Function3;
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
import com.gs.collections.api.list.MutableList;
import com.gs.collections.api.list.ParallelListIterable;
import com.gs.collections.api.list.primitive.MutableBooleanList;
import com.gs.collections.api.list.primitive.MutableByteList;
import com.gs.collections.api.list.primitive.MutableCharList;
import com.gs.collections.api.list.primitive.MutableDoubleList;
import com.gs.collections.api.list.primitive.MutableFloatList;
import com.gs.collections.api.list.primitive.MutableIntList;
import com.gs.collections.api.list.primitive.MutableLongList;
import com.gs.collections.api.list.primitive.MutableShortList;
import com.gs.collections.api.partition.list.PartitionMutableList;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.block.factory.Comparators;
import com.gs.collections.impl.block.factory.Predicates2;
import com.gs.collections.impl.block.factory.Procedures2;
import com.gs.collections.impl.block.procedure.CountProcedure;
import com.gs.collections.impl.block.procedure.FastListCollectIfProcedure;
import com.gs.collections.impl.block.procedure.FastListCollectProcedure;
import com.gs.collections.impl.block.procedure.FastListRejectProcedure;
import com.gs.collections.impl.block.procedure.FastListSelectProcedure;
import com.gs.collections.impl.block.procedure.MultimapPutProcedure;
import com.gs.collections.impl.lazy.AbstractLazyIterable;
import com.gs.collections.impl.lazy.parallel.AbstractBatch;
import com.gs.collections.impl.lazy.parallel.list.AbstractParallelListIterable;
import com.gs.collections.impl.lazy.parallel.list.CollectListBatch;
import com.gs.collections.impl.lazy.parallel.list.DistinctBatch;
import com.gs.collections.impl.lazy.parallel.list.ListBatch;
import com.gs.collections.impl.lazy.parallel.list.RootListBatch;
import com.gs.collections.impl.lazy.parallel.list.SelectListBatch;
import com.gs.collections.impl.lazy.parallel.set.UnsortedSetBatch;
import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import com.gs.collections.impl.list.mutable.primitive.ByteArrayList;
import com.gs.collections.impl.list.mutable.primitive.CharArrayList;
import com.gs.collections.impl.list.mutable.primitive.DoubleArrayList;
import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import com.gs.collections.impl.list.mutable.primitive.ShortArrayList;
import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import com.gs.collections.impl.parallel.BatchIterable;
import com.gs.collections.impl.partition.list.PartitionFastList;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import com.gs.collections.impl.tuple.Tuples;
import com.gs.collections.impl.utility.ArrayIterate;
import com.gs.collections.impl.utility.ArrayListIterate;
import com.gs.collections.impl.utility.Iterate;
import com.gs.collections.impl.utility.ListIterate;
import com.gs.collections.impl.utility.internal.InternalArrayIterate;
import net.jcip.annotations.NotThreadSafe;

/**
 * FastList is an attempt to provide the same functionality as ArrayList without the support for concurrent
 * modification exceptions.  It also attempts to correct the problem with subclassing ArrayList
 * in that the data elements are protected, not private.  It is this issue that caused this class
 * to be created in the first place.  The intent was to provide optimized internal iterators which use direct access
 * against the array of items, which is currently not possible by subclassing ArrayList.
 * <p/>
 * An empty FastList created by calling the default constructor starts with a shared reference to a static
 * empty array (DEFAULT_SIZED_EMPTY_ARRAY).  This makes empty FastLists very memory efficient.  The
 * first call to add will lazily create an array of size 10.
 * <p/>
 * An empty FastList created by calling the pre-size constructor with a value of 0 (new FastList(0)) starts
 * with a shared reference to a static  empty array (ZERO_SIZED_ARRAY).  This makes FastLists presized to 0 very
 * memory efficient as well.  The first call to add will lazily create an array of size 1.
 */
@NotThreadSafe
public class FastList<T>
        extends AbstractMutableList<T>
        implements Externalizable, RandomAccess, BatchIterable<T>
{
    private static final long serialVersionUID = 1L;
    private static final Object[] DEFAULT_SIZED_EMPTY_ARRAY = {};
    private static final Object[] ZERO_SIZED_ARRAY = {};
    private static final int MAXIMUM_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected int size;
    protected transient T[] items = (T[]) DEFAULT_SIZED_EMPTY_ARRAY;

    public FastList()
    {
    }

    public FastList(int initialCapacity)
    {
        this.items = initialCapacity == 0 ? (T[]) ZERO_SIZED_ARRAY : (T[]) new Object[initialCapacity];
    }

    protected FastList(T[] array)
    {
        this(array.length, array);
    }

    protected FastList(int size, T[] array)
    {
        this.size = size;
        this.items = array;
    }

    public FastList(Collection<? extends T> source)
    {
        this.items = (T[]) source.toArray();
        this.size = this.items.length;
    }

    public static <E> FastList<E> newList()
    {
        return new FastList<E>();
    }

    public static <E> FastList<E> wrapCopy(E... array)
    {
        E[] newArray = (E[]) new Object[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return new FastList<E>(newArray);
    }

    public static <E> FastList<E> newList(int initialCapacity)
    {
        return new FastList<E>(initialCapacity);
    }

    public static <E> FastList<E> newList(Iterable<? extends E> source)
    {
        return FastList.newListWith((E[]) Iterate.toArray(source));
    }

    /**
     * Creates a new list using the passed {@code elements} argument as the backing store.
     * <p/>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    public static <E> FastList<E> newListWith(E... elements)
    {
        return new FastList<E>(elements);
    }

    /**
     * Creates a new FastList pre-sized to the specified size filled with default values generated by the specified function.
     *
     * @since 3.0
     */
    public static <E> FastList<E> newWithNValues(int size, Function0<E> factory)
    {
        FastList<E> newFastList = FastList.newList(size);
        for (int i = 0; i < size; i++)
        {
            newFastList.add(factory.value());
        }
        return newFastList;
    }

    @Override
    public FastList<T> clone()
    {
        FastList<T> result = (FastList<T>) super.clone();
        if (this.items.length > 0)
        {
            result.items = this.items.clone();
        }
        return result;
    }

    public void clear()
    {
        Arrays.fill(this.items, null);
        this.size = 0;
    }

    @Override
    public void forEach(int from, int to, Procedure<? super T> procedure)
    {
        ListIterate.rangeCheck(from, to, this.size);
        InternalArrayIterate.forEachWithoutChecks(this.items, from, to, procedure);
    }

    @Override
    public void forEachWithIndex(int from, int to, ObjectIntProcedure<? super T> objectIntProcedure)
    {
        ListIterate.rangeCheck(from, to, this.size);
        InternalArrayIterate.forEachWithIndexWithoutChecks(this.items, from, to, objectIntProcedure);
    }

    public void batchForEach(Procedure<? super T> procedure, int sectionIndex, int sectionCount)
    {
        int sectionSize = this.size() / sectionCount;
        int start = sectionSize * sectionIndex;
        int end = sectionIndex == sectionCount - 1 ? this.size() : start + sectionSize;
        if (procedure instanceof FastListSelectProcedure)
        {
            this.batchFastListSelect(start, end, (FastListSelectProcedure<T>) procedure);
        }
        else if (procedure instanceof FastListCollectProcedure)
        {
            this.batchFastListCollect(start, end, (FastListCollectProcedure<T, ?>) procedure);
        }
        else if (procedure instanceof FastListCollectIfProcedure)
        {
            this.batchFastListCollectIf(start, end, (FastListCollectIfProcedure<T, ?>) procedure);
        }
        else if (procedure instanceof CountProcedure)
        {
            this.batchCount(start, end, (CountProcedure<T>) procedure);
        }
        else if (procedure instanceof FastListRejectProcedure)
        {
            this.batchReject(start, end, (FastListRejectProcedure<T>) procedure);
        }
        else if (procedure instanceof MultimapPutProcedure)
        {
            this.batchGroupBy(start, end, (MultimapPutProcedure<?, T>) procedure);
        }
        else
        {
            for (int i = start; i < end; i++)
            {
                procedure.value(this.items[i]);
            }
        }
    }

    /**
     * Implemented to avoid megamorphic call on castProcedure.
     */
    private void batchGroupBy(int start, int end, MultimapPutProcedure<?, T> castProcedure)
    {
        for (int i = start; i < end; i++)
        {
            castProcedure.value(this.items[i]);
        }
    }

    /**
     * Implemented to avoid megamorphic call on castProcedure.
     */
    private void batchReject(int start, int end, FastListRejectProcedure<T> castProcedure)
    {
        for (int i = start; i < end; i++)
        {
            castProcedure.value(this.items[i]);
        }
    }

    /**
     * Implemented to avoid megamorphic call on castProcedure.
     */
    private void batchCount(int start, int end, CountProcedure<T> castProcedure)
    {
        for (int i = start; i < end; i++)
        {
            castProcedure.value(this.items[i]);
        }
    }

    /**
     * Implemented to avoid megamorphic call on castProcedure.
     */
    private void batchFastListCollectIf(int start, int end, FastListCollectIfProcedure<T, ?> castProcedure)
    {
        for (int i = start; i < end; i++)
        {
            castProcedure.value(this.items[i]);
        }
    }

    /**
     * Implemented to avoid megamorphic call on castProcedure.
     */
    private void batchFastListCollect(int start, int end, FastListCollectProcedure<T, ?> castProcedure)
    {
        for (int i = start; i < end; i++)
        {
            castProcedure.value(this.items[i]);
        }
    }

    /**
     * Implemented to avoid megamorphic call on castProcedure.
     */
    private void batchFastListSelect(int start, int end, FastListSelectProcedure<T> castProcedure)
    {
        for (int i = start; i < end; i++)
        {
            castProcedure.value(this.items[i]);
        }
    }

    public int getBatchCount(int batchSize)
    {
        return Math.max(1, this.size() / batchSize);
    }

    public <E> E[] toArray(E[] array, int sourceFromIndex, int sourceToIndex, int destinationIndex)
    {
        System.arraycopy(this.items, sourceFromIndex, array, destinationIndex, sourceToIndex - sourceFromIndex + 1);
        return array;
    }

    public <E> E[] toArray(int sourceFromIndex, int sourceToIndex)
    {
        return this.toArray((E[]) new Object[sourceToIndex - sourceFromIndex + 1], sourceFromIndex, sourceToIndex, 0);
    }

    @Override
    public FastList<T> sortThis(Comparator<? super T> comparator)
    {
        ArrayIterate.sort(this.items, this.size, comparator);
        return this;
    }

    @Override
    public FastList<T> sortThis()
    {
        ArrayIterate.sort(this.items, this.size, null);
        return this;
    }

    @Override
    public FastList<T> reverseThis()
    {
        ArrayIterate.reverse(this.items, this.size);
        return this;
    }

    @Override
    public boolean addAll(Collection<? extends T> source)
    {
        if (source.isEmpty())
        {
            return false;
        }

        if (source.getClass() == FastList.class)
        {
            this.addAllFastList((FastList<T>) source);
        }
        else if (source.getClass() == ArrayList.class)
        {
            this.addAllArrayList((ArrayList<T>) source);
        }
        else
        {
            this.addAllCollection(source);
        }

        return true;
    }

    private void addAllFastList(FastList<T> source)
    {
        int sourceSize = source.size();
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        System.arraycopy(source.items, 0, this.items, this.size, sourceSize);
        this.size = newSize;
    }

    private void addAllArrayList(ArrayList<T> source)
    {
        int sourceSize = source.size();
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        ArrayListIterate.toArray(source, this.items, this.size, sourceSize);
        this.size = newSize;
    }

    private void addAllCollection(Collection<? extends T> source)
    {
        this.ensureCapacity(this.size + source.size());
        Iterate.forEachWith(source, Procedures2.<T>addToCollection(), this);
    }

    @Override
    public boolean containsAll(Collection<?> source)
    {
        return Iterate.allSatisfyWith(source, Predicates2.in(), this);
    }

    @Override
    public boolean containsAllArguments(Object... source)
    {
        return ArrayIterate.allSatisfyWith(source, Predicates2.in(), this);
    }

    @Override
    public <E> E[] toArray(E[] array)
    {
        if (array.length < this.size)
        {
            array = (E[]) Array.newInstance(array.getClass().getComponentType(), this.size);
        }
        System.arraycopy(this.items, 0, array, 0, this.size);
        if (array.length > this.size)
        {
            array[this.size] = null;
        }
        return array;
    }

    @Override
    public Object[] toArray()
    {
        return this.copyItemsWithNewCapacity(this.size);
    }

    public T[] toTypedArray(Class<T> clazz)
    {
        T[] array = (T[]) Array.newInstance(clazz, this.size);
        System.arraycopy(this.items, 0, array, 0, this.size);
        return array;
    }

    private void throwOutOfBounds(int index)
    {
        throw this.newIndexOutOfBoundsException(index);
    }

    public T set(int index, T element)
    {
        T previous = this.get(index);
        this.items[index] = element;
        return previous;
    }

    @Override
    public int indexOf(Object object)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (Comparators.nullSafeEquals(this.items[i], object))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object object)
    {
        for (int i = this.size - 1; i >= 0; i--)
        {
            if (Comparators.nullSafeEquals(this.items[i], object))
            {
                return i;
            }
        }
        return -1;
    }

    public void trimToSize()
    {
        if (this.size < this.items.length)
        {
            this.transferItemsToNewArrayWithCapacity(this.size);
        }
    }

    /**
     * Express load factor as 0.25 to trim a collection with more than 25% excess capacity
     */
    public boolean trimToSizeIfGreaterThanPercent(double loadFactor)
    {
        double excessCapacity = 1.0 - (double) this.size / (double) this.items.length;
        if (excessCapacity > loadFactor)
        {
            this.trimToSize();
            return true;
        }
        return false;
    }

    public void ensureCapacity(int minCapacity)
    {
        int oldCapacity = this.items.length;
        if (minCapacity > oldCapacity)
        {
            int newCapacity = Math.max(this.sizePlusFiftyPercent(oldCapacity), minCapacity);
            this.transferItemsToNewArrayWithCapacity(newCapacity);
        }
    }

    private void transferItemsToNewArrayWithCapacity(int newCapacity)
    {
        this.items = (T[]) this.copyItemsWithNewCapacity(newCapacity);
    }

    private Object[] copyItemsWithNewCapacity(int newCapacity)
    {
        Object[] newItems = new Object[newCapacity];
        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
        return newItems;
    }

    public FastList<T> with(T element1, T element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public FastList<T> with(T element1, T element2, T element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    public FastList<T> with(T... elements)
    {
        return this.withArrayCopy(elements, 0, elements.length);
    }

    public FastList<T> withArrayCopy(T[] elements, int begin, int length)
    {
        this.ensureCapacity(this.size + length);
        System.arraycopy(elements, begin, this.items, this.size, length);
        this.size += length;
        return this;
    }

    @Override
    public T getFirst()
    {
        return this.isEmpty() ? null : this.items[0];
    }

    @Override
    public T getLast()
    {
        return this.isEmpty() ? null : this.items[this.size() - 1];
    }

    @Override
    public void forEach(Procedure<? super T> procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i]);
        }
    }

    public void forEachIf(Predicate<? super T> predicate, Procedure<? super T> procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (predicate.accept(item))
            {
                procedure.value(item);
            }
        }
    }

    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super T> objectIntProcedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            objectIntProcedure.value(this.items[i], i);
        }
    }

    @Override
    public <P> void forEachWith(Procedure2<? super T, ? super P> procedure, P parameter)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i], parameter);
        }
    }

    @Override
    public FastList<T> select(Predicate<? super T> predicate)
    {
        return this.select(predicate, FastList.<T>newList());
    }

    @Override
    public <R extends Collection<T>> R select(Predicate<? super T> predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public <P> FastList<T> selectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return this.selectWith(predicate, parameter, FastList.<T>newList());
    }

    @Override
    public <P, R extends Collection<T>> R selectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter,
            R targetCollection)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (predicate.accept(item, parameter))
            {
                targetCollection.add(item);
            }
        }
        return targetCollection;
    }

    @Override
    public FastList<T> reject(Predicate<? super T> predicate)
    {
        return this.reject(predicate, FastList.<T>newList());
    }

    @Override
    public <R extends Collection<T>> R reject(Predicate<? super T> predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public <P> FastList<T> rejectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return this.rejectWith(predicate, parameter, FastList.<T>newList());
    }

    @Override
    public <P, R extends Collection<T>> R rejectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter,
            R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (!predicate.accept(item, parameter))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public <P> Twin<MutableList<T>> selectAndRejectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter)
    {
        MutableList<T> positiveResult = FastList.newList();
        MutableList<T> negativeResult = FastList.newList();
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            (predicate.accept(item, parameter) ? positiveResult : negativeResult).add(item);
        }
        return Tuples.twin(positiveResult, negativeResult);
    }

    @Override
    public <S> FastList<S> selectInstancesOf(Class<S> clazz)
    {
        FastList<S> result = FastList.newList(this.size);
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (clazz.isInstance(item))
            {
                result.add((S) item);
            }
        }
        result.trimToSize();
        return result;
    }

    @Override
    public void removeIf(Predicate<? super T> predicate)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (!predicate.accept(item))
            {
                // keep it
                if (currentFilledIndex != i)
                {
                    this.items[currentFilledIndex] = item;
                }
                currentFilledIndex++;
            }
        }
        this.wipeAndResetTheEnd(currentFilledIndex);
    }

    private void wipeAndResetTheEnd(int newCurrentFilledIndex)
    {
        for (int i = newCurrentFilledIndex; i < this.size; i++)
        {
            this.items[i] = null;
        }
        this.size = newCurrentFilledIndex;
    }

    @Override
    public <P> void removeIfWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (!predicate.accept(item, parameter))
            {
                // keep it
                if (currentFilledIndex != i)
                {
                    this.items[currentFilledIndex] = item;
                }
                currentFilledIndex++;
            }
        }
        this.wipeAndResetTheEnd(currentFilledIndex);
    }

    @Override
    public <V> FastList<V> collect(Function<? super T, ? extends V> function)
    {
        return this.collect(function, FastList.<V>newList(this.size()));
    }

    @Override
    public MutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction)
    {
        return this.collectBoolean(booleanFunction, new BooleanArrayList(this.size));
    }

    @Override
    public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super T> booleanFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(booleanFunction.booleanValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableByteList collectByte(ByteFunction<? super T> byteFunction)
    {
        return this.collectByte(byteFunction, new ByteArrayList(this.size));
    }

    @Override
    public <R extends MutableByteCollection> R collectByte(ByteFunction<? super T> byteFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(byteFunction.byteValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableCharList collectChar(CharFunction<? super T> charFunction)
    {
        return this.collectChar(charFunction, new CharArrayList(this.size));
    }

    @Override
    public <R extends MutableCharCollection> R collectChar(CharFunction<? super T> charFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(charFunction.charValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction)
    {
        return this.collectDouble(doubleFunction, new DoubleArrayList(this.size));
    }

    @Override
    public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super T> doubleFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(doubleFunction.doubleValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableFloatList collectFloat(FloatFunction<? super T> floatFunction)
    {
        return this.collectFloat(floatFunction, new FloatArrayList(this.size));
    }

    @Override
    public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super T> floatFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(floatFunction.floatValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableIntList collectInt(IntFunction<? super T> intFunction)
    {
        return this.collectInt(intFunction, new IntArrayList(this.size));
    }

    @Override
    public <R extends MutableIntCollection> R collectInt(IntFunction<? super T> intFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(intFunction.intValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableLongList collectLong(LongFunction<? super T> longFunction)
    {
        return this.collectLong(longFunction, new LongArrayList(this.size));
    }

    @Override
    public <R extends MutableLongCollection> R collectLong(LongFunction<? super T> longFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(longFunction.longValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public MutableShortList collectShort(ShortFunction<? super T> shortFunction)
    {
        return this.collectShort(shortFunction, new ShortArrayList(this.size));
    }

    @Override
    public <R extends MutableShortCollection> R collectShort(ShortFunction<? super T> shortFunction, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(shortFunction.shortValueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public <V, R extends Collection<V>> R collect(Function<? super T, ? extends V> function, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(function.valueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public <V> FastList<V> flatCollect(Function<? super T, ? extends Iterable<V>> function)
    {
        return this.flatCollect(function, FastList.<V>newList(this.size()));
    }

    @Override
    public <V, R extends Collection<V>> R flatCollect(
            Function<? super T, ? extends Iterable<V>> function,
            R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            Iterate.addAllTo(function.valueOf(this.items[i]), target);
        }
        return target;
    }

    @Override
    public <P, V> FastList<V> collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter)
    {
        return this.collectWith(function, parameter, FastList.<V>newList(this.size()));
    }

    @Override
    public <P, V, R extends Collection<V>> R collectWith(
            Function2<? super T, ? super P, ? extends V> function,
            P parameter,
            R targetCollection)
    {
        for (int i = 0; i < this.size; i++)
        {
            targetCollection.add(function.value(this.items[i], parameter));
        }
        return targetCollection;
    }

    @Override
    public <V> FastList<V> collectIf(
            Predicate<? super T> predicate,
            Function<? super T, ? extends V> function)
    {
        return this.collectIf(predicate, function, FastList.<V>newList());
    }

    @Override
    public <V, R extends Collection<V>> R collectIf(
            Predicate<? super T> predicate,
            Function<? super T, ? extends V> function,
            R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(function.valueOf(item));
            }
        }
        return target;
    }

    @Override
    public T detect(Predicate<? super T> predicate)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return null;
    }

    @Override
    public T detectIfNone(Predicate<? super T> predicate, Function0<? extends T> defaultValueBlock)
    {
        T result = this.detect(predicate);
        return result == null ? defaultValueBlock.value() : result;
    }

    @Override
    public <V extends Comparable<? super V>> T minBy(Function<? super T, ? extends V> function)
    {
        return ArrayIterate.minBy(this.items, this.size, function);
    }

    @Override
    public <V extends Comparable<? super V>> T maxBy(Function<? super T, ? extends V> function)
    {
        return ArrayIterate.maxBy(this.items, this.size, function);
    }

    @Override
    public <P> T detectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            if (predicate.accept(item, parameter))
            {
                return item;
            }
        }
        return null;
    }

    @Override
    public <P> T detectWithIfNone(
            Predicate2<? super T, ? super P> predicate,
            P parameter,
            Function0<? extends T> defaultValueBlock)
    {
        T result = this.detectWith(predicate, parameter);
        return result == null ? defaultValueBlock.value() : result;
    }

    public T get(int index)
    {
        if (index < this.size)
        {
            return this.items[index];
        }
        throw this.newIndexOutOfBoundsException(index);
    }

    private IndexOutOfBoundsException newIndexOutOfBoundsException(int index)
    {
        return new IndexOutOfBoundsException("Index: " + index + " Size: " + this.size);
    }

    @Override
    public boolean add(T newItem)
    {
        if (this.items.length == this.size)
        {
            this.ensureCapacityForAdd();
        }
        this.items[this.size++] = newItem;
        return true;
    }

    private void ensureCapacityForAdd()
    {
        if (this.items == DEFAULT_SIZED_EMPTY_ARRAY)
        {
            this.items = (T[]) new Object[10];
        }
        else
        {
            this.transferItemsToNewArrayWithCapacity(this.sizePlusFiftyPercent(this.size));
        }
    }

    public void add(int index, T element)
    {
        if (index > -1 && index < this.size)
        {
            this.addAtIndex(index, element);
        }
        else if (index == this.size)
        {
            this.add(element);
        }
        else
        {
            this.throwOutOfBounds(index);
        }
    }

    private void addAtIndex(int index, T element)
    {
        int oldSize = this.size++;
        if (this.items.length == oldSize)
        {
            T[] newItems = (T[]) new Object[this.sizePlusFiftyPercent(oldSize)];
            if (index > 0)
            {
                System.arraycopy(this.items, 0, newItems, 0, index);
            }
            System.arraycopy(this.items, index, newItems, index + 1, oldSize - index);
            this.items = newItems;
        }
        else
        {
            System.arraycopy(this.items, index, this.items, index + 1, oldSize - index);
        }
        this.items[index] = element;
    }

    private int sizePlusFiftyPercent(int oldSize)
    {
        int result = oldSize + (oldSize >> 1) + 1;
        return result < oldSize ? MAXIMUM_ARRAY_SIZE : result;
    }

    public T remove(int index)
    {
        T previous = this.get(index);
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.items, index + 1, this.items, index, totalOffset);
        }
        this.items[--this.size] = null;
        return previous;
    }

    @Override
    public boolean remove(Object object)
    {
        int index = this.indexOf(object);
        if (index >= 0)
        {
            this.remove(index);
            return true;
        }
        return false;
    }

    public boolean addAll(int index, Collection<? extends T> source)
    {
        if (index > this.size || index < 0)
        {
            this.throwOutOfBounds(index);
        }
        if (source.isEmpty())
        {
            return false;
        }

        if (source.getClass() == FastList.class)
        {
            this.addAllFastListAtIndex((FastList<T>) source, index);
        }
        else if (source.getClass() == ArrayList.class)
        {
            this.addAllArrayListAtIndex((ArrayList<T>) source, index);
        }
        else
        {
            this.addAllCollectionAtIndex(source, index);
        }
        return true;
    }

    private void addAllFastListAtIndex(FastList<T> source, int index)
    {
        int sourceSize = source.size();
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        this.shiftElementsAtIndex(index, sourceSize);
        System.arraycopy(source.items, 0, this.items, index, sourceSize);
        this.size = newSize;
    }

    private void addAllArrayListAtIndex(ArrayList<T> source, int index)
    {
        int sourceSize = source.size();
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        this.shiftElementsAtIndex(index, sourceSize);
        ArrayListIterate.toArray(source, this.items, index, sourceSize);
        this.size = newSize;
    }

    private void addAllCollectionAtIndex(Collection<? extends T> source, int index)
    {
        Object[] newItems = source.toArray();
        int sourceSize = newItems.length;
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        this.shiftElementsAtIndex(index, sourceSize);
        this.size = newSize;
        System.arraycopy(newItems, 0, this.items, index, sourceSize);
    }

    private void shiftElementsAtIndex(int index, int sourceSize)
    {
        int numberToMove = this.size - index;
        if (numberToMove > 0)
        {
            System.arraycopy(this.items, index, this.items, index + sourceSize, numberToMove);
        }
    }

    public int size()
    {
        return this.size;
    }

    @Override
    public int count(Predicate<? super T> predicate)
    {
        int count = 0;
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i]))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public <P> int countWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        int count = 0;
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i], parameter))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(Predicate<? super T> predicate)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i]))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i], parameter))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(Predicate<? super T> predicate)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (!predicate.accept(this.items[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (!predicate.accept(this.items[i], parameter))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean noneSatisfy(Predicate<? super T> predicate)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i], parameter))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super T, ? extends IV> function)
    {
        IV result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.value(result, this.items[i]);
        }
        return result;
    }

    @Override
    public int injectInto(int injectedValue, IntObjectToIntFunction<? super T> function)
    {
        int result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.intValueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public long injectInto(long injectedValue, LongObjectToLongFunction<? super T> function)
    {
        long result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.longValueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super T> function)
    {
        double result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.doubleValueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super T> function)
    {
        float result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.floatValueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public FastList<T> distinct()
    {
        MutableSet<T> seenSoFar = UnifiedSet.newSet();
        FastList<T> targetCollection = FastList.newList();
        for (int i = 0; i < this.size(); i++)
        {
            if (seenSoFar.add(this.items[i]))
            {
                targetCollection.add(this.items[i]);
            }
        }
        return targetCollection;
    }

    @Override
    public long sumOfInt(IntFunction<? super T> function)
    {
        long result = 0L;
        for (int i = 0; i < this.size; i++)
        {
            result += (long) function.intValueOf(this.items[i]);
        }
        return result;
    }

    @Override
    public long sumOfLong(LongFunction<? super T> function)
    {
        long result = 0L;
        for (int i = 0; i < this.size; i++)
        {
            result += function.longValueOf(this.items[i]);
        }
        return result;
    }

    @Override
    public double sumOfFloat(FloatFunction<? super T> function)
    {
        double result = 0.0d;
        for (int i = 0; i < this.size; i++)
        {
            result += (double) function.floatValueOf(this.items[i]);
        }
        return result;
    }

    @Override
    public double sumOfDouble(DoubleFunction<? super T> function)
    {
        double result = 0.0d;
        for (int i = 0; i < this.size; i++)
        {
            result += function.doubleValueOf(this.items[i]);
        }
        return result;
    }

    @Override
    public <IV, P> IV injectIntoWith(
            IV injectValue,
            Function3<? super IV, ? super T, ? super P, ? extends IV> function,
            P parameter)
    {
        IV result = injectValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.value(result, this.items[i], parameter);
        }
        return result;
    }

    @Override
    public FastList<T> toList()
    {
        return FastList.newList(this);
    }

    @Override
    public FastList<T> toSortedList()
    {
        return this.toSortedList(Comparators.naturalOrder());
    }

    @Override
    public FastList<T> toSortedList(Comparator<? super T> comparator)
    {
        return FastList.newList(this).sortThis(comparator);
    }

    @Override
    public MutableList<T> takeWhile(Predicate<? super T> predicate)
    {
        int endIndex = this.detectNotIndex(predicate);
        T[] result = (T[]) new Object[endIndex];
        System.arraycopy(this.items, 0, result, 0, endIndex);
        return FastList.newListWith(result);
    }

    @Override
    public MutableList<T> dropWhile(Predicate<? super T> predicate)
    {
        int startIndex = this.detectNotIndex(predicate);
        int resultSize = this.size() - startIndex;
        T[] result = (T[]) new Object[resultSize];
        System.arraycopy(this.items, startIndex, result, 0, resultSize);
        return FastList.newListWith(result);
    }

    @Override
    public PartitionMutableList<T> partitionWhile(Predicate<? super T> predicate)
    {
        PartitionMutableList<T> result = new PartitionFastList<T>();
        FastList<T> selected = (FastList<T>) result.getSelected();
        FastList<T> rejected = (FastList<T>) result.getRejected();
        int partitionIndex = this.detectNotIndex(predicate);
        int rejectedSize = this.size() - partitionIndex;
        selected.withArrayCopy(this.items, 0, partitionIndex);
        rejected.withArrayCopy(this.items, partitionIndex, rejectedSize);
        return result;
    }

    private int detectNotIndex(Predicate<? super T> predicate)
    {
        for (int index = 0; index < this.size; index++)
        {
            if (!predicate.accept(this.items[index]))
            {
                return index;
            }
        }
        return this.size;
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof List))
        {
            return false;
        }
        List<?> list = (List<?>) otherList;
        if (otherList instanceof FastList)
        {
            return this.fastListEquals((FastList<?>) otherList);
        }
        if (otherList instanceof RandomAccess)
        {
            return this.randomAccessListEquals(list);
        }
        return this.regularListEquals(list);
    }

    public boolean fastListEquals(FastList<?> otherFastList)
    {
        if (this.size() != otherFastList.size())
        {
            return false;
        }
        for (int i = 0; i < this.size; i++)
        {
            T one = this.items[i];
            Object two = otherFastList.items[i];
            if (!Comparators.nullSafeEquals(one, two))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @deprecated in 1.3
     */
    @Deprecated
    public boolean equals(FastList<?> otherList)
    {
        return this.fastListEquals(otherList);
    }

    private boolean regularListEquals(List<?> otherList)
    {
        Iterator<?> iterator = otherList.iterator();
        for (int i = 0; i < this.size; i++)
        {
            T one = this.items[i];
            if (!iterator.hasNext())
            {
                return false;
            }
            Object two = iterator.next();
            if (!Comparators.nullSafeEquals(one, two))
            {
                return false;
            }
        }
        return !iterator.hasNext();
    }

    private boolean randomAccessListEquals(List<?> otherList)
    {
        if (this.size() != otherList.size())
        {
            return false;
        }
        for (int i = 0; i < this.size; i++)
        {
            T one = this.items[i];
            Object two = otherList.get(i);
            if (!Comparators.nullSafeEquals(one, two))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 1;
        for (int i = 0; i < this.size; i++)
        {
            T item = this.items[i];
            hashCode = 31 * hashCode + (item == null ? 0 : item.hashCode());
        }
        return hashCode;
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        for (int i = 0; i < this.size; i++)
        {
            out.writeObject(this.items[i]);
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.size = in.readInt();
        this.items = (T[]) new Object[this.size];
        for (int i = 0; i < this.size; i++)
        {
            this.items[i] = (T) in.readObject();
        }
    }

    @Beta
    public ParallelListIterable<T> asParallel(ExecutorService executorService, int batchSize)
    {
        if (executorService == null)
        {
            throw new NullPointerException();
        }
        if (batchSize < 1)
        {
            throw new IllegalArgumentException();
        }
        return new FastListParallelIterable(executorService, batchSize);
    }

    private final class FastListBatch extends AbstractBatch<T> implements RootListBatch<T>
    {
        private final int chunkStartIndex;
        private final int chunkEndIndex;

        private FastListBatch(int chunkStartIndex, int chunkEndIndex)
        {
            this.chunkStartIndex = chunkStartIndex;
            this.chunkEndIndex = chunkEndIndex;
        }

        public void forEach(Procedure<? super T> procedure)
        {
            for (int i = this.chunkStartIndex; i < this.chunkEndIndex; i++)
            {
                procedure.value(FastList.this.items[i]);
            }
        }

        public boolean anySatisfy(Predicate<? super T> predicate)
        {
            for (int i = this.chunkStartIndex; i < this.chunkEndIndex; i++)
            {
                if (predicate.accept(FastList.this.items[i]))
                {
                    return true;
                }
            }
            return false;
        }

        public boolean allSatisfy(Predicate<? super T> predicate)
        {
            for (int i = this.chunkStartIndex; i < this.chunkEndIndex; i++)
            {
                if (!predicate.accept(FastList.this.items[i]))
                {
                    return false;
                }
            }
            return true;
        }

        public T detect(Predicate<? super T> predicate)
        {
            for (int i = this.chunkStartIndex; i < this.chunkEndIndex; i++)
            {
                if (predicate.accept(FastList.this.items[i]))
                {
                    return FastList.this.items[i];
                }
            }
            return null;
        }

        public ListBatch<T> select(Predicate<? super T> predicate)
        {
            return new SelectListBatch<T>(this, predicate);
        }

        public <V> ListBatch<V> collect(Function<? super T, ? extends V> function)
        {
            return new CollectListBatch<T, V>(this, function);
        }

        public UnsortedSetBatch<T> distinct(ConcurrentHashMap<T, Boolean> distinct)
        {
            return new DistinctBatch<T>(this, distinct);
        }
    }

    private final class FastListParallelIterable extends AbstractParallelListIterable<T, RootListBatch<T>>
    {
        private final ExecutorService executorService;
        private final int batchSize;

        private FastListParallelIterable(ExecutorService executorService, int batchSize)
        {
            this.executorService = executorService;
            this.batchSize = batchSize;
        }

        @Override
        public ExecutorService getExecutorService()
        {
            return this.executorService;
        }

        @Override
        public LazyIterable<RootListBatch<T>> split()
        {
            return new FastListParallelBatchLazyIterable();
        }

        public void forEach(Procedure<? super T> procedure)
        {
            forEach(this, procedure);
        }

        public boolean anySatisfy(Predicate<? super T> predicate)
        {
            return anySatisfy(this, predicate);
        }

        public boolean allSatisfy(Predicate<? super T> predicate)
        {
            return allSatisfy(this, predicate);
        }

        public T detect(Predicate<? super T> predicate)
        {
            return detect(this, predicate);
        }

        private class FastListParallelBatchIterator implements Iterator<RootListBatch<T>>
        {
            protected int chunkIndex;

            public boolean hasNext()
            {
                return this.chunkIndex * FastListParallelIterable.this.batchSize < FastList.this.size;
            }

            public RootListBatch<T> next()
            {
                int chunkStartIndex = this.chunkIndex * FastListParallelIterable.this.batchSize;
                int chunkEndIndex = (this.chunkIndex + 1) * FastListParallelIterable.this.batchSize;
                int truncatedChunkEndIndex = Math.min(chunkEndIndex, FastList.this.size);
                this.chunkIndex++;
                return new FastListBatch(chunkStartIndex, truncatedChunkEndIndex);
            }

            public void remove()
            {
                throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
            }
        }

        private class FastListParallelBatchLazyIterable
                extends AbstractLazyIterable<RootListBatch<T>>
        {
            public void forEach(Procedure<? super RootListBatch<T>> procedure)
            {
                for (RootListBatch<T> chunk : this)
                {
                    procedure.value(chunk);
                }
            }

            public <P> void forEachWith(Procedure2<? super RootListBatch<T>, ? super P> procedure, P parameter)
            {
                for (RootListBatch<T> chunk : this)
                {
                    procedure.value(chunk, parameter);
                }
            }

            public void forEachWithIndex(ObjectIntProcedure<? super RootListBatch<T>> objectIntProcedure)
            {
                throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
            }

            public Iterator<RootListBatch<T>> iterator()
            {
                return new FastListParallelBatchIterator();
            }
        }
    }
}
