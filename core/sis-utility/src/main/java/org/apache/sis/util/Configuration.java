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
package org.apache.sis.util;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotates methods having a system-wide impact on the configuration of the Apache SIS library.
 * See <cite>"Use"</cite> javadoc link for a list of annotated methods.
 *
 * <p><b>Do not use.</b> This annotation is for documentation purpose only
 * and will be replaced by the {@link org.apache.sis.setup.Configuration} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.3
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-86">SIS-86</a>
 *
 * @since 0.3
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Configuration {
}
