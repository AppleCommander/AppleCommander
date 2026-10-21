GAMEDOS
=======

  struct gd_dirent {
    char type;                /* 'A' = Applesoft, 'B' = Binary */
    unsigned char track1;
    unsigned char sector1;
    unsigned short int meta;  /* load addr for Binary,
                                 end of code addr for Applesoft */
    char filename[26];        /* space-padded */
    unsigned char len;        /* in sectors; file must be contiguous */
  };
  
  There are 16 dirents, stored from $BE00 up (track 00 sectors E and F)
  
  The following types are defined but not yet implemented:
  
    * T - Text
    * S - System, loads at $0800 (NOT $2000 as ProDOS!)
    * P - Picture, loads at $2000
    * I - Integer Basic
    
  GameDOS hooks the reset vector and the jump instruction at $03D0.
  
  I've got a program for qbasic that outputs GameDOS disks but it's clumsy
  and I'd rather use 6502 code for that purpose.
