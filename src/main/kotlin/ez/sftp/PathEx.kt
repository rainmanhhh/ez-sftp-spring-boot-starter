package ez.sftp

import java.nio.file.Path
import java.util.regex.Pattern

private val TMP_FILE_PATTERN = Pattern.compile("^(.+)(\\.[^.]+\\.tmp)$")

/**
 * Get the real name of a file, if it is a temporary file.
 * - If the file is in the temporary directory, and its name matches [TMP_FILE_PATTERN], it will be treated as a temporary file
 * - The first part of the file name (before the first dot) will be returned as the real name
 */
fun Path.getRealName(): String {
  val tmpDir = Path.of(System.getProperty("java.io.tmpdir"))
  if (startsWith(tmpDir)) {
    val fileName = fileName.toString()
    val matcher = TMP_FILE_PATTERN.matcher(fileName)
    if (matcher.matches()) {
      return matcher.group(1)
    }
  }
  return fileName.toString()
}
