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
package org.apache.sis.gui.coverage;

import javafx.scene.control.Accordion;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ChoiceBox;
import javafx.scene.paint.Color;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link CoverageCanvas} with associated controls to show in a {@link CoverageExplorer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageControls extends Controls {
    /**
     * The component for showing sample values.
     */
    private final CoverageCanvas view;

    /**
     * The controls for changing {@link #view}.
     */
    private final Accordion controls;

    /**
     * The image together with the status bar.
     */
    private final BorderPane imageAndStatus;

    /**
     * The coordinate reference system selected in the {@link ChoiceBox}.
     */
    private final ObjectProperty<ReferenceSystem> referenceSystem;

    /**
     * Creates a new set of coverage controls.
     *
     * @param  vocabulary  localized set of words, provided in argument because often known by the caller.
     * @param  coverage    property containing the coverage to show.
     */
    CoverageControls(final Vocabulary vocabulary, final ObjectProperty<GridCoverage> coverage,
                     final RecentReferenceSystems referenceSystems)
    {
        final Color background = Color.BLACK;
        view = new CoverageCanvas();
        view.setBackground(background);
        final StatusBar statusBar = new StatusBar(referenceSystems);
        statusBar.canvas.set(view);
        imageAndStatus = new BorderPane(view.getView());
        imageAndStatus.setBottom(statusBar.getView());
        /*
         * "Display" section with the following controls:
         *    - Coordinate reference system
         *    - Color stretching
         *    - Background color
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final ChoiceBox<ReferenceSystem> systems = referenceSystems.createChoiceBox(this::onReferenceSystemSelected);
            systems.setMaxWidth(Double.POSITIVE_INFINITY);
            referenceSystem = systems.valueProperty();
            final Label systemLabel = new Label(vocabulary.getLabel(Vocabulary.Keys.ReferenceSystem));
            systemLabel.setPadding(CAPTION_MARGIN);
            systemLabel.setLabelFor(systems);
            final GridPane gp = createControlGrid(
                label(vocabulary, Vocabulary.Keys.Stretching, Stretching.createButton((p,o,n) -> view.setStretching(n))),
                label(vocabulary, Vocabulary.Keys.Background, createBackgroundButton(background))
            );
            final Label label = new Label(vocabulary.getLabel(Vocabulary.Keys.Colors));
            label.setPadding(NEXT_CAPTION_MARGIN);
            label.setLabelFor(gp);
            displayPane = new VBox(systemLabel, systems, label, gp);
        }
        /*
         * Put all sections together and have the first one expanded by default.
         */
        controls = new Accordion(
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Display), displayPane)
            // TODO: more controls to be added in a future version.
        );
        controls.setExpandedPane(controls.getPanes().get(0));
        view.coverageProperty.bind(coverage);
    }

    /**
     * Creates the button for selecting a background color.
     */
    private ColorPicker createBackgroundButton(final Color background) {
        final ColorPicker b = new ColorPicker(background);
        b.setOnAction((e) -> {
            view.setBackground(((ColorPicker) e.getSource()).getValue());
        });
        return b;
    }

    /**
     * Invoked in JavaFX thread after {@link CoverageExplorer#setCoverage(ImageRequest)} completed.
     * This method updates the GUI with new information available, in particular
     * the coordinate reference system and the list of sample dimensions.
     *
     * @param  data  the new coverage, or {@code null} if none.
     */
    @Override
    final void coverageChanged(final GridCoverage data) {
        if (data != null) {
            referenceSystem.set(data.getCoordinateReferenceSystem());
        }
    }

    /**
     * Invoked when a new coordinate reference system is selected.
     */
    private void onReferenceSystemSelected(final ObservableValue<? extends ReferenceSystem> property,
                                           final ReferenceSystem oldValue, ReferenceSystem newValue)
    {
    }

    /**
     * Returns the main component, which is showing coverage tabular data.
     */
    @Override
    final Region view() {
        return imageAndStatus;
    }

    /**
     * Returns the controls for controlling the view of tabular data.
     */
    @Override
    final Control controls() {
        return controls;
    }
}
