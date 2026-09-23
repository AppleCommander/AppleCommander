package org.applecommander.util;

import java.util.List;

/**
 * An information provider supplies groups of information.
 */
public interface InformationProvider {
    List<InformationGroup> information();
}
