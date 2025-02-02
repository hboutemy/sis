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

/**
 * Helper classes for creating {@code FeatureType} instances. Usage of this package is not mandatory,
 * but make easier to create {@link org.apache.sis.feature.DefaultFeatureType} instances together with
 * their attributes and associations.
 *
 * <p>The starting point is {@link org.apache.sis.feature.builder.FeatureTypeBuilder}.
 * The following example creates a feature type for a capital, as a special kind of city,
 * named "Utopia" by default:</p>
 *
 * {@snippet lang="java" :
 *     FeatureTypeBuilder builder;
 *
 *     // Create a feature type for a city, which contains a name and a population.
 *     builder = new FeatureTypeBuilder() .setName("City");
 *     builder.addAttribute(String.class) .setName("name").setDefaultValue("Utopia");
 *     builder.addAttribute(Integer.class).setName("population");
 *     FeatureType city = builder.build();
 *
 *     // Create a subclass for a city which is also a capital.
 *     builder = new FeatureTypeBuilder().setName("Capital").setSuperTypes(city);
 *     builder.addAttribute(String.class).setName("parliament");
 *     FeatureType capital = builder.build();
 *     }
 *
 * A call to {@code System.out.println(capital)} prints the following table:
 *
 * <pre class="text">
 *   Capital ⇾ City
 *   ┌────────────┬─────────┬──────────────┬───────────────┐
 *   │ Name       │ Type    │ Multiplicity │ Default value │
 *   ├────────────┼─────────┼──────────────┼───────────────┤
 *   │ name       │ String  │   [1 … 1]    │ Utopia        │
 *   │ population │ Integer │   [1 … 1]    │               │
 *   │ parliament │ String  │   [1 … 1]    │               │
 *   └────────────┴─────────┴──────────────┴───────────────┘</pre>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 *
 * @see org.apache.sis.feature.DefaultFeatureType
 *
 * @since 0.8
 */
package org.apache.sis.feature.builder;
