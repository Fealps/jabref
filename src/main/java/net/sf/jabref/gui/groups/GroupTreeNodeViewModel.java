/*  Copyright (C) 2003-2015 JabRef contributors.
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.groups;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.undo.CountingUndoManager;
import net.sf.jabref.logic.groups.*;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

public class GroupTreeNodeViewModel implements Transferable, TreeNode {

    private static final int MAX_DISPLAYED_LETTERS = 35;
    private static final Icon GROUP_REFINING_ICON = IconTheme.JabRefIcon.GROUP_REFINING.getSmallIcon();
    private static final Icon GROUP_INCLUDING_ICON = IconTheme.JabRefIcon.GROUP_INCLUDING.getSmallIcon();
    private static final Icon GROUP_REGULAR_ICON = null;

    public static final DataFlavor FLAVOR;
    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor df = null;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=net.sf.jabref.logic.groups.GroupTreeNode");
        } catch (ClassNotFoundException e) {
            // never happens
        }
        FLAVOR = df;
        FLAVORS = new DataFlavor[] {GroupTreeNodeViewModel.FLAVOR};
    }

    private final GroupTreeNode node;

    public GroupTreeNodeViewModel(GroupTreeNode node) {
        this.node = node;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return GroupTreeNodeViewModel.FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(GroupTreeNodeViewModel.FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor)) {
            throw new UnsupportedFlavorException(someFlavor);
        }
        return this;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return node.getChildAt(childIndex).map(GroupTreeNodeViewModel::new).orElse(null);
    }

    @Override
    public int getChildCount() {
        return node.getNumberOfChildren();
    }

    @Override
    public TreeNode getParent() {
        Optional<GroupTreeNode> parent = node.getParent();
        return parent.map(GroupTreeNodeViewModel::new).orElse(null);
    }

    @Override
    public int getIndex(TreeNode child) {
        if(! (child instanceof GroupTreeNodeViewModel)) {
            return -1;
        }

        GroupTreeNodeViewModel childViewModel = (GroupTreeNodeViewModel)child;
        return node.getIndexOfChild(childViewModel.getNode()).orElse(-1);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return node.isLeaf();
    }

    @Override
    public Enumeration children() {
        Iterable<GroupTreeNode> children = node.getChildren();
        return new Enumeration() {

            @Override
            public boolean hasMoreElements() {
                return children.iterator().hasNext();
            }

            @Override
            public Object nextElement() {
                return children.iterator().next();
            }
        };
    }

    public GroupTreeNode getNode() {
        return node;
    }

    /** Collapse this node and all its children. */
    public void collapseSubtree(JTree tree) {
        tree.collapsePath(this.getTreePath());

        for(GroupTreeNodeViewModel child : getChildren()) {
            child.collapseSubtree(tree);
        }
    }

    /** Expand this node and all its children. */
    public void expandSubtree(JTree tree) {
        tree.expandPath(this.getTreePath());

        for(GroupTreeNodeViewModel child : getChildren()) {
            child.collapseSubtree(tree);
        }
    }

    public List<GroupTreeNodeViewModel> getChildren() {
        List<GroupTreeNodeViewModel> children = new ArrayList<>();
        for(GroupTreeNode child : node.getChildren()) {
            children.add(new GroupTreeNodeViewModel(child));
        }
        return children;
    }

    protected boolean printInItalics() {
        return Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC) &&  node.getGroup().isDynamic();
    }

    public String getText() {
        AbstractGroup group = node.getGroup();
        String name = StringUtil.limitStringLength(group.getName(), MAX_DISPLAYED_LETTERS);
        StringBuilder sb = new StringBuilder(60);
        sb.append(name);

        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS)) {
            if (group instanceof ExplicitGroup) {
                sb.append(" [").append(((ExplicitGroup) group).getNumEntries()).append(']');
            } else if ((group instanceof KeywordGroup) || (group instanceof SearchGroup)) {
                int hits = 0;
                BasePanel currentBasePanel = JabRef.jrf.getCurrentBasePanel();
                if(currentBasePanel != null) {
                    for (BibEntry entry : currentBasePanel.getDatabase().getEntries()) {
                        if (group.contains(entry)) {
                            hits++;
                        }
                    }
                }
                sb.append(" [").append(hits).append(']');
            }
        }

        return sb.toString();
    }

    public String getDescription() {
        return "<html>" + node.getGroup().getShortDescription() + "</html>";
    }

    public Icon getIcon() {
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_ICONS)) {
            switch (node.getGroup().getHierarchicalContext()) {
            case REFINING:
                return GROUP_REFINING_ICON;
            case INCLUDING:
                return GROUP_INCLUDING_ICON;
            default:
                return GROUP_REGULAR_ICON;
            }
        } else {
            return null;
        }
    }

    public TreePath getTreePath() {
        List<GroupTreeNode> pathToNode = node.getPathFromRoot();
        return new TreePath(pathToNode.stream().map(GroupTreeNodeViewModel::new).toArray());
    }

    public boolean canAddEntries(List<BibEntry> entries) {
        return getNode().getGroup().supportsAdd() && !getNode().getGroup().containsAll(entries);
    }

    public boolean canRemoveEntries(List<BibEntry> entries) {
        return getNode().getGroup().supportsRemove() && getNode().getGroup().containsAny(entries);
    }

    public void sortChildrenByName(boolean recursive) {
        getNode().sortChildren(
                (node1, node2) -> node1.getGroup().getName().compareToIgnoreCase(node2.getGroup().getName()),
                recursive);
    }

    public String getName() {
        return getNode().getGroup().getName();
    }

    public boolean canBeEdited() {
        return getNode().getGroup() instanceof AllEntriesGroup;
    }

    public boolean canMoveUp() {
        return (getNode().getPreviousSibling() != null)
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveDown() {
        return (getNode().getNextSibling() != null)
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveLeft() {
        return !(getNode().getGroup() instanceof AllEntriesGroup)
                // TODO: Null!
                && !(getNode().getParent().get().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveRight() {
        return (getNode().getPreviousSibling() != null)
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public void changeEntriesTo(List<BibEntry> entries, UndoManager undoManager) {
        AbstractGroup group = node.getGroup();
        Optional<EntriesGroupChange> changesRemove = Optional.empty();
        Optional<EntriesGroupChange> changesAdd = Optional.empty();

        // Sort entries into current members and non-members of the group
        // Current members will be removed
        // Current non-members will be added
        List<BibEntry> toRemove = new ArrayList<>(entries.size());
        List<BibEntry> toAdd = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            // Sort according to current state of the entries
            if (group.contains(entry)) {
                toRemove.add(entry);
            } else {
                toAdd.add(entry);
            }
        }

        // If there are entries to remove
        if (!toRemove.isEmpty()) {
            changesRemove = node.removeFromGroup(toRemove);
        }
        // If there are entries to add
        if (!toAdd.isEmpty()) {
            changesAdd = node.addToGroup(toAdd);
        }

        // Remember undo information
        if (changesRemove.isPresent()) {
            AbstractUndoableEdit undoRemove = UndoableChangeEntriesOfGroup.getUndoableEdit(this, changesRemove.get());
            if (changesAdd.isPresent() && undoRemove != null) {
                // we removed and added entries
                undoRemove.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(this, changesAdd.get()));
            }
            undoManager.addEdit(undoRemove);
        } else if (changesAdd != null) {
            undoManager.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(this, changesAdd.get()));
        }
    }

    public boolean isAllEntriesGroup() {
        return getNode().getGroup() instanceof AllEntriesGroup;
    }

    public void addNewGroup(AbstractGroup newGroup, CountingUndoManager undoManager, GroupSelector groupSelector) {
        GroupTreeNode newNode = new GroupTreeNode(newGroup);
        this.getNode().addChild(newNode);

        UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupSelector, this,
                new GroupTreeNodeViewModel(newNode), UndoableAddOrRemoveGroup.ADD_NODE);
        undoManager.addEdit(undo);
    }
}
