/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2024 Partha Sutradhar.
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package org.eventa.core.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheConcurrentHashMap<K, V> {

    private final int maxSize;
    private final ConcurrentMap<K, V> cache;
    private final LinkedHashMap<K, V> lruMap;

    public CacheConcurrentHashMap(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>();
        this.lruMap = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > CacheConcurrentHashMap.this.maxSize;
            }
        };
    }

    public V put(K key, V value) {
        synchronized (lruMap) {
            V oldValue = cache.put(key, value);
            lruMap.put(key, value);
            if (lruMap.size() > maxSize) {
                Map.Entry<K, V> eldest = lruMap.entrySet().iterator().next();
                cache.remove(eldest.getKey());
                lruMap.remove(eldest.getKey());
            }
            return oldValue;
        }
    }

    public V get(K key) {
        synchronized (lruMap) {
            return cache.get(key);
        }
    }

    public V remove(K key) {
        synchronized (lruMap) {
            lruMap.remove(key);
            return cache.remove(key);
        }
    }

    public boolean containsKey(K key) {
        synchronized (lruMap) {
            return cache.containsKey(key);
        }
    }

    public int size() {
        synchronized (lruMap) {
            return cache.size();
        }
    }

    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        synchronized (lruMap) {
            if (!cache.containsKey(key)) {
                V value = mappingFunction.apply(key);
                put(key, value);
                return value;
            }
            return cache.get(key);
        }
    }
}
