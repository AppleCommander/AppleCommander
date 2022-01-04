# `ac`, extended

This is a revamp of the venerable `ac` command-line utility with a more modern command-line interface.

# Sample Run

All of the commands use `-d` to designate the disk. If there are many commands, setting the environment variable `ACX_DISK_NAME` can be used to simplify commands. 

For example, this sequence:
1. Creates a ProDOS disk image (pulling ProDOS from the 2.4.2 system master).
2. Imports and tokenizes the sample `startup.bas` program and places it on the disk as `STARTUP`.
3. Lists the resulting disk.

```
$ cat > startup.bas
10 TEXT:HOME:GR
20 FOR Y=0 TO 3
30 FOR X=0 TO 3
40 COLOR=Y*4+X
50 FOR A=0 TO 9  
60 HLIN X*10,X*10+9 AT Y*10+A
70 NEXT A,X,Y
80 END
<ctrl+D>

$ export ACX_DISK_NAME=sample.po
$ acx create --format=ProDOS_2_4_2.dsk --name=SAMPLES --size=140k --type=prodos
$ acx import --basic startup.bas --name=STARTUP

$ acx list --native
File: sample.po
Name: /SAMPLES/
* PRODOS          SYS      035 01/02/2022 01/13/2018     17,128          
* BASIC.SYSTEM    SYS      021 01/02/2022 01/13/2018     10,240 A=$2000  
  STARTUP         BAS      001 01/02/2022 01/02/2022         97 A=$0801  
ProDOS format; 110592 bytes free; 32768 bytes used.
```


# Usage

```
$ acx --help                                                                    
Usage: acx [-hVv] [--debug] [--quiet] [COMMAND]

'ac' experimental utility

Options:
      --debug     Show detailed stack traces.
  -h, --help      Show this help message and exit.
      --quiet     Turn off all logging.
  -v, --verbose   Be verbose. Multiple occurrences increase logging.
  -V, --version   Print version information and exit.

Commands:
  convert          Uncompress a ShrinkIt or Binary II file;
  copy, cp         Copy files between disks.
  create, mkdisk   Rename volume of a disk image.
  delete, del, rm  Delete file(s) from a disk image.
  diskmap, map     Show disk usage map.
  export, x, get   Export file(s) from a disk image.
  help             Displays help information about the specified command
  import, put      Import file onto disk.
  info, i          Show information on a disk image(s).
  list, ls         List directory of disk image(s).
  lock             Lock file(s) on a disk image.
  mkdir, md        Create a directory on disk.
  rename, ren      Rename file on a disk image.
  rename-disk      Rename volume of a disk image.
  rmdir, rd        Remove a directory on disk.
  unlock           Unlock file(s) on a disk image.
```

## Info

```
$ acx info --help
Usage: acx info [-h] <paths>...

Show information on a disk image(s).

Parameters:
      <paths>...   Image(s) to process.

Options:
  -h, --help       Show help for subcommand.
```

```
$ acx info "Beagle Graphics.dsk" 
File Name: Beagle Graphics.dsk
Disk Name: DISK VOLUME #254
Physical Size (bytes): 143360
Free Space (bytes): 20480
Used Space (bytes): 122880
Physical Size (KB): 140
Free Space (KB): 20
Used Space (KB): 120
Archive Order: DOS
Disk Format: DOS 3.3
Total Sectors: 560
Free Sectors: 80
Used Sectors: 480
Tracks On Disk: 35
Sectors On Disk: 16
```

## List

```
$ acx list --help
Usage: acx list [-hr] [--[no-]column] [--deleted] [--[no-]footer] [--[no-]
                header] [--globs=<globs>[,<globs>...]]... [-n | -s | -l]
                [--file | --directory] <paths>...

List directory of disk image(s).

Parameters:
      <paths>...            Image(s) to process.

Options:
      --[no-]column         Show column headers.
      --deleted             Show deleted files.
      --directory           Only include directories.
      --file                Only include files.
      --[no-]footer         Show footer.
      --globs=<globs>[,<globs>...]
                            File glob(s) to match.
  -h, --help                Show help for subcommand.
      --[no-]header         Show header.
  -r, --[no-]recursive      Display directory recursively.
File display formatting:
  -l, --long, --detail      Use long/detailed directory format.
  -n, --native              Use native directory format (default).
  -s, --short, --standard   Use brief directory format.
```

```
$ acx list --no-recursive DEVCD.HDV 
File: DEVCD.HDV
Name: /DEV.CD/
  TOOLS           DIR      002 06/25/1990 04/13/1989      1,024          
  II.DISK.CENTRAL DIR      001 06/25/1990 04/13/1989        512          
  UTILITIES       DIR      002 06/25/1990 04/13/1989      1,024          
  READ.ME.FIRST   DIR      001 06/25/1990 04/21/1989        512          
  GUIDED.TOURS    DIR      001 06/25/1990 04/13/1989        512          
  FINDER.DATA     FND      001 06/25/1990 10/12/1989        172          
  DEVELOP         DIR      001 07/05/1990 06/25/1990        512          
ProDOS format; 1701376 bytes free; 19270144 bytes used.
```
