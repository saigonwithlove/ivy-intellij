// Copyright 2010 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms of any of the following licenses:
//
//  LGPL, GNU Lesser General Public License, V2.1 or later, http://www.gnu.org/licenses/lgpl.html
//  EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.
//
// Home page: http://www.source-code.biz/filemirrorsync

package saigonwithlove.ivy.intellij.mirror;

import java.nio.file.Path;
import java.nio.file.Paths;

// FileMirrorSync - A file mirror synchronization tool (one-way file sync, incremental file copy).
// Driver for the command line interface.
public class FileMirrorSync {

  private static Path sourcePath;
  private static Path targetPath;
  private static FileSyncProcessor.Options options;
  private static boolean displayHelpOption;
  private static int exitStatusCode;

  public static void main(String[] args) {
    try {
      main2(args);
    } catch (CommandLineParser.CommandLineException e) {
      exitStatusCode = 5;
      System.err.println("FileMirrorSync error: " + e.getMessage());
    } catch (Throwable e) {
      exitStatusCode = 9;
      System.err.print("FileMirrorSync error: ");
      e.printStackTrace(System.err);
    }
    System.exit(exitStatusCode);
  }

  private static void main2(String[] args) throws Exception {
    options = new FileSyncProcessor.Options();
    parseCommandLineArgs(args);
    if (displayHelpOption) {
      displayHelp();
      return;
    }
    FileSyncProcessor fsp = new FileSyncProcessor();
    UserInterface ui = new UserInterface();
    FileSyncProcessor.Statistics statistics = new FileSyncProcessor.Statistics();
    fsp.main(sourcePath, targetPath, options, ui, statistics);
    displayStatistics(statistics);
  }

  // --- User interface
  // ------------------------------------------------------------------------------

  private static class UserInterface implements FileSyncProcessor.UserInterface {
    boolean tickerStarted;

    public void writeInfo(String s) {
      System.out.println(s);
    }

    public void writeInfoTicker() {
      System.out.print('.');
      tickerStarted = true;
    }

    public void endInfoTicker() {
      if (!tickerStarted) return;
      System.out.println();
      tickerStarted = false;
    }

    public void writeDebug(String s) {
      System.out.println(s);
    }

    public void listItem(String relativePath, FileSyncProcessor.DiffStatus diffStatus) {
      System.out.println(diffStatus.code + "  " + relativePath);
    }
  }

  private static void displayStatistics(FileSyncProcessor.Statistics statistics) {
    if (options.verbosityLevel <= 0) return;
    if (statistics.totalDiffs == 0) {
      System.out.println("No differences found.");
      return;
    }
    String would = options.listOnly ? "that would be " : "";
    if (options.verbosityLevel <= 1) {
      System.out.println("Total differences: " + statistics.totalDiffs);
    } else {
      System.out.println("Files " + would + formatDiffs(statistics.fileDiffs));
      System.out.println("Total file differences: " + statistics.totalFileDiffs);
      System.out.println("Directories " + would + formatDiffs(statistics.directoryDiffs));
      System.out.println("Total directory differences: " + statistics.totalDirectoryDiffs);
    }
    System.out.println("Number of bytes " + would + "copied: " + statistics.bytesCopied);
  }

  private static String formatDiffs(long[] diffs) {
    return "added: "
        + diffs[FileSyncProcessor.DiffStatus.add.ordinal()]
        + ", "
        + "modified: "
        + diffs[FileSyncProcessor.DiffStatus.modify.ordinal()]
        + ", "
        + "renamed: "
        + diffs[FileSyncProcessor.DiffStatus.rename.ordinal()]
        + ", "
        + "deleted: "
        + diffs[FileSyncProcessor.DiffStatus.delete.ordinal()];
  }

  // --- Command-line Processing
  // ---------------------------------------------------------------------

  private static void parseCommandLineArgs(String[] args) {
    CommandLineParser clp = new CommandLineParser(args);
    if (clp.eol()) {
      displayHelpOption = true;
      return;
    }
    while (!clp.eol()) {
      if (clp.isSwitch()) processSwitch(clp);
      else processPositionalParameter(clp);
    }
    if (displayHelpOption) return;
    if (sourcePath == null)
      throw new CommandLineParser.CommandLineException("Missing source path parameter.");
    if (targetPath == null)
      throw new CommandLineParser.CommandLineException("Missing target path parameter.");
  }

  private static void processPositionalParameter(CommandLineParser clp) {
    switch (clp.nextParameterPosition()) {
      case 0:
        sourcePath = Paths.get(clp.nextArg());
        break;
      case 1:
        targetPath = Paths.get(clp.nextArg());
        break;
      default:
        throw new CommandLineParser.CommandLineException(
            "Unexpected extra positional parameter on command line.");
    }
  }

