package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ExceptionsRuleDialog;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;

public class ExceptionsTable extends AbstractFilterTable {

    private static final long serialVersionUID = 4698030718806607175L;
    private LinkgrabberFilter linkgrabberFilter;

    public ExceptionsTable(LinkgrabberFilter linkgrabberFilter) {
        super(new ExceptionsTableModel("ExceptionsTableModel"));
        this.setSearchEnabled(true);
        this.linkgrabberFilter = linkgrabberFilter;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu
     * , java.lang.Object, java.util.ArrayList,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, LinkgrabberFilterRule contextObject, ArrayList<LinkgrabberFilterRule> selection, ExtColumn<LinkgrabberFilterRule> column, MouseEvent ev) {
        popup.add(new JMenuItem(new NewAction(this).toContextMenuAction()));
        popup.add(new JMenuItem(new RemoveAction(this, selection, false).toContextMenuAction()));

        popup.add(new JMenuItem(new DuplicateAction(contextObject, this).toContextMenuAction()));
        popup.addSeparator();
        popup.add(new ExportAction(selection).toContextMenuAction());
        return popup;
    }

    @Override
    protected void onDoubleClick(MouseEvent e, LinkgrabberFilterRule obj) {
        try {
            Dialog.getInstance().showDialog(new ExceptionsRuleDialog(obj));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
        getExtTableModel().fireTableDataChanged();
        LinkFilterController.getInstance().update();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.ArrayList
     * , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(ArrayList<LinkgrabberFilterRule> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(this, selectedObjects, direct).actionPerformed(null);
        return true;
    }
}
