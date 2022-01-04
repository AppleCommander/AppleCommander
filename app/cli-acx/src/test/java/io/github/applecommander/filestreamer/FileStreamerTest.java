package io.github.applecommander.filestreamer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.DiskUnrecognizedException;

import org.junit.Test;

public class FileStreamerTest {
    private static final List<String> EXPECTED_MERLIN = Arrays.asList(
            "PRODOS", "MERLIN.SYSTEM", "PARMS", "ED", "ED.16",
            "SOURCEROR", "SOURCEROR/OBJ", "SOURCEROR/LABELS", "SOURCEROR/LABELS.S",
            "LIBRARY", "LIBRARY/SENDMSG.S", "LIBRARY/PRDEC.S", "LIBRARY/FPMACROS.S", 
                       "LIBRARY/MACROS.S", "LIBRARY/ROCKWELL.S",
            "SOURCE", "SOURCE/PARMS.S", "SOURCE/EDMAC.S", "SOURCE/KEYMAC.S",
                       "SOURCE/PRINTFILER.S", "SOURCE/MAKE.DUMP.S", "SOURCE/CLOCK.S",
                       "SOURCE/PI.START.S", "SOURCE/PI.MAIN.S", "SOURCE/PI.LOOK.S",
                       "SOURCE/PI.DIV.S", "SOURCE/PI.ADD.S", "SOURCE/PI.MACS.S",
                       "SOURCE/PI.NAMES.S",
            "UTILITIES", "UTILITIES/REMOVE.ED", "UTILITIES/EDMAC", "UTILITIES/CLOCK.12.ED",
                       "UTILITIES/XREF", "UTILITIES/XREFA", "UTILITIES/FORMATTER",
                       "UTILITIES/PRINTFILER", "UTILITIES/MON.65C02", "UTILITIES/MAKE.DUMP",
                       "UTILITIES/CONV.REL.LNK", "UTILITIES/CONV.LNK.REL", 
                       "UTILITIES/CLR.HI.BIT", "UTILITIES/KEYMAC",
            "PI", "PI/NAMES", "PI/START", "PI/MAIN", "PI/LOOK", "PI/DIV", "PI/ADD", "PI/OBJ"
        );
    private static final List<String> EXPECTED_UNIDOS = Arrays.asList(
            "HELLO", "FORMATTER", "FORMATTER.OBJ", "MFID", "FUD",   // Disk #1
            "HELLO", "MFID", "FUD"                                  // Disk #2
        );
    
    @Test
    public void testRecursiveListMerlin() throws DiskUnrecognizedException, IOException {
        List<String> actual = 
            FileStreamer.forDisk("./src/test/resources/disks/MERLIN8PRO1.DSK")
                        .recursive(true)
                        .stream()
                        .map(this::makeFullPath)
                        .collect(Collectors.toList());
        
        assertEquals(EXPECTED_MERLIN, actual);
    }

    @Test
    public void testNonRecursiveListMerlin() throws DiskUnrecognizedException, IOException {
        List<String> actual = 
            FileStreamer.forDisk("./src/test/resources/disks/MERLIN8PRO1.DSK")
                        .recursive(false)
                        .stream()
                        .map(this::makeFullPath)
                        .collect(Collectors.toList());
        
        List<String> expected = EXPECTED_MERLIN.stream()
                        .filter(s -> !s.contains("/"))
                        .collect(Collectors.toList());
        
        assertEquals(expected, actual);
    }
    
    @Test
    public void testListUnidos() throws DiskUnrecognizedException, IOException {
        List<String> actual = 
            FileStreamer.forDisk("./src/test/resources/disks/UniDOS_3.3.dsk")
                        .recursive(true)
                        .stream()
                        .map(this::makeFullPath)
                        .collect(Collectors.toList());
        
        assertEquals(EXPECTED_UNIDOS, actual);
    }


    private String makeFullPath(FileTuple tuple) {
        if (tuple.paths == null || tuple.paths.isEmpty()) {
            return tuple.fileEntry.getFilename();
        } else {
            return String.join("/", String.join("/", tuple.paths), tuple.fileEntry.getFilename());
        }
    }
}
