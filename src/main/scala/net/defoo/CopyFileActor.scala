package net.defoo

import akka.actor.{Props, Actor}
import org.apache.commons.io.FileUtils
import java.nio.file.{Paths, StandardCopyOption, Files}
import java.io.File

/**
 * Created by derek on 23/02/14.
 */
class FileCopyActor extends Actor {


  def receive = {

    case BackupFile(jobId, srcFile, dest, Some(lastBackup)) =>

      srcFile.isDirectory match {
        case true =>
          dest.mkdirs
          dest setLastModified srcFile.lastModified
          sender ! BackupFileComplete(jobId = jobId, src = srcFile, dest = dest)

        case false =>
          val srcChecksum = FileUtils checksumCRC32 srcFile
          val lastBackupChecksum = FileUtils checksumCRC32 lastBackup

          val newBackupChecksum = (srcChecksum != lastBackupChecksum) match {
            case true => //changed
              FileUtils copyFile(srcFile, dest)
              FileUtils checksumCRC32 dest

            case false => //not changed
              createLinkToLastBackup(dest, lastBackup)
              lastBackupChecksum
          }

          assert(srcChecksum == newBackupChecksum)

          sender ! BackupFileComplete(jobId = jobId, src = srcFile, dest = dest, Option(srcChecksum), Option(newBackupChecksum), Option(lastBackupChecksum))
      }


    case BackupFile(jobId, srcFile, dest, None) =>
      srcFile.isDirectory match {
        case true =>
          dest.mkdirs()
          dest.setLastModified(srcFile.lastModified)
          sender ! BackupFileComplete(jobId = jobId, src = srcFile, dest = dest)

        case false =>
          FileUtils.copyFile(srcFile, dest)
          val srcChecksum = FileUtils checksumCRC32 srcFile
          val newBackupChecksum = FileUtils checksumCRC32 dest
          assert(srcChecksum == newBackupChecksum)
          sender ! BackupFileComplete(jobId = jobId, src = srcFile, dest = dest, Option(srcChecksum), Option(newBackupChecksum), None)

      }

  }


  def createLinkToLastBackup(dest: File, lastBackup: File) {
    dest.getParentFile match {
      case parent if parent != null => FileUtils.forceMkdir(parent)
      case _ =>
    }

    Files.createLink(dest.toPath, lastBackup.toPath)
  }
}

