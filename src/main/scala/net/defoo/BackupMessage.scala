package net.defoo

import java.io.File
import org.apache.commons.io.filefilter.IOFileFilter
import java.util.UUID

/**
 * Created by derek on 23/02/14.
 */

sealed case class BackupJob(jobId: String, src: File, backupHomeDir: File, fileFilter: IOFileFilter, dirFilter: IOFileFilter, actualCopy: Boolean = false, recurring: Boolean = false)

sealed case class BackupFile(jobId: String, src: File, dest: File, lastBackup: Option[File])

sealed case class BackupFileComplete(jobId: String, src: File, dest: File, srcChecksum: Option[Long] = None, backupChecksum: Option[Long] = None, lastBackupChecksum: Option[Long] = None)



