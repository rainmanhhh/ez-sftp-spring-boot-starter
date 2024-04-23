package ez.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import io.github.hligaty.haibaracp.core.SftpSession
import io.github.hligaty.haibaracp.core.SftpTemplate
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class SftpService private constructor(
  private val config: SftpAutoConfig,
  /**
   * 远程目录。必须以`/`开头，不能以`/`结尾
   */
  private val remoteDir: String,
  private val sftpTemplate: SftpTemplate<SftpSession>
) {
  init {
    ensureRemoteDirectoryExists(remoteDir)
  }

  constructor(config: SftpAutoConfig, sftpTemplate: SftpTemplate<SftpSession>) :
    this(config, config.remoteDir.removeSuffix("/"), sftpTemplate)

  /**
   * 切换工作目录（除非[relativePath]为空，否则会返回一个新的[SftpService]实例）
   * @param relativePath 相对路径。默认为空字符串，表示保持现有目录不变
   */
  fun cd(relativePath: String = ""): SftpService {
    if (relativePath.isEmpty()) return this
    val remoteDir = "$remoteDir/$relativePath".removeSuffix("/")
    return SftpService(config, remoteDir, sftpTemplate)
  }

  /**
   * 根据数据ID生成子目录（相对路径），再切换到此目录。
   * - 例如，[id]为123000789，层数为4，每层位数为3，则生成的子目录为`000/123/000/789`（相对于当前工作目录）
   */
  fun cd(id: Long): SftpService {
    val relativePath = idToPath(id, config.idLevels, config.digitsPerLevel)
    return cd(relativePath)
  }

  fun <T> exec(action: (ChannelSftp) -> T) = sftpTemplate.execute {
    it.cd(remoteDir)
    action(it)
  }

  fun upload(localPath: Path, remoteFileName: String? = null) {
    val finalRemoteFileName = remoteFileName ?: localPath.getRealName()
    exec {
      it.put(localPath.toAbsolutePath().toString(), finalRemoteFileName)
    }
  }

  fun download(remoteFileName: String, localPath: Path? = null): Path {
    val finalLocalPath = localPath ?: Files.createTempFile("${remoteFileName}.", null)!!
    try {
      exec {
        it.get(remoteFileName, finalLocalPath.toAbsolutePath().toString())
      }
    } catch (e: Exception) {
      logger.error("download file failed: {}/{}", remoteDir, remoteFileName)
      throw e
    }
    return finalLocalPath
  }

  fun ls(path: String = ".") = exec { it.ls(path) }!!.map { it as LsEntry }

  private fun ensureRemoteDirectoryExists(remoteDirPath: String) {
    val dirs = remoteDirPath.split('/').filter { it.isNotEmpty() } // 分割路径并去除空字符串
    var currentPath = if (remoteDirPath.startsWith("/")) "/" else ""
    sftpTemplate.executeWithoutResult {
      for (dir in dirs) {
        currentPath += "$dir/"
        if (!sftpTemplate.exists(currentPath)) {
          // 使用 executeWithoutResult 执行创建目录的命令
          try {
            it.mkdir(currentPath)
          } catch (e: Exception) {
            throw RuntimeException("创建远程目录失败：[$currentPath]", e)
          }
        }
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(SftpService::class.java)
  }
}
