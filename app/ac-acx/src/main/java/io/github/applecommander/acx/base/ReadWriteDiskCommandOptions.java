package io.github.applecommander.acx.base;

public abstract class ReadWriteDiskCommandOptions extends ReadOnlyDiskImageCommandOptions {
    @Override
    public Integer call() throws Exception {
        int returnCode = handleCommand();
        
        if (returnCode == 0) {
            saveDisk(disk);
        }
        
        return returnCode;
    }
}
