C MODULE SCUGNL
C-----------------------------------------------------------------------
C
C   ROUTINE TO PUNCH GENERAL UGNL PARAMETERS.
C
      SUBROUTINE SCUGNL (UNITS,IVUGNL,UGNLID,ISEASN,MDRSUB,ULLMTS,
     *   ELLMTS,NBLEND,DPOWER,DPCNMN,STMNWT,SORTBY,NHPSUB,ISTAT)
C
      CHARACTER*(*) UNITS
      CHARACTER*8 UGNLID
      CHARACTER*8 CHAR
      CHARACTER*80 CARD/' '/
C
      DIMENSION ISEASN(2),MDRSUB(4),ULLMTS(2),ELLMTS(4)
      DIMENSION NBLEND(2),DPOWER(4),NHPSUB(4)
C
      INCLUDE 'uio'
      INCLUDE 'scommon/sudbgx'
C
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/ppinit_punch/RCS/scugnl.f,v $
     . $',                                                             '
     .$Id: scugnl.f,v 1.2 1998/04/07 15:00:21 page Exp $
     . $' /
C    ===================================================================
C
C
C
      IF (ISTRCE.GT.0) THEN
         WRITE (IOSDBG,260)
         CALL SULINE (IOSDBG,1)
         ENDIF
C
C  SET DEBUG LEVEL
      LDEBUG=ISBUG('UGNL')
C
      ISTAT=0
C
      MCHAR=LEN(CHAR)
      NCHECK=0
C
      IUNDEF=-997
C
C  PRINT PARAMETER ARRAY VERSION NUMBER
      IF (LDEBUG.GT.0) THEN
         WRITE (IOSDBG,270) IVUGNL
         CALL SULINE (IOSDBG,2)
         ENDIF
