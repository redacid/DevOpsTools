package ua.`in`.ios.devopstools

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

/**
 * Class for storing information about the operating system and its capabilities
 */
class SystemInfo private constructor() {
    // Basic system properties
    var osName: String = ""
    var osVersion: String = ""
    var osArch: String = ""
    var osFamily: OsFamily = OsFamily.UNKNOWN
    var distribution: String = ""

    // Supported installation types
    var supportsDeb: Boolean = false
    var supportsRpm: Boolean = false
    var supportsDmg: Boolean = false
    var supportsExe: Boolean = false
    var supportsMsi: Boolean = false
    var supportsAppImage: Boolean = false
    var supportsSnapcraft: Boolean = false
    var supportsFlatpak: Boolean = false

    // Additional information
    var userHome: String = ""
    var userName: String = ""
    var totalMemory: Long = 0 // In bytes
    var freeMemory: Long = 0 // In bytes
    var availableProcessors: Int = 0
    var cpuModel: String = ""

    // OS family types
    enum class OsFamily {
        WINDOWS,
        LINUX,
        MACOS,
        UNIX,
        UNKNOWN
    }

    // Path to package cache
    var packageCachePath: String = ""

    /**
     * Returns the default package manager type for the current system
     */
    fun getDefaultPackageManagerType(): String {
        return when {
            supportsDeb -> "deb"
            supportsRpm -> "rpm"
            supportsDmg -> "dmg"
            supportsExe || supportsMsi -> "windows"
            supportsAppImage -> "appimage"
            supportsSnapcraft -> "snap"
            supportsFlatpak -> "flatpak"
            else -> "unknown"
        }
    }

