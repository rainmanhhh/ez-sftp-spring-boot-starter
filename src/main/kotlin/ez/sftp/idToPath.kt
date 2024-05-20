package ez.sftp

/**
 * Convert an ID to a path string based on the specified levels and digitsPerLevel.
 * - eg: idToPath(23456789, 4, 3) returns "000/023/000/789"
 * - special case: if levels is 1, return the ID as a string
 */
fun idToPath(id: Long, levels: Int, digitsPerLevel: Int): String {
  val idStr = id.toString() // eg. "23000789"
  if (levels == 1) return idStr
  val totalLengthWithoutSlash = levels * digitsPerLevel // 4 * 3 = 12 (length of "000023000789")
  val totalLength = totalLengthWithoutSlash + levels // 12 + 4 = 16 (length of "000/023/000/789/")
  val padLength = totalLengthWithoutSlash - idStr.length // number of padding zeros at the start, here is 4
  if (padLength < 0) throw RuntimeException("id is too long for the specified levels and digitsPerLevel: $id")
  val sb = StringBuilder(totalLength)
  var tmp = 0 // temp counter, everytime it reaches digitsPerLevel, add a slash and clear it
  for (i in 0 until padLength) {
    sb.append('0')
    if (++tmp == digitsPerLevel) {
      sb.append('/')
      tmp = 0
    }
  }
  for (element in idStr) {
    sb.append(element)
    if (++tmp == digitsPerLevel) {
      sb.append('/')
      tmp = 0
    }
  }
  return sb.substring(0, sb.length - 1) // remove the last slash. return value is "000/023/000/789"
}
