package com.webcodepro.applecommander.util.filestreamer;

import java.util.function.Predicate;

public enum TypeOfFile {
    FILE(tuple -> !tuple.fileEntry.isDirectory()), 
    DIRECTORY(tuple -> tuple.fileEntry.isDirectory()), 
    BOTH(tuple -> true);
    
    public final Predicate<FileTuple> predicate;
    
    private TypeOfFile(Predicate<FileTuple> predicate) {
        this.predicate = predicate;
    }
}