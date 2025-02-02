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
package org.apache.sis.internal.filter.sqlmm;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;

// Branch-dependent imports
import org.apache.sis.filter.Expression;


/**
 * Constructor for a geometry which is transformed from a Well-Known Text (WKT) representation.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 * @param  <G>  the implementation type of geometry objects.
 *
 * @since 1.1
 */
final class ST_FromText<R,G> extends GeometryParser<R,G> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1627156125717985980L;

    /**
     * Creates a new function for the given parameters.
     */
    ST_FromText(final SQLMM operation, final Expression<R,?>[] parameters, final Geometries<G> library) {
        super(operation, parameters, library);
    }

    /**
     * Creates a new expression of the same type than this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<R,?>[] effective) {
        return new ST_FromText<>(operation, effective, getGeometryLibrary());
    }

    /**
     * Returns the name of the kind of input expected by this expression.
     */
    @Override
    final String inputName() {
        return "text";
    }

    /**
     * Parses the given value.
     *
     * @param  value  the WKT value.
     * @return the geometry parsed from the given value.
     * @throws ClassCastException if the given value is not a {@link String} or an array of bytes.
     * @throws Exception if parsing failed for another reason. This is an implementation-specific exception.
     */
    @Override
    protected GeometryWrapper parse(final Object value) throws Exception {
        // ClassCastException is part of method contract.
        return library.parseWKT((String) value);
    }
}
