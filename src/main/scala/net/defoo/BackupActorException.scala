package net.defoo

import java.io.File

/**
 * Created by derek on 09/03/14.
 */
sealed case class CannotAccessBackupHomeException(backupHome: File)
  extends Exception(s"Cannot access backupHome ${backupHome.getAbsolutePath}")
