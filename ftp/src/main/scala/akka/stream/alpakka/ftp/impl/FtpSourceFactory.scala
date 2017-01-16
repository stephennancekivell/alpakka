/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp.impl

import akka.stream.alpakka.ftp.FtpCredentials.{AnonFtpCredentials, NonAnonFtpCredentials}
import akka.stream.alpakka.ftp.{FtpFileSettings, RemoteFileSettings}
import akka.stream.alpakka.ftp.RemoteFileSettings._
import com.jcraft.jsch.JSch
import org.apache.commons.net.ftp.FTPClient
import java.net.InetAddress
import java.nio.file.Path

private[ftp] trait FtpSourceFactory[FtpClient] { self =>

  type S <: RemoteFileSettings

  protected[this] final val DefaultChunkSize = 8192

  protected[this] def ftpClient: () => FtpClient

  protected[this] def ftpBrowserSourceName: String

  protected[this] def ftpIOSourceName: String

  protected[this] def createBrowserGraph(
      _basePath: String,
      _connectionSettings: S
  )(implicit _ftpLike: FtpLike[FtpClient, S]): FtpBrowserGraphStage[FtpClient, S] =
    new FtpBrowserGraphStage[FtpClient, S] {
      lazy val name: String = ftpBrowserSourceName
      val basePath: String = _basePath
      val connectionSettings: S = _connectionSettings
      val ftpClient: () => FtpClient = self.ftpClient
      val ftpLike: FtpLike[FtpClient, S] = _ftpLike
    }

  protected[this] def createIOGraph(
      _path: Path,
      _connectionSettings: S,
      _chunkSize: Int
  )(implicit _ftpLike: FtpLike[FtpClient, S]): FtpIOGraphStage[FtpClient, S] =
    new FtpIOGraphStage[FtpClient, S] {
      lazy val name: String = ftpIOSourceName
      val path: Path = _path
      val connectionSettings: S = _connectionSettings
      val ftpClient: () => FtpClient = self.ftpClient
      val ftpLike: FtpLike[FtpClient, S] = _ftpLike
      val chunkSize: Int = _chunkSize
    }

  protected[this] def defaultSettings(
      hostname: String,
      username: Option[String] = None,
      password: Option[String] = None
  ): S
}

private[ftp] trait FtpSource extends FtpSourceFactory[FTPClient] {
  protected final val FtpBrowserSourceName = "FtpBrowserSource"
  protected final val FtpIOSourceName = "FtpIOSource"
  protected val ftpClient: () => FTPClient = () => new FTPClient
  protected val ftpBrowserSourceName: String = FtpBrowserSourceName
  protected val ftpIOSourceName: String = FtpIOSourceName
}

private[ftp] trait FtpsSource extends FtpSourceFactory[FTPClient] {
  protected final val FtpsBrowserSourceName = "FtpsBrowserSource"
  protected final val FtpsIOSourceName = "FtpsIOSource"
  protected val ftpClient: () => FTPClient = () => new FTPClient
  protected val ftpBrowserSourceName: String = FtpsBrowserSourceName
  protected val ftpIOSourceName: String = FtpsIOSourceName
}

private[ftp] trait SftpSource extends FtpSourceFactory[JSch] {
  protected final val sFtpBrowserSourceName = "sFtpBrowserSource"
  protected final val sFtpIOSourceName = "sFtpIOSource"
  protected val ftpClient: () => JSch = () => new JSch
  protected val ftpBrowserSourceName: String = sFtpBrowserSourceName
  protected val ftpIOSourceName: String = sFtpIOSourceName
}

private[ftp] trait FtpDefaultSettings {
  protected def defaultSettings(
      hostname: String,
      username: Option[String],
      password: Option[String]
  ): FtpSettings =
    FtpSettings(
      InetAddress.getByName(hostname),
      DefaultFtpPort,
      if (username.isDefined)
        NonAnonFtpCredentials(username.get, password.getOrElse(""))
      else
        AnonFtpCredentials
    )
}

private[ftp] trait FtpsDefaultSettings {
  protected def defaultSettings(
      hostname: String,
      username: Option[String],
      password: Option[String]
  ): FtpsSettings =
    FtpsSettings(
      InetAddress.getByName(hostname),
      DefaultFtpsPort,
      if (username.isDefined)
        NonAnonFtpCredentials(username.get, password.getOrElse(""))
      else
        AnonFtpCredentials
    )
}

private[ftp] trait SftpDefaultSettings {
  protected def defaultSettings(
      hostname: String,
      username: Option[String],
      password: Option[String]
  ): SftpSettings =
    SftpSettings(
      InetAddress.getByName(hostname),
      DefaultSftpPort,
      if (username.isDefined)
        NonAnonFtpCredentials(username.get, password.getOrElse(""))
      else
        AnonFtpCredentials
    )
}

private[ftp] trait FtpSourceParams extends FtpSource with FtpDefaultSettings {
  type S = FtpFileSettings
  protected[this] val ftpLike: FtpLike[FTPClient, S] = FtpLike.ftpLikeInstance
}

private[ftp] trait FtpsSourceParams extends FtpsSource with FtpsDefaultSettings {
  type S = FtpFileSettings
  protected[this] val ftpLike: FtpLike[FTPClient, S] = FtpLike.ftpLikeInstance
}

private[ftp] trait SftpSourceParams extends SftpSource with SftpDefaultSettings {
  type S = SftpSettings
  protected[this] val ftpLike: FtpLike[JSch, S] = FtpLike.sFtpLikeInstance
}
