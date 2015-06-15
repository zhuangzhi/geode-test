/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire.test.support;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * The CollectionUtils class is a utility class for working with the Java Collections Framework.
 *
 * @author John Blum
 * @see java.util.Collection
 * @see java.util.Collections
 * @see org.springframework.data.gemfire.util.CollectionUtils
 * @since 1.5.0
 */
@SuppressWarnings("unused")
// TODO replace with org.springframework.data.gemfire.util.CollectionUtils
public abstract class CollectionUtils extends org.springframework.data.gemfire.util.CollectionUtils {

	public static <T> Iterable<T> iterable(final Enumeration<T> enumeration) {
		return new Iterable<T>() {
			@Override public Iterator<T> iterator() {
				return org.springframework.util.CollectionUtils.toIterator(enumeration);
			}
		};
	}

	public static <T> Iterable<T> iterable(final Iterator<T> iterator) {
		return new Iterable<T>() {
			@Override public Iterator<T> iterator() {
				return iterator;
			}
		};
	}

	public static <T> List<T> subList(final List<T> source, final int... indices) {
		List<T> result = new ArrayList<T>(indices.length);
		for (int index : indices) {
			result.add(source.get(index));
		}
		return result;
	}

}
