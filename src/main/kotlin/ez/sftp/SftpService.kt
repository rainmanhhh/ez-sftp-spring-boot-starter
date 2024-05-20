package ez.sftp

import com.jcraft.jsch.ChannelSftp
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
  private var idLevels: Int,
  private var digitsPerLevel: Int,
  private val sftpTemplate: SftpTemplate<SftpSession>
) : ISftpService {
  init {
    ensureRemoteDirectoryExists(remoteDir)
  }

  constructor(config: SftpAutoConfig, sftpTemplate: SftpTemplate<SftpSession>) :
    this(config, config.remoteDir.removeSuffix("/"), config.idLevels, config.digitsPerLevel, sftpTemplate)

  override fun setIdFormat(idLevels: Int, digitsPerLevel: Int): ISftpService = apply {
    this.idLevels = idLevels
    this.digitsPerLevel = digitsPerLevel
  }

  override fun cd(relativePath: String): SftpService {
    if (relativePath.isEmpty()) return this
    val remoteDir = "$remoteDir/$relativePath".removeSuffix("/")
    return SftpService(config, remoteDir, config.idLevels, config.digitsPerLevel, sftpTemplate)
  }

  override fun cd(id: Long): SftpService {
    val relativePath = idToPath(id, idLevels, digitsPerLevel)
    return cd(relativePath)
  }

  override fun <T> exec(action: (ChannelSftp) -> T) = sftpTemplate.execute {
    it.cd(remoteDir)
    action(it)
  }

  override fun upload(localPath: Path, remoteFileName: String?) {
    val finalRemoteFileName = remoteFileName ?: localPath.getRealName()
    exec {
      it.put(localPath.toAbsolutePath().toString(), finalRemoteFileName)
    }
  }

  override fun download(remoteFileName: String, localPath: Path?): Path {
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
