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

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class FileSyncProcessor {

  public static class Options {
    public boolean listOnly;
    // List only, without changing anything in the target directory tree.
    public boolean ignoreCase;
    // Ignore case when associating source file/directory names with target file/directory names.
    // This is automatically done when one or both of the file systems are case-insensitive.
    public boolean renameCase = true;
    // True to rename target files/directories when the names of source and target only differ in
    // case.
    public int timeTolerance = 1999; // in milliseconds
    // Tolerance in milliseconds for comparing the last modified time of files.
    // We assume 1.999 seconds, because the FAT file system only has 2 seconds resolution.
    public boolean summerTimeTolerance = true;
    // True to ignore time offsets of +/- 1 hour.
    // Offsets of +/- 1 hour may occur when one copy of the files is stored on a file system
    // that uses UTC time (e.g. NTFS), and the other version on a file system that uses local
    // time (e.g. FAT).
    public boolean ignoreSystemHiddenFiles = true;
    // True to ignore files and directories that have both the system and the hidden
    // attributes set. When enabled, files and directories with these attributes are ignored in the
    // source and in the target directory trees.
    public int verbosityLevel = 5; // 0..9
    public int debugLevel;
  } // 0..9

  public enum DiffStatus {
    add('A'), // add to target      - source exists, target does not exist
    modify('M'), // modify target      - source and target exist but are different
    rename('R'), // rename target      - source and target exist and are equal, but file name
    // upper/lower case characters differ
    delete('D'); // delete from target - source does not exist, target exists
    public final char code;

    DiffStatus(char code) {
      this.code = code;
    }
  }

  public static class Statistics {
    public long[] fileDiffs = new long[DiffStatus.values().length];
    public long totalFileDiffs;
    public long[] directoryDiffs = new long[DiffStatus.values().length];
    public long totalDirectoryDiffs;
    public long totalDiffs;
    public long bytesCopied;
  }

  public static interface UserInterface {
    void writeInfo(String s);

    void writeInfoTicker();

    void endInfoTicker();

    void writeDebug(String s);

    void listItem(String relativePath, DiffStatus diffStatus);
  }

  private class SyncItem implements Comparable<SyncItem> {
    DiffStatus diffStatus; // null=equal
    String key;
    boolean sourceExists;
    boolean targetExists;
    String sourceRelativePath;
    String targetRelativePath;
    String sourceName;
    String targetName;
    boolean sourceIsDirectory;
    boolean targetIsDirectory;
    long sourceFileSize;
    long targetFileSize;
    long sourceLastModifiedTime; // in milliseconds
    long targetLastModifiedTime; // in milliseconds

    @Override
    public int compareTo(SyncItem o) {
      return key.compareTo(o.key);
    }

    public String getRelativePath() {
      return (sourceRelativePath != null) ? sourceRelativePath : targetRelativePath;
    }

    public Path getSourcePath() {
      return sourceBaseDir.resolve(sourceRelativePath);
    }

    public Path getOldTargetPath() {
      return targetBaseDir.resolve(targetRelativePath);
    }

    public Path getNewTargetPath() {
      return targetBaseDir.resolve(sourceRelativePath);
    }
  }

  private static final String keyPathSeparator =
      "\u0000"; // path separator used for the internal key string
  private static final long oneHourInMillis = 3600000;
  private static final long mirrorTickerInterval = 1000;

  private Options options;
  private UserInterface ui;
  private Path sourcePathAbs;
  private Path targetPathAbs;
  private FileSystem sourceFileSystem;
  private FileSystem targetFileSystem;
  private Path sourceBaseDir;
  private Path targetBaseDir;
  private String sourcePathSeparator;
  private String targetPathSeparator;
  private boolean sourceFileSystemIsCaseSensitive;
  private boolean targetFileSystemIsCaseSensitive;
  private boolean caseSensitive;
  private HashMap<String, SyncItem> itemMap; // maps key strings to SyncItems
  private ArrayList<SyncItem> itemList; // items sorted by key
  private int itemListPos; // current position in itemList
  private long mirrorTickerCounter;
  private Statistics statistics;

  // --- Main ---------------------------------------------------------------------

  public void main(
      Path sourcePath, Path targetPath, Options options, UserInterface ui, Statistics statistics)
      throws Exception {
    this.sourcePathAbs = sourcePath.toAbsolutePath().normalize();
    this.targetPathAbs = targetPath.toAbsolutePath().normalize();
    this.options = options;
    this.ui = ui;
    this.statistics = statistics;
    init();
    readDirectoryTrees();
    if (!compareFiles()) {
      return;
    }
    if (options.listOnly) {
      listFiles();
    } else {
      mirrorFiles();
    }
  }

  private void init() throws Exception {
    if (!Files.exists(sourcePathAbs)) throw new Exception("The source path does not exist.");
    if (sourcePathAbs.equals(targetPathAbs))
      throw new Exception("Source and target paths are equal.");
    BasicFileAttributes sourcePathAttrs =
        Files.readAttributes(sourcePathAbs, BasicFileAttributes.class);
    if (sourcePathAttrs.isDirectory()) {
      sourceBaseDir = sourcePathAbs;
    } else { // source path is file
      sourceBaseDir = sourcePathAbs.getParent();
    }
    BasicFileAttributes targetPathAttrs =
        !Files.exists(targetPathAbs)
            ? null
            : Files.readAttributes(targetPathAbs, BasicFileAttributes.class);
    if (targetPathAttrs != null && !targetPathAttrs.isDirectory()) {
      throw new Exception("Target path exists and is not a directory.");
    }
    targetBaseDir = targetPathAbs;
    sourceFileSystem = sourcePathAbs.getFileSystem();
    targetFileSystem = targetPathAbs.getFileSystem();
    sourceFileSystemIsCaseSensitive = isFileSystemCaseSensitive(sourceFileSystem);
    targetFileSystemIsCaseSensitive = isFileSystemCaseSensitive(targetFileSystem);
    caseSensitive =
        !options.ignoreCase && sourceFileSystemIsCaseSensitive && targetFileSystemIsCaseSensitive;
    // Unless boths file systems are case-sensitive, we use case-insensitive comparisons for file
    // names.
    sourcePathSeparator = sourceFileSystem.getSeparator();
    targetPathSeparator = targetFileSystem.getSeparator();
  }

  private static boolean isFileSystemCaseSensitive(FileSystem fileSystem) {
    return !fileSystem.getPath("a").equals(fileSystem.getPath("A"));
  }

  // --- Read directory trees -----------------------------------------------------

  private void readDirectoryTrees() throws Exception {
    itemMap = new HashMap<String, SyncItem>(4096);
    readSourceDirectoryTree();
    readTargetDirectoryTree();
    itemList = new ArrayList<SyncItem>(itemMap.values());
    Collections.sort(itemList);
    itemMap = null;
  }

  private void readSourceDirectoryTree() throws Exception {
    if (options.verbosityLevel >= 1) ui.writeInfo("Reading source directory.");
    FileSyncFileVisitor fileVisitor = new FileSyncFileVisitor();
    fileVisitor.isSource = true;
    Files.walkFileTree(sourcePathAbs, fileVisitor);
    if (options.verbosityLevel >= 2) ui.endInfoTicker();
  }

  private void readTargetDirectoryTree() throws Exception {
    if (options.verbosityLevel >= 1) ui.writeInfo("Reading target directory.");
    if (!Files.exists(targetPathAbs)) return;
    FileSyncFileVisitor fileVisitor = new FileSyncFileVisitor();
    fileVisitor.isSource = false;
    Files.walkFileTree(targetPathAbs, fileVisitor);
    if (options.verbosityLevel >= 2) ui.endInfoTicker();
  }

  private class FileSyncFileVisitor extends SimpleFileVisitor<Path> {
    private static final long tickerInterval = 10000;
    boolean isSource; // true=processing source tree, false=processing target tree
    long tickerCounter;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      if (options.debugLevel >= 9) ui.writeDebug("preVisitDirectory: " + dir);
      ticker();
      if (!isPathIncluded(dir, isSource)) {
        if (options.debugLevel >= 6) System.out.println("Directory excluded: \"" + dir + "\".");
        return FileVisitResult.SKIP_SUBTREE;
      }
      registerSyncItem(dir, attrs, true, isSource);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      ticker();
      if (options.debugLevel >= 9) ui.writeDebug("visitFile: " + file);
      if (!isPathIncluded(file, isSource)) {
        if (options.debugLevel >= 7) ui.writeDebug("File excluded: \"" + file + "\".");
        return FileVisitResult.CONTINUE;
      }
      registerSyncItem(file, attrs, false, isSource);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
      ticker();
      if (options.debugLevel >= 9)
        ui.writeDebug("VisitFileFailed for \"" + file + "\", exception=" + e);
      // Problem: Under Windows, an AccessDeniedException occurs for "\System Volume Information"
      // before
      // preVisitDirectory() is called. We solve this by ignoring visitFileFailed for
      // files/directories that
      // are not included.
      if (!isPathIncluded(file, isSource)) {
        return FileVisitResult.CONTINUE;
      }
      throw e;
    }

    private final void ticker() {
      if (options.verbosityLevel >= 2) {
        if (++tickerCounter % tickerInterval == 0) {
          ui.writeInfoTicker();
        }
      }
    }
  }

  private void registerSyncItem(
      Path path, BasicFileAttributes attrs, boolean isDirectory, boolean isSource)
      throws IOException {
    String relativePath = genRelativePath(path, isSource);
    if (relativePath == null) return; // ignore base directories
    String key = genSyncItemKey(relativePath, isSource);
    SyncItem item = itemMap.get(key);
    if (item == null) {
      item = new SyncItem();
      item.key = key;
      itemMap.put(key, item);
    }
    if (isSource) {
      if (item.sourceExists) {
        throw new IOException(
            "File name collision in source directory tree, "
                + "path1=\""
                + item.sourceRelativePath
                + "\", path2=\""
                + relativePath
                + "\".");
      }
      item.sourceExists = true;
      item.sourceRelativePath = relativePath;
      item.sourceName = path.getFileName().toString();
      item.sourceIsDirectory = isDirectory;
      if (!isDirectory) {
        item.sourceFileSize = attrs.size();
        item.sourceLastModifiedTime = attrs.lastModifiedTime().toMillis();
      }
    } else {
      if (item.targetExists) {
        throw new IOException(
            "File name collision in target directory tree, "
                + "path1=\""
                + item.targetRelativePath
                + "\", path2=\""
                + relativePath
                + "\".");
      }
      item.targetExists = true;
      item.targetRelativePath = relativePath;
      item.targetName = path.getFileName().toString();
      item.targetIsDirectory = isDirectory;
      if (!isDirectory) {
        item.targetFileSize = attrs.size();
        item.targetLastModifiedTime = attrs.lastModifiedTime().toMillis();
      }
    }
  }

  private String genRelativePath(Path path, boolean isSource) {
    Path relPath;
    if (isSource) {
      relPath = sourceBaseDir.relativize(path);
    } else {
      relPath = targetBaseDir.relativize(path);
    }
    if (relPath == null || relPath.toString().length() == 0) return null;
    return relPath.toString();
  }

  private String genSyncItemKey(String relativePath, boolean isSource) {
    String s = relativePath;
    if (!caseSensitive) {
      s = s.toUpperCase();
    }
    if (isSource) {
      s = fastReplace(s, sourcePathSeparator, keyPathSeparator);
    } else {
      s = fastReplace(s, targetPathSeparator, keyPathSeparator);
    }
    return s;
  }

  private boolean isPathIncluded(Path path, boolean isSource) throws IOException {
    // Under Windows, the root directory of a drive (e.g. "c:\") has the system and hidden
    // attributes set.
    // We solve this problem by always including the root source and target directories.
    if (isSource) {
      if (path.equals(sourcePathAbs)) return true;
    } else {
      if (path.equals(targetPathAbs)) return true;
    }
    if (options.ignoreSystemHiddenFiles) {
      DosFileAttributes dosAttrs = getDosFileAttributes(path);
      if (dosAttrs != null && dosAttrs.isSystem() && dosAttrs.isHidden()) {
        return false;
      }
    }
    return true;
  }

  private DosFileAttributes getDosFileAttributes(Path path) throws IOException {
    try {
      return Files.readAttributes(path, DosFileAttributes.class);
    } catch (UnsupportedOperationException e) {
      return null;
    }
  }

  // --- Compare files ------------------------------------------------------------

  private boolean compareFiles() {
    if (options.debugLevel >= 2) ui.writeDebug("Comparing directory entries.");
    boolean differencesFound = false;
    for (SyncItem item : itemList) {
      item.diffStatus = compareItem(item);
      if (item.diffStatus != null) differencesFound = true;
    }
    return differencesFound;
  }

  // Returns null if source and target are equal.
  private DiffStatus compareItem(SyncItem item) {
    if (!item.targetExists) return DiffStatus.add;
    if (!item.sourceExists) return DiffStatus.delete;
    if (item.sourceIsDirectory != item.targetIsDirectory) return DiffStatus.modify;
    boolean isDirectory = item.sourceIsDirectory;
    if (!isDirectory) {
      if (item.sourceFileSize != item.targetFileSize) return DiffStatus.modify;
      long timeDiff = Math.abs(item.sourceLastModifiedTime - item.targetLastModifiedTime);
      if (options.summerTimeTolerance && timeDiff >= oneHourInMillis) {
        timeDiff = timeDiff - oneHourInMillis;
      }
      if (timeDiff > options.timeTolerance) return DiffStatus.modify;
    }
    if (options.renameCase && !item.sourceName.equals(item.targetName)) return DiffStatus.rename;
    return null;
  }

  // --- List files ---------------------------------------------------------------

  private void listFiles() {
    for (SyncItem item : itemList) {
      if (item.diffStatus != null && options.verbosityLevel >= 5) {
        listItem(item);
      }
      updateStatistics(item);
    }
  }

  private void listItem(SyncItem item) {
    ui.listItem(item.getRelativePath(), item.diffStatus);
  }

  // --- Mirror copy files --------------------------------------------------------

  private void mirrorFiles() throws Exception {
    mirrorTickerCounter = 0;
    if (options.verbosityLevel >= 1) ui.writeInfo("Transferring files.");
    createTargetBaseDirectory();
    itemListPos = 0;
    while (itemListPos < itemList.size()) {
      SyncItem item = itemList.get(itemListPos++);
      mirrorItem(item);
    }
  }

  private void mirrorItem(SyncItem item) throws Exception {
    if (item.diffStatus == null) return;
    if (options.verbosityLevel >= 5) {
      listItem(item);
    } else if (options.verbosityLevel >= 2) {
      if (++mirrorTickerCounter % mirrorTickerInterval == 0) ui.writeInfoTicker();
    }
    switch (item.diffStatus) {
      case add:
        addItem(item);
        break;
      case modify:
        modifyItem(item);
        break;
      case rename:
        renameItem(item);
        break;
      case delete:
        deleteItem(item);
        break;
      default:
        throw new AssertionError();
    }
    updateStatistics(item);
  }

  private void addItem(SyncItem item) throws Exception {
    Path sourcePath = item.getSourcePath();
    Path targetPath = item.getNewTargetPath();
    if (item.sourceIsDirectory) {
      if (options.debugLevel >= 3) ui.writeDebug("Creating directory \"" + targetPath + "\".");
      copyEmptyDirectory(sourcePath, targetPath);
    } else {
      if (options.debugLevel >= 3)
        ui.writeDebug("Copying file \"" + sourcePath + "\" to \"" + targetPath + "\".");
      copyFile(sourcePath, targetPath);
    }
  }

  private void copyEmptyDirectory(Path sourcePath, Path targetPath) throws Exception {
    Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
  }

  private void copyFile(Path sourcePath, Path targetPath) throws Exception {
    // To prevent a partially copied file when the program is interrupted,
    // we first copy to a temporary file and then rename the copied file.
    Path tempPath = targetPath.getParent().resolve("$$tempFileMirrorSyncOutputFile$$.tmp");
    Files.copy(
        sourcePath,
        tempPath,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
  }

  private void deleteItem(SyncItem item) throws Exception {
    if (item.targetIsDirectory) {
      deleteDirectoryContents(item.key);
    } // (recoursive)
    Path path = item.getOldTargetPath();
    if (options.debugLevel >= 3) ui.writeDebug("Deleting \"" + path + "\".");
    deletePath(path);
  }

  private void deletePath(Path path) throws Exception {
    try {
      Files.delete(path);
    } catch (AccessDeniedException e) {
      if (!tryToResetReadOnlyAttribute(path)) throw e;
      Files.delete(path);
    }
  }

  private boolean tryToResetReadOnlyAttribute(Path path) {
    try {
      DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
      if (view == null) return false;
      if (!view.readAttributes().isReadOnly()) return false;
      view.setReadOnly(false);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  // Deletes the contents of a directory.
  private void deleteDirectoryContents(String key) throws Exception {
    String keyStart = key + keyPathSeparator;
    while (itemListPos < itemList.size()) {
      SyncItem item = itemList.get(itemListPos);
      if (!item.key.startsWith(keyStart)) break; // end of directory reached
      itemListPos++;
      if (item.diffStatus != DiffStatus.delete) {
        throw new Exception(
            "Unexpected file/directory found in directory that should be deleted: \""
                + item.getRelativePath()
                + "\".");
      }
      mirrorItem(item);
    }
  }

  private void modifyItem(SyncItem item) throws Exception {
    deleteItem(item);
    addItem(item);
  }

  private void renameItem(SyncItem item) throws Exception {
    Path oldPath = item.getOldTargetPath();
    Path newPath = item.getNewTargetPath();
    if (options.debugLevel >= 3)
      ui.writeDebug("Renaming \"" + oldPath + "\" to \"" + newPath + "\".");
    Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
    // Without the ATOMIC_MOVE option, the file is not renamed in Windows, because the old and the
    // new
    // file name differ only in upper/lower case.
    if (item.targetIsDirectory) {
      fixupTargetPaths(item.key, item.targetRelativePath, item.sourceRelativePath);
    }
  }

  // When a target directory is renamed, we fix up the relative target path strings of all files
  // and subdirectories contained in that directory.
  private void fixupTargetPaths(String key, String oldPath, String newPath) {
    String keyStart = key + keyPathSeparator;
    String oldPathStart = oldPath + targetPathSeparator;
    String newPathStart =
        fastReplace(newPath, sourcePathSeparator, targetPathSeparator) + targetPathSeparator;
    int i = itemListPos;
    while (i < itemList.size()) {
      SyncItem item = itemList.get(i++);
      if (!item.key.startsWith(keyStart)) break; // end of directory reached
      if (!item.targetExists) continue;
      if (item.targetRelativePath.startsWith(oldPathStart)) {
        String s = newPathStart + item.targetRelativePath.substring(oldPathStart.length());
        if (options.debugLevel >= 9)
          ui.writeDebug(
              "Fixup target path: from \"" + item.targetRelativePath + "\" to \"" + s + "\".");
        item.targetRelativePath = s;
      }
    }
  }

  private void createTargetBaseDirectory() throws Exception {
    if (Files.exists(targetBaseDir)) return;
    if (options.debugLevel >= 3)
      ui.writeDebug("Creating target base directory \"" + targetBaseDir + "\".");
    Files.createDirectories(targetBaseDir);
  }

  // --- Statistics ---------------------------------------------------------------

  private void updateStatistics(SyncItem item) {
    if (item.diffStatus == null) {
      return;
    }
    boolean isDirectory = item.sourceExists ? item.sourceIsDirectory : item.targetIsDirectory;
    if (isDirectory) {
      statistics.directoryDiffs[item.diffStatus.ordinal()]++;
      statistics.totalDirectoryDiffs++;
    } else {
      statistics.fileDiffs[item.diffStatus.ordinal()]++;
      statistics.totalFileDiffs++;
      if (item.diffStatus == DiffStatus.add || item.diffStatus == DiffStatus.modify) {
        statistics.bytesCopied += item.sourceFileSize;
      }
    }
    statistics.totalDiffs++;
  }

  // --- General tools ------------------------------------------------------------

  // String.replace(CharSequence, CharSequence) is slow in JDK7 beta.
  private static String fastReplace(String s, String s1, String s2) {
    if (s1.length() != 1 || s2.length() != 1) {
      if (s1.equals(s2)) return s;
      return s.replace(s1, s2);
    }
    char c1 = s1.charAt(0);
    char c2 = s2.charAt(0);
    if (c1 == c2) return s;
    return s.replace(c1, c2);
  }
} // end class FileSyncProcessor
