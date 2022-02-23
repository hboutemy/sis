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
package org.apache.sis.internal.style;

import java.awt.Color;
import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.Fill}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Fill implements org.opengis.style.Fill {

    private GraphicFill graphicFill;
    private Expression<Feature,Color> color;
    private Expression<Feature, ? extends Number> opacity;

    public Fill() {
        this(null, StyleFactory.DEFAULT_FILL_COLOR, StyleFactory.DEFAULT_FILL_OPACITY);
    }

    public Fill(GraphicFill graphicFill, Expression<Feature, Color> color, Expression<Feature, ? extends Number> opacity) {
        ArgumentChecks.ensureNonNull("color", color);
        ArgumentChecks.ensureNonNull("opacity", opacity);
        this.graphicFill = graphicFill;
        this.color = color;
        this.opacity = opacity;
    }

    @Override
    public GraphicFill getGraphicFill() {
        return graphicFill;
    }

    public void setGraphicFill(GraphicFill graphicFill) {
        this.graphicFill = graphicFill;
    }

    @Override
    public Expression<Feature, Color> getColor() {
        return color;
    }

    public void setColor(Expression<Feature, Color> color) {
        ArgumentChecks.ensureNonNull("color", color);
        this.color = color;
    }

    @Override
    public Expression<Feature,? extends Number> getOpacity() {
        return opacity;
    }

    public void setOpacity(Expression<Feature, ? extends Number> opacity) {
        ArgumentChecks.ensureNonNull("opacity", opacity);
        this.opacity = opacity;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphicFill, color, opacity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Fill other = (Fill) obj;
        return Objects.equals(this.graphicFill, other.graphicFill)
            && Objects.equals(this.color, other.color)
            && Objects.equals(this.opacity, other.opacity);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Fill castOrCopy(org.opengis.style.Fill candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Fill) {
            return (Fill) candidate;
        }
        return new Fill(
                GraphicFill.castOrCopy(candidate.getGraphicFill()),
                candidate.getColor(),
                candidate.getOpacity());
    }
}