  private static void processSwitch(CommandLineParser clp) {
    if (clp.check("-h")
        || clp.check("-help")
        || clp.check("-?")
        || clp.check("?")
        || clp.check("/?")
        || clp.check("help")) {
      displayHelpOption = true;
    } else if (clp.check("-l") || clp.check("-listOnly")) {
      options.listOnly = true;
    } else if (clp.check("-ignoreCaseAssoc")) {
      options.ignoreCase = true;
    } else if (clp.check("-noIgnoreCaseAssoc")) {
      options.ignoreCase = false;
    } else if (clp.check("-renameCase")) {
      options.renameCase = true;
    } else if (clp.check("-noRenameCase")) {
      options.renameCase = false;
    } else if (clp.check("-timeTolerance")) {
      options.timeTolerance = clp.nextArgInt();
    } else if (clp.check("-summerTimeTolerance")) {
      options.summerTimeTolerance = true;
    } else if (clp.check("-noSummerTimeTolerance")) {
      options.summerTimeTolerance = false;
    } else if (clp.check("-ignoreSystemHiddenFiles")) {
      options.ignoreSystemHiddenFiles = true;
    } else if (clp.check("-noIgnoreSystemHiddenFiles")) {
      options.ignoreSystemHiddenFiles = false;
    } else if (clp.check("-v") || clp.check("-verbosity")) {
      options.verbosityLevel = clp.nextArgInt();
    } else if (clp.check("-debugLevel")) {
      options.debugLevel = clp.nextArgInt();
    } else {
      throw new CommandLineParser.CommandLineException(
          "Unrecognized command-line option \"" + clp.getToken() + "\".");
    }
  }

  private static void displayHelp() {
    FileSyncProcessor.Options defaultOptions = new FileSyncProcessor.Options();
    System.out.println(
        "\n"
            + "FileMirrorSync - A file mirror synchronization tool\n\n"
            + "Usage:\n\n"
            + "  java -Xmx500M -jar filemirrorsync.jar [options] sourcePath targetPath\n\n"
            + "Parameters:\n\n"
            + " sourcePath\n"
            + "    Path of the source directory or source file.\n\n"
            + " targetPath\n"
            + "    Path of the target directory.\n\n"
            + "Options:\n\n"
            + " -l\n"
            + " -listOnly\n"
            + "    Only list the file and directory differences, without copying or changing\n"
            + "    anything.\n\n"
            + " -ignoreCase\n"
            + " -noIgnoreCase\n"
            + "    Specifies whether case should be ignored when associating source\n"
            + "    file/directory names with target file/directory names.\n"
            + "    This is automatically done when one or both of the file systems\n"
            + "    are case-insensitive.\n"
            + "    Default is "
            + (defaultOptions.ignoreCase ? "-ignoreCase" : "-noIgnoreCase")
            + ".\n\n"
            + " -renameCase\n"
            + " -noRenameCase\n"
            + "    Specifies whether file/directory names should be renamed when the names of\n"
            + "    the source and target files/directories only differ in case.\n"
            + "    Default is "
            + (defaultOptions.renameCase ? "-renameCase" : "-noRenameCase")
            + ".\n\n"
            + " -timeTolerance <milliSeconds>\n"
            + "    Tolerance in milliseconds for comparing the last modified time of files.\n"
            + "    This should be set to 1999 (nearly 2 seconds) when mirroring files to a FAT\n"
            + "    file systems from a non-FAT file system, because the FAT file system only\n"
            + "    has a 2 seconds resolution.\n"
            + "    Default is "
            + defaultOptions.timeTolerance
            + ".\n\n"
            + " -summerTimeTolerance\n"
            + " -noSummerTimeTolerance\n"
            + "    Specifies whether time offsets of +/- 1 hour should be ignored.\n"
            + "    Offsets of +/- 1 hour may occur when one copy of the files is stored on a\n"
            + "    file system that uses UTC time (e.g. NTFS), and the other version on a\n"
            + "    file system that uses local time (e.g. FAT).\n"
            + "    Default is "
            + (defaultOptions.summerTimeTolerance
                ? "-summerTimeTolerance"
                : "-noSummerTimeTolerance")
            + ".\n\n"
            + " -ignoreSystemHiddenFiles\n"
            + " -noIgnoreSystemHiddenFiles\n"
            + "    Specifies whether files and directories that have both the system and the\n"
            + "    hidden attributes set should be ignored. When enabled, files and\n"
            + "    directories with these attributes are ignored in the source and in the\n"
            + "    target directory trees.\n"
            + "    Default is "
            + (defaultOptions.ignoreSystemHiddenFiles
                ? "-ignoreSystemHiddenFiles"
                : "noIgnoreSystemHiddenFiles")
            + ".\n\n"
            + " -v <verbosityLevel>\n"
            + " -verbosity <verbosityLevel>\n"
            + "    Verbosity level between 0 and 9.\n"
            + "    Default is "
            + defaultOptions.verbosityLevel
            + "\n\n"
            + " -debugLevel <debugLevel>\n"
            + "    Debug level between 0 and 9.\n"
            + "    Default is "
            + defaultOptions.debugLevel
            + ".\n\n"
            + "Codes used in the output:\n\n"
            + "  "
            + FileSyncProcessor.DiffStatus.add.code
            + "  add to target      - source exists, target does not exist\n"
            + "  "
            + FileSyncProcessor.DiffStatus.modify.code
            + "  modify target      - source and target exist but are different\n"
            + "  "
            + FileSyncProcessor.DiffStatus.rename.code
            + "  rename target      - source and target exist and are equal, but file name\n"
            + "                          upper/lower case characters differ\n"
            + "  "
            + FileSyncProcessor.DiffStatus.delete.code
            + "  delete from target - source does not exist, target exists\n\n"
            + "Author: Christian d'Heureuse, www.source-code.biz");
  }
} // end class FileMirrorSync
