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
package org.apache.sis.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.StringJoiner;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.collections.ObservableList;
import org.apache.sis.gui.dataset.ResourceEvent;
import org.apache.sis.gui.dataset.ResourceExplorer;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.RecentChoices;
import org.apache.sis.util.ArraysExt;


/**
 * Manages a list of recently opened files. The list of files is initialized from user preferences.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 */
final class RecentFiles implements EventHandler<ActionEvent> {
    /**
     * Maximum number of items to show.
     */
    private static final int MAX_COUNT = 10;

    /**
     * Menu items for each recently opened file.
     * This list should have no more than {@value #MAX_COUNT} elements.
     */
    private final ObservableList<MenuItem> items;

    /**
     * The explorer where to open the file.
     */
    private final ResourceExplorer explorer;

    /**
     * Creates a new handler for a list of recently used files in the specified menu.
     */
    private RecentFiles(final ResourceExplorer explorer, final Menu menu) {
        this.explorer = explorer;
        items = menu.getItems();
    }

    /**
     * Creates a menu for a list of recently used files.
     * The menu is initialized with a list of files fetched from user preferences.
     * The user preferences are updated when {@link #touched(ResourceEvent, boolean)} is invoked.
     */
    static Menu create(final ResourceExplorer explorer, final Resources localized) {
        final Menu           menu    = new Menu(localized.getString(Resources.Keys.OpenRecentFile));
        final RecentFiles    handler = new RecentFiles(explorer, menu);
        final CharSequence[] files   = RecentChoices.getFiles();
        final MenuItem[]     items   = new MenuItem[files.length];
        int n = 0;
        for (int i=0; i<files.length; i++) {
            final String file = files[i].toString();
            if (!file.isBlank()) {
                items[n++] = handler.createItem(new File(file));
            }
        }
        handler.items.setAll(ArraysExt.resize(items, n));
        explorer.setOnResourceLoaded((e) -> handler.touched(e, false));
        explorer.setOnResourceClosed((e) -> handler.touched(e, true));
        return menu;
    }

    /**
     * Creates a new menu item for the specified file.
     */
    private MenuItem createItem(final File file) {
        final MenuItem item = new MenuItem(file.getName());
        item.setUserData(file);
        item.setOnAction(this);
        item.setDisable(!file.exists());
        return item;
    }

    /**
     * Notifies that a file is opened or closed. If opened, the file item (if any) is temporarily removed.
     * If closed, a new menu items is created for the specified file and is inserted at the beginning of
     * the list of recent files. If the list of recent files has more than {@value #MAX_COUNT} elements,
     * the latest item is discarded.
     */
    private void touched(final ResourceEvent event, final boolean closed) {
        final Path path = event.getResourcePath();
        final File file;
        try {
            file = path.toFile();
        } catch (UnsupportedOperationException e) {
            // Future version may have an "recently used URI" section. We don't do that for now.
            return;
        }
        final int size = items.size();
        /*
         * Verifies if an item already exists for the given file.
         * If yes, we will remove it (if opening) or move it to the top (if closing).
         */
        MenuItem item = null;
        for (int i=0; i<size; i++) {
            if (file.equals(items.get(i).getUserData())) {
                item = items.remove(i);
                break;
            }
        }
        /*
         * The menu item has been removed. Reinsert it at the top of the list only if the file has been closed.
         * But save in the preferences in all cases, in case the application shutdown without closing the files.
         */
        final StringJoiner s = new StringJoiner(System.lineSeparator());
        int count = 0;
        if (closed) {
            if (item == null) {
                if (size >= MAX_COUNT) {
                    item = items.remove(size-1);
                    item.setText(file.getName());
                    item.setUserData(file);
                    item.setDisable(false);
                } else {
                    item = createItem(file);
                }
            }
            items.add(0, item);
        } else {
            s.add(file.getPath());          // Explicit saved because that item is no longer in the menu.
            count = 1;
        }
        /*
         * At this point the menu items has been updated.
         * Now save the file list in user preferences.
         */
        for (final MenuItem i : items) {
            s.add(((File) i.getUserData()).getPath());
            if (++count >= MAX_COUNT) break;
        }
        RecentChoices.setFiles(s.toString());
    }

    /**
     * Invoked when the user selects a file.
     */
    @Override
    public void handle(final ActionEvent event) {
        final Object file = ((MenuItem) event.getSource()).getUserData();
        explorer.loadResources(Collections.singleton(file));
    }
}