    /**
     * Formats memory size in a user-friendly format
     */
    fun formatMemorySize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format("%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }

    /**
     * Returns a string with summary system information
     */
    override fun toString(): String {
        return """
            Operating System: $osName
            OS Version: $osVersion
            Architecture: $osArch
            OS Family: $osFamily
            Distribution: $distribution
            User: $userName
            Home Directory: $userHome
            Total Memory: ${formatMemorySize(totalMemory)}
            Free Memory: ${formatMemorySize(freeMemory)}
            Processors: $availableProcessors
            CPU Model: $cpuModel
            
            Supported Installation Types:
            - DEB: $supportsDeb
            - RPM: $supportsRpm
            - DMG: $supportsDmg
            - EXE: $supportsExe
            - MSI: $supportsMsi
            - AppImage: $supportsAppImage
            - Snapcraft: $supportsSnapcraft
            - Flatpak: $supportsFlatpak
            
            Default Package Manager Type: ${getDefaultPackageManagerType()}
            Package Cache Path: $packageCachePath
        """.trimIndent()
    }

    companion object {
        @Volatile
        private var instance: SystemInfo? = null

        fun getInstance(): SystemInfo {
            return instance ?: synchronized(this) {
                instance ?: createInstance().also { instance = it }
            }
        }

        private fun createInstance(): SystemInfo {
            val systemInfo = SystemInfo()
            detectSystemInfo(systemInfo)
            return systemInfo
        }

        /**
         * Function for detecting system information
         */
        private fun detectSystemInfo(info: SystemInfo) {
            // Get basic information from Java system properties
            info.osName = System.getProperty("os.name", "")
            info.osVersion = System.getProperty("os.version", "")
            info.osArch = System.getProperty("os.arch", "")
            info.userHome = System.getProperty("user.home", "")
            info.userName = System.getProperty("user.name", "")

            // Determine system resources
            val runtime = Runtime.getRuntime()
            info.availableProcessors = runtime.availableProcessors()

            // Determine system memory using OperatingSystemMXBean
            try {
                val osMxBean = ManagementFactory.getOperatingSystemMXBean()
                if (osMxBean is com.sun.management.OperatingSystemMXBean) {
                    info.totalMemory = osMxBean.getTotalMemorySize()
                    info.freeMemory = osMxBean.getFreeMemorySize()
                } else {
                    // Fallback
                    detectMemoryAlternative(info)
                }
            } catch (e: Exception) {
                // If an error occurred, use alternative method
                detectMemoryAlternative(info)
            }

            // Determine OS family and set appropriate flags
            when {
                info.osName.lowercase().contains("linux") -> {
                    info.osFamily = OsFamily.LINUX
                    detectLinuxDistribution(info)
                    detectLinuxPackageManagers(info)
                    info.packageCachePath = "${info.userHome}/.devopstools/packages"
                }
                info.osName.lowercase().contains("windows") -> {
                    info.osFamily = OsFamily.WINDOWS
                    info.supportsExe = true
                    info.supportsMsi = true
                    info.packageCachePath = "${info.userHome}\\AppData\\Local\\DevOpsTools\\packages"
                }
                info.osName.lowercase().contains("mac") -> {
                    info.osFamily = OsFamily.MACOS
                    info.supportsDmg = true
                    info.packageCachePath = "${info.userHome}/Library/Caches/DevOpsTools/packages"
                }
                info.osName.lowercase().contains("solaris") ||
                        info.osName.lowercase().contains("sunos") ||
                        info.osName.lowercase().contains("freebsd") -> {
                    info.osFamily = OsFamily.UNIX
                    info.packageCachePath = "${info.userHome}/.devopstools/packages"
                }
                else -> {
                    info.osFamily = OsFamily.UNKNOWN
                    info.packageCachePath = "${info.userHome}/.devopstools/packages"
                }
            }

            // Determine CPU model
            detectCpuModel(info)

            // Create package cache directory if it doesn't exist
            try {
                Files.createDirectories(Paths.get(info.packageCachePath))
            } catch (e: Exception) {
                println("Error creating package cache directory: ${e.message}")
            }
        }

        /**
         * Alternative method for determining system memory
         */
        private fun detectMemoryAlternative(info: SystemInfo) {
            try {
                val osName = info.osName.lowercase()
                when {
                    // For Linux, read from /proc/meminfo
                    osName.contains("linux") -> {
                        val memInfoFile = File("/proc/meminfo")
                        if (memInfoFile.exists()) {
                            val content = memInfoFile.readText()

                            // Look for total memory
                            val totalMatch = "MemTotal:\\s+(\\d+)\\s+kB".toRegex().find(content)
                            if (totalMatch != null) {
                                val kbValue = totalMatch.groupValues[1].toLongOrNull() ?: 0
                                info.totalMemory = kbValue * 1024 // Convert from KB to bytes
                            }

                            // Look for free memory
                            val freeMatch = "MemFree:\\s+(\\d+)\\s+kB".toRegex().find(content)
                            if (freeMatch != null) {
                                val kbValue = freeMatch.groupValues[1].toLongOrNull() ?: 0
                                info.freeMemory = kbValue * 1024 // Convert from KB to bytes
                            }
                        }
                    }
                    // For Windows, use systeminfo command
                    osName.contains("windows") -> {
                        val process = Runtime.getRuntime().exec("systeminfo")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?

                        while (reader.readLine().also { line = it } != null) {
                            if (line!!.contains("Total Physical Memory")) {
                                val memMatch = "(\\d+,?\\d*)\\s+MB".toRegex().find(line!!)
                                if (memMatch != null) {
                                    val mbValue = memMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                                    info.totalMemory = (mbValue * 1024 * 1024).toLong() // MB to bytes
                                }
                            } else if (line!!.contains("Available Physical Memory")) {
                                val memMatch = "(\\d+,?\\d*)\\s+MB".toRegex().find(line!!)
                                if (memMatch != null) {
                                    val mbValue = memMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                                    info.freeMemory = (mbValue * 1024 * 1024).toLong() // MB to bytes
                                }
                            }
                        }
                    }
                    // For MacOS, use sysctl command
                    osName.contains("mac") -> {
                        val totalProcess = Runtime.getRuntime().exec("sysctl -n hw.memsize")
                        val totalReader = BufferedReader(InputStreamReader(totalProcess.inputStream))
                        val totalMemLine = totalReader.readLine()
                        if (totalMemLine != null) {
                            info.totalMemory = totalMemLine.trim().toLongOrNull() ?: 0
                        }

                        // For free memory on MacOS, need a different command
                        val freeProcess = Runtime.getRuntime().exec("vm_stat")
                        val freeReader = BufferedReader(InputStreamReader(freeProcess.inputStream))
                        var line: String?
                        var pageSize = 4096L // Default page size
                        var freePages = 0L

                        while (freeReader.readLine().also { line = it } != null) {
                            if (line!!.contains("page size of")) {
                                val pageSizeMatch = "(\\d+)\\s+bytes".toRegex().find(line!!)
                                if (pageSizeMatch != null) {
                                    pageSize = pageSizeMatch.groupValues[1].toLongOrNull() ?: 4096L
                                }
                            } else if (line!!.contains("Pages free")) {
                                val freePagesMatch = ":\\s+(\\d+)".toRegex().find(line!!)
                                if (freePagesMatch != null) {
                                    freePages = freePagesMatch.groupValues[1].toLongOrNull() ?: 0
                                }
                            }
                        }

                        info.freeMemory = freePages * pageSize
                    }
                    else -> {
                        // Fallback - use JVM information
                        val runtime = Runtime.getRuntime()
                        info.totalMemory = runtime.maxMemory()
                        info.freeMemory = runtime.freeMemory()
                    }
                }
            } catch (e: Exception) {
                // Fallback
                val runtime = Runtime.getRuntime()
                info.totalMemory = runtime.maxMemory()
                info.freeMemory = runtime.freeMemory()
            }
        }

        /**
         * Determines Linux distribution
         */
        private fun detectLinuxDistribution(info: SystemInfo) {
            try {
                // First check /etc/os-release file
                val osReleaseFile = File("/etc/os-release")
                if (osReleaseFile.exists()) {
                    val osReleaseContent = osReleaseFile.readText()

                    // Look for ID and VERSION_ID
                    val idMatch = "ID=([^\\s\"]+)".toRegex().find(osReleaseContent)
                    val versionMatch = "VERSION_ID=\"?([^\"\\s]+)\"?".toRegex().find(osReleaseContent)

                    val id = idMatch?.groupValues?.get(1) ?: ""
                    val version = versionMatch?.groupValues?.get(1) ?: ""

                    info.distribution = if (version.isNotEmpty()) "$id $version" else id

                    // Determine deb and rpm support based on ID
                    when (id.lowercase()) {
                        "ubuntu", "debian", "linuxmint", "elementary", "pop", "zorin" -> {
                            info.supportsDeb = true
                        }
                        "fedora", "rhel", "centos", "rocky", "alma", "ol", "opensuse" -> {
                            info.supportsRpm = true
                        }
                    }

                    return
                }

                // If os-release not found, check other files
                val lsbReleaseFile = File("/etc/lsb-release")
                if (lsbReleaseFile.exists()) {
                    val lsbContent = lsbReleaseFile.readText()
                    val distroMatch = "DISTRIB_ID=([^\\s]+)".toRegex().find(lsbContent)
                    val versionMatch = "DISTRIB_RELEASE=([^\\s]+)".toRegex().find(lsbContent)

                    val distro = distroMatch?.groupValues?.get(1) ?: ""
                    val version = versionMatch?.groupValues?.get(1) ?: ""

                    info.distribution = if (version.isNotEmpty()) "$distro $version" else distro

                    if (distro.lowercase() in listOf("ubuntu", "debian", "linuxmint")) {
                        info.supportsDeb = true
                    }

                    return
                }

                // If still undetermined, try using lsb_release command
                val process = Runtime.getRuntime().exec("lsb_release -a")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                var distro = ""
                var version = ""

                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("Distributor ID:")) {
                        distro = line!!.split(":")[1].trim()
                    } else if (line!!.contains("Release:")) {
                        version = line!!.split(":")[1].trim()
                    }
                }

                if (distro.isNotEmpty()) {
                    info.distribution = if (version.isNotEmpty()) "$distro $version" else distro

                    if (distro.lowercase() in listOf("ubuntu", "debian", "linuxmint")) {
                        info.supportsDeb = true
                    } else if (distro.lowercase() in listOf("fedora", "centos", "redhat")) {
                        info.supportsRpm = true
                    }
                }
            } catch (e: Exception) {
                info.distribution = "Unknown Linux Distribution"
            }
        }

