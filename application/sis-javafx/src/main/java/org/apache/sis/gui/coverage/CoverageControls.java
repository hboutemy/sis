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

import java.util.Locale;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.gui.map.MapMenu;
import org.apache.sis.internal.gui.control.ValueColorMapper;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link CoverageCanvas} with associated controls to show in a {@link CoverageExplorer}.
 * This class installs bidirectional bindings between {@link CoverageCanvas} and the controls.
 * The controls are updated when the coverage shown in {@link CoverageCanvas} is changed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class CoverageControls extends ViewAndControls {
    /**
     * The component for showing sample values.
     *
     * @see CoverageExplorer#getCanvas()
     */
    final CoverageCanvas view;

    /**
     * The control showing categories and their colors for the current coverage.
     */
    private final TableView<Category> categoryTable;

    /**
     * The renderer of isolines.
     */
    private final IsolineRenderer isolines;

    /**
     * Creates a new set of coverage controls.
     *
     * @param  owner  the widget which creates this view. Can not be null.
     */
    CoverageControls(final CoverageExplorer owner) {
        super(owner);
        final Locale     locale     = owner.getLocale();
        final Resources  resources  = Resources.forLocale(locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);

        view = new CoverageCanvas(this, locale);
        view.setBackground(Color.BLACK);
        status.track(view);
        final MapMenu menu = new MapMenu(view);
        menu.addReferenceSystems(owner.referenceSystems);
        menu.addCopyOptions(status);
        /*
         * "Display" section with the following controls:
         *    - Current CRS
         *    - Interpolation
         *    - Color stretching
         *    - Colors for each category
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final Label crsControl = new Label();
            crsControl.setPadding(CONTENT_MARGIN);
            crsControl.setTooltip(new Tooltip(resources.getString(Resources.Keys.SelectCrsByContextMenu)));
            menu.selectedReferenceSystem().ifPresent((text) -> crsControl.textProperty().bind(text));
            /*
             * Creates a "Values" sub-section with the following controls:
             *   - Interpolation
             *   - Color stretching
             */
            final GridPane valuesControl = Styles.createControlGrid(0,
                label(vocabulary, Vocabulary.Keys.Interpolation, InterpolationConverter.button(view)),
                label(vocabulary, Vocabulary.Keys.Stretching, Stretching.createButton((p,o,n) -> view.setStyling(n))));
            /*
             * Creates a "Categories" section with the category table.
             */
            final CoverageStyling styling = new CoverageStyling(view);
            categoryTable = styling.createCategoryTable(resources, vocabulary);
            VBox.setVgrow(categoryTable, Priority.ALWAYS);
            /*
             * All sections put together.
             */
            displayPane = new VBox(
                    labelOfGroup(vocabulary, Vocabulary.Keys.ReferenceSystem, crsControl,    true),  crsControl,
                    labelOfGroup(vocabulary, Vocabulary.Keys.Values,          valuesControl, false), valuesControl,
                    labelOfGroup(vocabulary, Vocabulary.Keys.Categories,      categoryTable, false), categoryTable);
        }
        /*
         * "Isolines" section with the following controls:
         *    - Colors for each isoline levels
         */
        final VBox isolinesPane;
        {   // Block for making variables locale to this scope.
            final ValueColorMapper mapper = new ValueColorMapper(resources, vocabulary);
            isolines = new IsolineRenderer(view);
            isolines.setIsolineTables(java.util.Collections.singletonList(mapper.getSteps()));
            final Region view = mapper.getView();
            VBox.setVgrow(view, Priority.ALWAYS);
            isolinesPane = new VBox(view);                          // TODO: add band selector
        }
        /*
         * Put all sections together and have the first one expanded by default.
         * The "Properties" section will be built by `PropertyPaneCreator` only if requested.
         */
        final TitledPane deferred;                  // Control to be built only if requested.
        controlPanes = new TitledPane[] {
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Display),  displayPane),
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Isolines), isolinesPane),
            deferred = new TitledPane(vocabulary.getString(Vocabulary.Keys.Properties), null)
        };
        /*
         * Set listeners: changes on `CoverageCanvas` properties are propagated to the corresponding
         * `CoverageExplorer` properties. This constructor does not install listeners in the opposite
         * direction; instead `CoverageExplorer` will invoke `load(ImageRequest)`.
         */
        view.resourceProperty.addListener((p,o,n) -> notifyDataChanged(n, null));
        view.coverageProperty.addListener((p,o,n) -> notifyDataChanged(view.getResourceIfAdjusting(), n));
        deferred.expandedProperty().addListener(new PropertyPaneCreator(view, deferred));
        setView(view.getView());
    }

    /**
     * Invoked in JavaFX thread after {@link CoverageCanvas} resource or coverage property value changed.
     * This method updates the controls GUI with new information available and update the corresponding
     * {@link CoverageExplorer} properties.
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     */
    private void notifyDataChanged(final GridCoverageResource resource, final GridCoverage coverage) {
        final ObservableList<Category> items = categoryTable.getItems();
        if (coverage == null) {
            items.clear();
        } else {
            final int visibleBand = 0;          // TODO: provide a selector for the band to show.
            items.setAll(coverage.getSampleDimensions().get(visibleBand).getCategories());
        }
        owner.notifyDataChanged(resource, coverage);
    }

    /**
     * Returns the grid coverage shown in the view, or {@code null} if none.
     */
    @Override
    GridCoverage getCoverage() {
        return view.getCoverage();
    }

    /**
     * Sets the view content to the given coverage.
     * This method is invoked when a new source of data (either a resource or a coverage) is specified,
     * or when a previously hidden view is made visible. This implementation starts a background thread.
     *
     * @param  request  the resource or coverage to set, or {@code null} for clearing the view.
     */
    @Override
    final void load(final ImageRequest request) {
        view.setImage(request);
    }
}
