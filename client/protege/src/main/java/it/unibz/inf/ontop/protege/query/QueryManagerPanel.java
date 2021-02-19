package it.unibz.inf.ontop.protege.query;

/*
 * #%L
 * ontop-protege4
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.protege.utils.DialogUtils;
import it.unibz.inf.ontop.protege.utils.IconLoader;
import it.unibz.inf.ontop.protege.utils.OntopAbstractAction;
import it.unibz.inf.ontop.protege.utils.SimpleDocumentListener;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import static it.unibz.inf.ontop.protege.utils.DialogUtils.CANCEL_BUTTON_TEXT;
import static it.unibz.inf.ontop.protege.utils.DialogUtils.OK_BUTTON_TEXT;

public class QueryManagerPanel extends JPanel {

	private static final long serialVersionUID = 6920100822784727963L;

    static private final String QUERY_ICON_PATH = "images/query_icon.png";
    static private final String GROUP_ICON_PATH = "images/group_icon.png";

    private final Icon queryIcon;
    private final Icon groupIcon;

    private final List<QueryManagerPanelSelectionListener> listeners = new ArrayList<>();

    private final QueryManager queryManager;

    private final JTree queryManagerTree;

	public QueryManagerPanel(QueryManager queryManager) {
        this.queryManager = queryManager;

        queryIcon = IconLoader.getImageIcon(QUERY_ICON_PATH);
        groupIcon = IconLoader.getImageIcon(GROUP_ICON_PATH);

        setLayout(new BorderLayout());

        QueryManagerTreeModel model = new QueryManagerTreeModel(queryManager);
        queryManagerTree = new JTree(model);
        queryManagerTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                QueryManager.Item item = (QueryManager.Item)value;
                setText(item.getID());
                setIcon(item.isQuery() ? queryIcon : groupIcon);

                return this;
            }
        });
        queryManagerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        queryManagerTree.getSelectionModel().addTreeSelectionListener(evt -> {
            QueryManager.Item entity = (QueryManager.Item) evt.getPath().getLastPathComponent();
            listeners.forEach(l -> l.selectionChanged(entity.isQuery() ? entity : null));
        });

        model.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) { /* NO-OP */ }
            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                selectAndScrollTo(e.getTreePath().pathByAddingChild(e.getChildren()[0]));
            }
            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                selectAndScrollTo(e.getTreePath());
            }
            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                selectAndScrollTo(e.getTreePath());
            }
            private void selectAndScrollTo(TreePath treePath) {
                queryManagerTree.setSelectionPath(treePath);
                queryManagerTree.scrollPathToVisible(treePath);
            }
        });
        add(new JScrollPane(queryManagerTree), BorderLayout.CENTER);

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(DialogUtils.getMenuItem("Add Query...", addQueryAction));
        popupMenu.add(DialogUtils.getMenuItem("Add Group...", addGroupAction));
        JMenuItem renameMenuItem = DialogUtils.getMenuItem("Rename Query/Group", renameAction, false);
        popupMenu.add(renameMenuItem);
        JMenuItem deleteMenuItem = DialogUtils.getMenuItem("Delete Query/Group", deleteAction, false);
        popupMenu.add(deleteMenuItem);
        queryManagerTree.setComponentPopupMenu(popupMenu);

        deleteAction.setEnabled(false);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.LINE_AXIS));

        controlPanel.add(DialogUtils.getButton(addQueryAction));
        controlPanel.add(DialogUtils.getButton(addGroupAction));
        controlPanel.add(DialogUtils.getButton(deleteAction));

        queryManagerTree.getSelectionModel().addTreeSelectionListener(evt -> {
            boolean nonEmptySelection = queryManagerTree.getSelectionPaths() != null;
            renameMenuItem.setEnabled(nonEmptySelection);
            deleteAction.setEnabled(nonEmptySelection);
            deleteMenuItem.setEnabled(nonEmptySelection);
        });
        add(controlPanel, BorderLayout.NORTH);
	}

	public void addQueryManagerSelectionListener(QueryManagerPanelSelectionListener listener) {
		if (listener != null && !listeners.contains(listener))
		    listeners.add(listener);
	}

	public void removeQueryManagerSelectionListener(QueryManagerPanelSelectionListener listener) {
		if (listener != null)
		    listeners.remove(listener);
	}

    private final OntopAbstractAction addQueryAction = new OntopAbstractAction(
            "Query",
            "plus.png",
            "Create a new query") {
        @Override
        public void actionPerformed(ActionEvent evt) {
            QueryManager.Item group = getTargetForInsertion();
            String id = showNewItemDialog(
                    "New query ID:", "Create New Query", getUsedIDs(group));
            if (id != null)
                group.addQueryChild(id, "");
        }
    };

    private final OntopAbstractAction addGroupAction = new OntopAbstractAction(
            "Group",
            "plus.png",
            "Create a new group") {
        @Override
        public void actionPerformed(ActionEvent e) {
            QueryManager.Item group = getTargetForInsertion();
            String id = showNewItemDialog(
                    "New group ID:", "Create New Group", getUsedIDs(group));
            if (id != null)
                group.addGroupChild(id);
        }
    };

    private final OntopAbstractAction renameAction = new OntopAbstractAction(
            "Rename",
            null,
            null) {
        @Override
        public void actionPerformed(ActionEvent e) {
            TreePath path = queryManagerTree.getSelectionPath();
            if (path == null)
                return;

            QueryManager.Item item = (QueryManager.Item)path.getLastPathComponent();
            if (item.getParent() == null)
                return;

            String id = showNewItemDialog(
                    "<html><b>New</b> ID for " + (item.isQuery() ? "query" : "group") + " \"" + DialogUtils.htmlEscape(item.getID()) + "\":</html>",
                    "Rename " + (item.isQuery() ? "Query" : "Group") + " \"" + item.getID() + "\"",
                    getUsedIDs(item.getParent()));
            if (id != null)
                item.setID(id);
        }
    };

    private final OntopAbstractAction deleteAction = new OntopAbstractAction(
            "Delete",
            "minus.png",
            "Delete selected group or query") {
        @Override
        public void actionPerformed(ActionEvent evt) {
            TreePath path = queryManagerTree.getSelectionPath();
            if (path == null)
                return;

            QueryManager.Item item = (QueryManager.Item) path.getLastPathComponent();
            if (item.getParent() == null) // root cannot be removed
                return;

            if (!confirmDelete(item))
                return;

            item.getParent().removeChild(item);
        }
    };


    private QueryManager.Item getTargetForInsertion() {
        TreePath path = queryManagerTree.getSelectionPath();
        if (path == null)
            return queryManager.getRoot();

        QueryManager.Item item = (QueryManager.Item) path.getLastPathComponent();
        return item.isQuery() ? item.getParent() : item;
    }

    private ImmutableSet<String> getUsedIDs(QueryManager.Item item) {
	    return item.getChildren().stream()
                .map(QueryManager.Item::getID)
                .collect(ImmutableCollectors.toSet());
    }

    private String showNewItemDialog(String labelString, String title, ImmutableSet<String> usedIDs) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        JLabel label = new JLabel(labelString);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);

        panel.add(Box.createVerticalStrut(10));

        JTextField idField = new JTextField("");
        idField.setAlignmentX(Component.LEFT_ALIGNMENT);
        idField.setColumns(40);
        Border normalBorder = idField.getBorder();
        Border errorBorder = BorderFactory.createLineBorder(Color.RED, 1);
        panel.add(idField);

        panel.add(Box.createVerticalStrut(10));

        JLabel errorLabel = new JLabel("<html>&nbsp;</html>");
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(errorLabel.getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9)));
        panel.add(errorLabel);

        panel.add(Box.createVerticalStrut(20));

        // manual construction of the buttons is required to control their enabled status
        JButton okButton = new JButton(OK_BUTTON_TEXT);
        okButton.addActionListener(e ->
                getOptionPane((JComponent)e.getSource()).setValue(okButton));
        okButton.setEnabled(false);
        JButton cancelButton = new JButton(CANCEL_BUTTON_TEXT);
        cancelButton.addActionListener(e ->
                getOptionPane((JComponent)e.getSource()).setValue(cancelButton));

        idField.getDocument().addDocumentListener((SimpleDocumentListener) evt -> {
            String id = idField.getText().trim();
            if (id.isEmpty() || usedIDs.contains(id)) {
                idField.setBorder(errorBorder);
                errorLabel.setText(id.isEmpty()
                        ? "ID cannot be empty."
                        : "A query or a group with this ID already exists.");
                okButton.setEnabled(false);
            }
            else {
                idField.setBorder(normalBorder);
                errorLabel.setText("<html>&nbsp;</html>");
                okButton.setEnabled(true);
            }
        });

        if (JOptionPane.showOptionDialog(null,
                panel,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                IconLoader.getOntopIcon(),
                new Object[] { okButton, cancelButton },
                okButton) != JOptionPane.OK_OPTION)
            return null;

        return idField.getText().trim();
    }

    protected JOptionPane getOptionPane(JComponent parent) {
        return  (parent instanceof JOptionPane)
                ? (JOptionPane) parent
                : getOptionPane((JComponent)parent.getParent());
    }


	private boolean confirmDelete(QueryManager.Item item) {
        return JOptionPane.showConfirmDialog(null,
                "<html>This will delete " + (item.isQuery() ?  "query" : "group") +
                        " \""  + DialogUtils.htmlEscape(item.getID()) + "\"" +
                        (item.getChildCount() == 0 ? "" : " along with all its queries and groups") + ".<br><br>" +
                        "Do you wish to <b>continue</b>?<br></html>",
                "Delete confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                IconLoader.getOntopIcon()) == JOptionPane.YES_OPTION;
    }
}