C
C  PUNCH 'UGNL' STARTING IN COLUMN 1
      NPOS=1
      NSPACE=0
      CHAR='UGNL'
      LCHAR=LENSTR(CHAR)
      CALL UTOCRD (ICDPUN,NPOS,CHAR,LCHAR,NSPACE,CARD,0,NCHECK,
     *   LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
C  PUNCH UNITS TYPE
      CALL UTOCRD (ICDPUN,NPOS,'(',1,0,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
      CALL UTOCRD (ICDPUN,NPOS,UNITS,4,0,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
      CALL UTOCRD (ICDPUN,NPOS,')',1,1,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
C  PUNCH USER NAME
      CALL UTOCRD (ICDPUN,NPOS,UGNLID,LEN(UGNLID),1,CARD,3,0,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
C  PUNCH LATITUDE AND LONGITUDE LIMITS
      DO 10 I=1,4
         CALL URELCH (ULLMTS(I),MCHAR,CHAR,2,NFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
10       CONTINUE
C
C  PUNCH ELEVATION LIMITS
      DO 20 I=1,2
         VALUE=ELLMTS(I)
         IF (UNITS.EQ.'ENGL') THEN
            CALL UDUCNV ('M   ','FT  ',1,1,ELLMTS(I),VALUE,IERR)
            IF (IERR.GT.0) GO TO 240
            ENDIF
         CALL URELCH (VALUE,MCHAR,CHAR,0,NFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
20       CONTINUE
C
C  PUNCH MDR GRID SUBSET
      DO 30 I=1,4
         IF (MDRSUB(I).NE.IUNDEF) GO TO 40
30       CONTINUE
      CALL UTOCRD (ICDPUN,NPOS,'4*'',,''',6,1,CARD,4,0,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
      GO TO 60
40    DO 50 I=1,4
         CALL UINTCH (MDRSUB(I),MCHAR,CHAR,LFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,1,0,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
50       CONTINUE
C
C  PUNCH DEFAULT POWERS
60    NPOWER=0
      DO 70 I=1,3
         IF (DPOWER(I).NE.IUNDEF) GO TO 70
         NPOWER=NPOWER+1
70       CONTINUE
      IF (NPOWER.EQ.0) GO TO 150
      GO TO (80,110,140),NPOWER
80    DO 100 I=1,3
         IF (DPOWER(I).EQ.IUNDEF) GO TO 90
            CALL URELCH (DPOWER(I),MCHAR,CHAR,2,NFILL,IERR)
            CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,
     *         LNUM,IERR)
            IF (IERR.GT.0) GO TO 240
         GO TO 100
90       CALL UTOCRD (ICDPUN,NPOS,',,',2,1,CARD,0,NCHECK,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
100      CONTINUE
         GO TO 170
110   I=1
      IF (DPOWER(I).EQ.IUNDEF) GO TO 130
120      CALL URELCH (DPOWER(I),MCHAR,CHAR,2,NFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
            I=I+1
            IF (I.GE.3) GO TO 170
130   CALL UTOCRD (ICDPUN,NPOS,',,,',3,1,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
         I=I+2
         IF (I.GT.3) GO TO 170
         GO TO 120
140   CALL UTOCRD (ICDPUN,NPOS,'3*'',,''',6,1,CARD,4,0,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
      GO TO 170
150   DO 160 I=1,3
         CALL URELCH (DPOWER(I),MCHAR,CHAR,2,NFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
160      CONTINUE
C
C  PUNCH BEGINNING MONTHS OF SUMMER AND WINTER SEASON
170   DO 180 I=1,2
         CALL UINTCH (ISEASN(I),MCHAR,CHAR,LFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,1,0,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
180      CONTINUE
C
C  PUNCH BLEND FACTORS
      DO 190 I=1,2
         CALL UINTCH (NBLEND(I),MCHAR,CHAR,LFILL,IERR)
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,1,0,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
190      CONTINUE
C
C  PUNCH MINIMUM DAILY PRECIPITATION FOR ESTIMATING TIME DISTRIBUTION
      VALUE=DPCNMN
      IF (UNITS.EQ.'METR') THEN
         CALL UDUCNV ('IN  ','MM  ',1,1,DPCNMN,VALUE,IERR)
         IF (IERR.GT.0) GO TO 240
         ENDIF
      CALL URELCH (VALUE,MCHAR,CHAR,2,NFILL,IERR)
      CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
C  PUNCH MINIMUM WEIGHT OF STATIONS TO BE KEPT FOR STATION WEIGHTING
      CALL URELCH (STMNWT,MCHAR,CHAR,2,NFILL,IERR)
      CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,1,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
C  PUNCH INDICATOR HOW ALPHABETICAL ORDER LISTS TO BE SORTED
      CALL UTOCRD (ICDPUN,NPOS,SORTBY,4,1,CARD,0,NCHECK,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
      IF (IVUGNL.LT.2) GO TO 230
C
C  PUNCH HRAP GRID SUBSET
      DO 200 I=1,4
         IF (NHPSUB(I).NE.IUNDEF) GO TO 210
200      CONTINUE
      GO TO 230
210   CALL UTOCRD (ICDPUN,NPOS,'HRAP(',5,0,CARD,1,0,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
      NSPACE=1
      DO 220 I=1,4
         CALL UINTCH (NHPSUB(I),MCHAR,CHAR,LFILL,IERR)
         IF (I.EQ.4) NSPACE=0
         CALL UTOCRD (ICDPUN,NPOS,CHAR,MCHAR,NSPACE,CARD,1,0,LNUM,IERR)
         IF (IERR.GT.0) GO TO 240
220      CONTINUE
      CALL UTOCRD (ICDPUN,NPOS,')',1,1,CARD,1,0,LNUM,IERR)
      IF (IERR.GT.0) GO TO 240
C
230   CALL UPNCRD (ICDPUN,CARD)
      GO TO 250
C
240   ISTAT=1
C
250   IF (ISTRCE.GT.0) THEN
         WRITE (IOSDBG,280)
         CALL SULINE (IOSDBG,1)
         ENDIF
C
      RETURN
C
C- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
C
260   FORMAT (' *** ENTER SCUGNL')
270   FORMAT ('0PARAMETER ARRAY VERSION NUMBER = ',I2)
280   FORMAT (' *** EXIT SCUGNL')
C
      END
