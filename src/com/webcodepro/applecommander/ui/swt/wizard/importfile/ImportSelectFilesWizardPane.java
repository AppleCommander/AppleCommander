package com.webcodepro.applecommander.ui.swt.wizard.importfile;

import java.io.File;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.webcodepro.applecommander.ui.ImportSpecification;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Allow the used to choose the files to import into the disk image.
 * <br>
 * Created on Jan 17, 2003.
 * @author Rob Greene
 */
public class ImportSelectFilesWizardPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private ImportWizard wizard;
	private Composite control;
	private Composite parent;
	private Button removeButton;
	private Button editButton;
	private Table fileTable;
	private Text addressText;
	private Button rawCheckbox;
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
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Create the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#open()
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
		label.setText(textBundle.get("ImportWizardPrompt")); //$NON-NLS-1$

		fileTable = new Table(control, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
			| SWT.V_SCROLL | SWT.H_SCROLL);
		fileTable.setLayoutData(new RowData(330, 100));
		fileTable.setHeaderVisible(true);
		fileTable.addSelectionListener(new SelectionAdapter() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				getRemoveButton().setEnabled(true);
				getEditButton().setEnabled(true);
			}
			/**
			 * Double-click.
			 */
			public void widgetDefaultSelected(SelectionEvent event) {
				editSelection();
			}
		});
		
		TableColumn column = new TableColumn(fileTable, SWT.LEFT);
		column.setText(textBundle.get("SourceColumnHeader")); //$NON-NLS-1$
		column.setWidth(130);
		column = new TableColumn(fileTable, SWT.LEFT);
		column.setText(textBundle.get("TargetColumnHeader")); //$NON-NLS-1$
		column.setWidth(130);
		column = new TableColumn(fileTable, SWT.LEFT);
		column.setText(textBundle.get("TypeColumnHeader")); //$NON-NLS-1$
		column.setWidth(70);
		
		Composite buttonPanel = new Composite(control, SWT.NULL);
		buttonPanel.setLayout(new FillLayout());

		Button chooseButton = new Button(buttonPanel, SWT.PUSH);
		chooseButton.setText(textBundle.get("ChooseButton")); //$NON-NLS-1$
		chooseButton.setFocus();
		chooseButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(getParent().getShell(), 
					SWT.OPEN | SWT.MULTI);
				dialog.setFilterPath(
					UserPreferences.getInstance().getImportDirectory());
				String filename = dialog.open();
				if (filename != null) {
					setFilenames(dialog.getFilterPath(), dialog.getFileNames());
					UserPreferences.getInstance().setImportDirectory(
						dialog.getFilterPath());
				}
			}
		});

		removeButton = new Button(buttonPanel, SWT.PUSH);
		removeButton.setText(textBundle.get("RemoveButton")); //$NON-NLS-1$
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * Single click.
			 */
			public void widgetSelected(SelectionEvent event) {
				TableItem[] items = getFileTable().getSelection();
				for (int i=0; i<items.length; i++) {
					ImportSpecification spec = (ImportSpecification) 
						items[i].getData();
					getWizard().removeImportSpecification(spec);
				}
				getRemoveButton().setEnabled(false);
				refreshTable();
			}
		});
		
		editButton = new Button(buttonPanel, SWT.PUSH);
		editButton.setText(textBundle.get("EditButton")); //$NON-NLS-1$
		editButton.setEnabled(false);
		editButton.addSelectionListener(new SelectionAdapter() {
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
	protected void setFilenames(String path, String[] filenames) {
		for (int i=0; i<filenames.length; i++) {
			ImportSpecification spec = new ImportSpecification(
				path + File.separatorChar+ filenames[i],
				wizard.getDisk().getSuggestedFilename(filenames[i]),
				wizard.getDisk().getSuggestedFiletype(filenames[i]));
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
			File file = new File(spec.getSourceFilename());
			TableItem item = new TableItem(fileTable, SWT.NULL);
			item.setText(new String[] {
				file.getName(),
				spec.getTargetFilename(),
				spec.getFiletype() });
			item.setData(spec);
			canFinish &= spec.hasFiletype();
		}
		fileTable.redraw();
		wizard.enableFinishButton(canFinish);
	}
	/**
	 * Dispose of all resources.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
	 */
	public void dispose() {
		fileTable.dispose();
		control.dispose();
	}
	/**
	 * Edit the current selection.
	 */
	public void editSelection() {
		final ImportSpecification spec = (ImportSpecification) 
			fileTable.getSelection()[0].getData();
		
		final Shell dialog = new Shell(wizard.getDialog(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(textBundle.get("FileImportSettingsTitle")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.makeColumnsEqualWidth = false;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.numColumns = 2;
		layout.verticalSpacing = 5;
		dialog.setLayout(layout);
		
		// Filename prompt:
		Label label = new Label(dialog, SWT.NONE);
		label.setText(textBundle.get("FilenameLabel")); //$NON-NLS-1$
		final Text filenameText = new Text(dialog, SWT.BORDER);
		filenameText.setText(spec.getTargetFilename());
		filenameText.setTextLimit(30);
		GridData layoutData = new GridData();
		layoutData.widthHint = 150;
		filenameText.setLayoutData(layoutData);
		
		// Filetype prompt:
		label = new Label(dialog, SWT.NONE);
		label.setText(textBundle.get("FiletypeLabel")); //$NON-NLS-1$
		final Combo filetypes = new Combo(dialog, SWT.BORDER | SWT.READ_ONLY);
		filetypes.setItems(wizard.getDisk().getFiletypes());
		if (spec.hasFiletype()) {
			filetypes.select(filetypes.indexOf(spec.getFiletype()));
		}
		
		// Address component: (only used for some filetypes)
		label = new Label(dialog, SWT.NONE);
		label.setText(textBundle.get("AddressLabel")); //$NON-NLS-1$
		addressText = new Text(dialog, SWT.BORDER);
		addressText.setTextLimit(5);
		addressText.setText(AppleUtil.getFormattedWord(spec.getAddress()));
		layoutData = new GridData();
		layoutData.widthHint = 75;
		addressText.setLayoutData(layoutData);
		if (spec.hasFiletype()) {
			addressText.setEnabled(
				wizard.getDisk().needsAddress(spec.getFiletype()));
		}
		
		// RAW file image: (probably only used for DOS 3.3)
		label = new Label(dialog, SWT.NONE);
		label.setText(textBundle.get("RawBinaryCheckbox")); //$NON-NLS-1$
		rawCheckbox = new Button(dialog, SWT.CHECK);
		rawCheckbox.setSelection(spec.isRawFileImport());

		// Enable/disable the address component:
		filetypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				String filetype = filetypes.getItem(
					filetypes.getSelectionIndex());
				getAddressText().setEnabled(
					getWizard().getDisk().needsAddress(filetype));
			}
		});

		// Bottom row of buttons
		layoutData = new GridData();
		layoutData.horizontalSpan = 2;
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.horizontalAlignment = GridData.CENTER;
		Composite composite = new Composite(dialog, SWT.NONE);
		composite.setLayoutData(layoutData);
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));
		Button button = new Button(composite, SWT.PUSH);
		button.setText(textBundle.get("CancelButton")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				filenameText.dispose();
				filetypes.dispose();
				getAddressText().dispose();
				getRawCheckBox().dispose();
				dialog.close();
			}
		});
		button = new Button(composite, SWT.PUSH);
		button.setText(textBundle.get("OkButton")); //$NON-NLS-1$
		dialog.setDefaultButton(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				spec.setTargetFilename(getWizard().getDisk().
					getSuggestedFilename(filenameText.getText()));
				spec.setFiletype(filetypes.getItem(
					filetypes.getSelectionIndex()));
				spec.setAddress(AppleUtil.convertFormattedWord(
					getAddressText().getText()));
				spec.setRawFileImport(getRawCheckBox().getSelection());
				filenameText.dispose();
				filetypes.dispose();
				getAddressText().dispose();
				getRawCheckBox().dispose();
				dialog.close();
				refreshTable();
			}
		});
		dialog.pack();
		SwtUtil.center(wizard.getDialog(), dialog);
		dialog.open();
	}
	
	protected Button getRemoveButton() {
		return removeButton;
	}
	
	protected Button getEditButton() {
		return editButton;
	}
	
	protected Composite getParent() {
		return parent;
	}
	
	protected Table getFileTable() {
		return fileTable;
	}
	
	protected ImportWizard getWizard() {
		return wizard;
	}
	
	protected Text getAddressText() {
		return addressText;
	}
	
	protected Button getRawCheckBox() {
		return rawCheckbox;
	}
}
