/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003-2022 by Robert Greene
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your 
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.webcodepro.applecommander.ui.swt.wizard.importfile;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.ImportSpecification;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.wizard.Wizard;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.ApplesoftTokenizer;
import io.github.applecommander.bastools.api.Configuration;
import io.github.applecommander.bastools.api.Parser;
import io.github.applecommander.bastools.api.TokenReader;
import io.github.applecommander.bastools.api.Visitors;
import io.github.applecommander.bastools.api.model.Program;
import io.github.applecommander.bastools.api.model.Token;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * The Disk Import Wizard.
 * <br>
 * Created on Jan 16, 2003.
 * @author Rob Greene
 */
public class ImportWizard extends Wizard {
	private static Set<String> APPLESOFT_FILETYPES = Set.of("B", "BAS");
	private FormattedDisk disk;
	private List<ImportSpecification> importSpecifications;
	/**
	 * Constructor for ImportWizard.
	 */
	public ImportWizard(Shell parent, ImageManager imageManager, FormattedDisk disk) {
		super(parent, imageManager.get(ImageManager.LOGO_IMPORT_WIZARD), 
				UiBundle.getInstance().get("ImportWizardTitle")); //$NON-NLS-1$
		this.disk = disk;
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.Wizard#createInitialWizardPane()
	 */
	public WizardPane createInitialWizardPane() {
		return new ImportSelectFilesWizardPane(getContentPane(), this);
	}
	/**
	 * Add an import specification.
	 */
	public void addImportSpecification(ImportSpecification importSpecification) throws IOException {
		validate(importSpecification);
		getImportSpecifications().add(importSpecification);
	}

	/**
	 * Perform validation for some problematic file type imports.
	 */
	public void validate(ImportSpecification importSpecification) throws IOException {
		if (APPLESOFT_FILETYPES.contains(importSpecification.getFiletype())) {
			try {
				// 1. Validate that this is a binary Applesoft file.
				byte[] data = Files.readAllBytes(Path.of(importSpecification.getSourceFilename()));
				ApplesoftTokenizer tokenizer = new ApplesoftTokenizer(data);
				while (tokenizer.hasMoreTokens()) {
					tokenizer.getNextToken();    // Make sure we can loop through entire program
				}
				importSpecification.setFileData(data);
			} catch (Throwable ignored) {
				try {
					// 2. Make an attempt at tokenizing the file (assuming it's text).
					File file = new File(importSpecification.getSourceFilename());
					Configuration config = Configuration.builder().sourceFile(file).build();
					Queue<Token> tokens = TokenReader.tokenize(config.sourceFile);
					Parser parser = new Parser(tokens);
					Program program = parser.parse();
					byte[] data = Visitors.byteVisitor(config).dump(program);
					importSpecification.setFileData(data);
				} catch (Throwable ignored2) {
					throw new IOException("File does not appear to be an Applesoft program");
				}
			}
		}
	}
	/**
	 * Remove an import specification.
	 */
	public void removeImportSpecification(ImportSpecification importSpecification) {
		getImportSpecifications().remove(importSpecification);
	}
	/**
	 * Get the list of ImportSpecifications.
	 */
	public List<ImportSpecification> getImportSpecifications() {
		if (importSpecifications == null) {
			importSpecifications = new ArrayList<>();
		}
		return importSpecifications;
	}
	/**
	 * Get the FormattedDisk the wizard is working with.
	 */
	public FormattedDisk getDisk() {
		return disk;
	}
}