        /**
         * Determines available package managers on Linux
         */
        private fun detectLinuxPackageManagers(info: SystemInfo) {
            // Check for package manager commands

            // Check AppImage support (can be used on all distributions)
            info.supportsAppImage = true

            // Check for package manager availability
            info.supportsSnapcraft = checkCommandExists("snap")
            info.supportsFlatpak = checkCommandExists("flatpak")

            // If deb not yet determined, check for apt or dpkg
            if (!info.supportsDeb) {
                info.supportsDeb = checkCommandExists("apt") || checkCommandExists("dpkg")
            }

            // If rpm not yet determined, check for yum, dnf, or rpm
            if (!info.supportsRpm) {
                info.supportsRpm = checkCommandExists("yum") ||
                        checkCommandExists("dnf") ||
                        checkCommandExists("rpm")
            }
        }

        /**
         * Determines CPU model
         */
        private fun detectCpuModel(info: SystemInfo) {
            try {
                when (info.osFamily) {
                    OsFamily.LINUX -> {
                        val cpuInfoFile = File("/proc/cpuinfo")
                        if (cpuInfoFile.exists()) {
                            val cpuInfoContent = cpuInfoFile.readText()
                            val modelMatch = "model name.*:(.*)".toRegex().find(cpuInfoContent)
                            info.cpuModel = modelMatch?.groupValues?.get(1)?.trim() ?: "Unknown"
                        }
                    }
                    OsFamily.WINDOWS -> {
                        val process = Runtime.getRuntime().exec("wmic cpu get name")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?
                        // Skip first line, which is the header
                        reader.readLine()
                        line = reader.readLine()
                        if (line != null) {
                            info.cpuModel = line.trim()
                        }
                    }
                    OsFamily.MACOS -> {
                        val process = Runtime.getRuntime().exec("sysctl -n machdep.cpu.brand_string")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val cpuModel = reader.readLine()
                        if (cpuModel != null) {
                            info.cpuModel = cpuModel.trim()
                        }
                    }
                    else -> {
                        info.cpuModel = "Unknown"
                    }
                }
            } catch (e: Exception) {
                info.cpuModel = "Unknown"
            }

            // If CPU model could not be determined, use general data
            if (info.cpuModel.isBlank() || info.cpuModel == "Unknown") {
                info.cpuModel = "${info.availableProcessors} cores ${info.osArch}"
            }
        }

        /**
         * Checks if a command exists on the system
         */
        private fun checkCommandExists(command: String): Boolean {
            return try {
                val process = if (System.getProperty("os.name").lowercase().contains("windows")) {
                    ProcessBuilder("where", command).start()
                } else {
                    ProcessBuilder("which", command).start()
                }
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }
    }
}