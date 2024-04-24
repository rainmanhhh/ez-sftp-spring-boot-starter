package ez.sftp

import io.github.hligaty.haibaracp.core.SftpSession
import io.github.hligaty.haibaracp.core.SftpTemplate
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackages = ["ez.sftp"])
@ConfigurationProperties("ez.sftp")
class SftpAutoConfig {
  /**
   * root working dir on sftp server.
   * empty string means use sftp login user's home dir.
   */
  var remoteDir = ""

  /**
   * when building working dir by data id, how many levels to use.
   */
  var idLevels = 4

  /**
   * when building working dir by data id, maximum sub dir name length of each level
   */
  var digitsPerLevel = 3

  @Bean
  fun sftpService(sftpTemplate: SftpTemplate<SftpSession>): ISftpService =
    SftpService(this, sftpTemplate)
}