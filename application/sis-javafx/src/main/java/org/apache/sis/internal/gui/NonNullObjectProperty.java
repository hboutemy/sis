/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.gui;

import java.util.Objects;
import javafx.beans.property.SimpleObjectProperty;


/**
 * A simple property implementation which does not accept null values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <T> the type of the wrapped object.
 *
 * @since 1.1
 */
public final class NonNullObjectProperty<T> extends SimpleObjectProperty<T> {
    /**
     * Creates a new property.
     *
     * @param bean          object for which this property is a member.
     * @param name          name of the property in the bean.
     * @param initialValue  initial value of the property.
     */
    public NonNullObjectProperty(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Sets the property value.
     *
     * @param  newValue  the new property value.
     */
    @Override
    public void set​(final T newValue) {
        super.set(Objects.requireNonNull(newValue));
    }
}
