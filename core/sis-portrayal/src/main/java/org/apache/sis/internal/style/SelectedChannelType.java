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

import java.util.Objects;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.SelectedChannelType}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SelectedChannelType implements org.opengis.style.SelectedChannelType {

    private String channelName;
    private ContrastEnhancement contrastEnhancement;

    public SelectedChannelType() {
    }

    public SelectedChannelType(String channelName, ContrastEnhancement contrastEnhancement) {
        this.channelName = channelName;
        this.contrastEnhancement = contrastEnhancement;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public ContrastEnhancement getContrastEnhancement() {
        return contrastEnhancement;
    }

    public void setContrastEnhancement(ContrastEnhancement contrastEnhancement) {
        this.contrastEnhancement = contrastEnhancement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelName, contrastEnhancement);
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
        final SelectedChannelType other = (SelectedChannelType) obj;
        return Objects.equals(this.channelName, other.channelName)
            && Objects.equals(this.contrastEnhancement, other.contrastEnhancement);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static SelectedChannelType castOrCopy(org.opengis.style.SelectedChannelType candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof SelectedChannelType) {
            return (SelectedChannelType) candidate;
        }
        return new SelectedChannelType(
                candidate.getChannelName(),
                ContrastEnhancement.castOrCopy(candidate.getContrastEnhancement()));
    }
}
