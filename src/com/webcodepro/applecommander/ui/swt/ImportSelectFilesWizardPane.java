package com.webcodepro.applecommander.ui.swt;

import com.webcodepro.applecommander.ui.ImportSpecification;

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Allow the used to choose the files to import into the disk image.
 * <br>
 * Created on Jan 17, 2003.
 * @author Rob Greene
 */
public class ImportSelectFilesWizardPane extends WizardPane {
	private ImportWizard wizard;
	private Composite control;
	private Composite parent;
	private Button removeButton;
	private Button editButton;
	private Table fileTable;
	/**
	 * Constructor for ImportSelectFilesWizardPane.
	 */
	public ImportSelectFilesWizardPane(Composite parent, ImportWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Get the next visible pane.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Create the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#open()
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
		wizard.enableNextButton(false);
		wizard.enableFinishButton(false);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		control.setLayout(layout);
		Label label = new Label(control, SWT.WRAP);
		label.setText("Please choose the files to be imported:");

		fileTable = new Table(control, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
			| SWT.V_SCROLL | SWT.H_SCROLL);
		fileTable.setLayoutData(new RowData(330, 100));
		fileTable.setHeaderVisible(true);
		fileTable.addSelectionListener(new SelectionAdapter() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				removeButton.setEnabled(true);
				editButton.setEnabled(true);
			}
		});
		
		TableColumn column = new TableColumn(fileTable, SWT.LEFT);
		column.setText("Source");
		column.setWidth(130);
		column = new TableColumn(fileTable, SWT.LEFT);
		column.setText("Target");
		column.setWidth(130);
		column = new TableColumn(fileTable, SWT.LEFT);
		column.setText("Type");
		column.setWidth(70);
		
		Composite buttonPanel = new Composite(control, SWT.NULL);
		buttonPanel.setLayout(new FillLayout());

		Button chooseButton = new Button(buttonPanel, SWT.PUSH);
		chooseButton.setText("Choose...");
		chooseButton.setFocus();
		chooseButton.addSelectionListener(new SelectionListener() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(parent.getShell(), 
					SWT.OPEN | SWT.MULTI);
				String filename = dialog.open();
				if (filename != null) {
					setFilenames(dialog.getFileNames());
				}
			}
			/**
			 * Double-click.
			 */
			public void widgetDefaultSelected(SelectionEvent event) {
				editSelection();
			}
		});

		removeButton = new Button(buttonPanel, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				TableItem[] items = fileTable.getSelection();
				for (int i=0; i<items.length; i++) {
					ImportSpecification spec = (ImportSpecification) 
						items[i].getData();
					wizard.removeImportSpecification(spec);
				}
				removeButton.setEnabled(false);
				refreshTable();
			}
		});
		
		editButton = new Button(buttonPanel, SWT.PUSH);
		editButton.setText("Edit...");
		editButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				editSelection();
			}
		});
	}
	/**
	 * Set all filenames to be imported.
	 */
	protected void setFilenames(String[] filenames) {
		for (int i=0; i<filenames.length; i++) {
			ImportSpecification spec = new ImportSpecification(
				filenames[i],
				wizard.getDisk().getSuggestedFilename(filenames[i]));
			wizard.addImportSpecification(spec);
		}
		refreshTable();
	}
	/**
	 * Refresh the table of information.
	 */
	protected void refreshTable() {
		fileTable.removeAll();
		Iterator specs = wizard.getImportSpecifications().iterator();
		boolean canFinish = specs.hasNext();
		while (specs.hasNext()) {
			ImportSpecification spec = (ImportSpecification) specs.next();
			TableItem item = new TableItem(fileTable, SWT.NULL);
			item.setText(new String[] {
				spec.getSourceFilename(),
				spec.getTargetFilename(),
				spec.getFiletype() });
			item.setData(spec);
			canFinish &= (spec.getFiletype() != null 
				&& spec.getFiletype().length() > 0);
		}
		fileTable.redraw();
		wizard.enableFinishButton(canFinish);
	}
	/**
	 * Dispose of all resources.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#dispose()
	 */
	public void dispose() {
		fileTable.dispose();
		control.dispose();
	}
	/**
	 * Edit the current selection.
	 */
	public void editSelection() {
		// FIXME
	}
}
