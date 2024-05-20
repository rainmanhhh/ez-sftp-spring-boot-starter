package ez.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import java.nio.file.Path

interface ISftpService {
  /**
   * 设置id格式. 默认情况下id格式使用[SftpAutoConfig]中配置的值，可以通过此方法设置自定义值（仅对当前[ISftpService]实例有效）
   * @param idLevels 层数
   * @param digitsPerLevel 每层位数
   */
  fun setIdFormat(idLevels: Int, digitsPerLevel: Int): ISftpService

  /**
   * 将id格式设置为（1层，18位），适用于id范围较小的情况
   */
  fun setIdFormat() = setIdFormat(1, 18)

  /**
   * 切换工作目录（除非[relativePath]为空，否则会返回一个新的[SftpService]实例）
   * @param relativePath 相对路径。默认为空字符串，表示保持现有目录不变
   */
  fun cd(relativePath: String = ""): ISftpService

  /**
   * 根据数据ID生成子目录（相对路径），再切换到此目录。
   * - 例如，[id]为123000789，层数为4，每层位数为3，则生成的子目录为`000/123/000/789`（相对于当前工作目录）
   */
  fun cd(id: Long): ISftpService

  /**
   * 执行SFTP操作
   * @param action 待执行的SFTP操作
   * @return 执行结果
   */
  fun <T> exec(action: (ChannelSftp) -> T): T?

  /**
   * 上传文件
   * @param localPath 本地文件路径
   * @param remoteFileName 远程文件名（可选）。如果为空，则使用本地文件名（临时文件会使用[getRealName]处理）
   */
  fun upload(localPath: Path, remoteFileName: String? = null)

  /**
   * 下载文件
   * @param remoteFileName 远程文件名
   * @param localPath 本地文件路径（可选）。如果为空，则下载到临时文件夹
   */
  fun download(remoteFileName: String, localPath: Path? = null): Path

  /**
   * 列出目录下的文件
   * @param path 目录路径（可选）。默认为空字符串，表示当前目录
   */
  fun ls(path: String = ".") = exec { it.ls(path) }!!.map { it as LsEntry }
}