package com.projects.sharefiles.controller;

import com.projects.sharefiles.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("admin/health")
public class AdminHealthController {

    @Autowired
    private FileStorageService fileStorageService;

    // Initialize OSHI SystemInfo once
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final CentralProcessor processor = hardware.getProcessor();

    // Store previous ticks to calculate load delta
    private long[] prevTicks = new long[CentralProcessor.TickType.values().length];

    public AdminHealthController() {
        this.prevTicks = processor.getSystemCpuLoadTicks();
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. ACCURATE CPU LOAD (OSHI)
        // OSHI calculates load by comparing "ticks" between two moments.
        // We use the previous call's ticks vs now.
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks(); // Update ticks for next time

        // Convert to percentage (0.0 to 1.0 -> 0 to 100)
        int cpuPercent = (int) (cpuLoad * 100);

        data.put("cpu", Map.of(
                "load", cpuPercent,
                "cores", processor.getLogicalProcessorCount()
        ));

        // 2. ACCURATE SYSTEM RAM (OSHI)
        GlobalMemory memory = hardware.getMemory();
        long totalRam = memory.getTotal();
        long availableRam = memory.getAvailable();
        long usedRam = totalRam - availableRam;

        data.put("systemMemory", Map.of(
                "totalGB", totalRam / 1024 / 1024 / 1024,
                "usedGB", usedRam / 1024 / 1024 / 1024,
                "percent", (int) ((double) usedRam / totalRam * 100)
        ));

        // 3. DISK (Standard Java is fine here)
        File disk = new File(".");
        long totalSpace = disk.getTotalSpace();
        long freeSpace = disk.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        data.put("disk", Map.of(
                "totalGB", totalSpace / 1024 / 1024 / 1024,
                "usedGB", usedSpace / 1024 / 1024 / 1024,
                "freeGB", freeSpace / 1024 / 1024 / 1024,
                "percent", (int) ((double) usedSpace / totalSpace * 100)
        ));

        // 4. UPTIME
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        data.put("uptimeMinutes", uptimeMs / 1000 / 60);

        return data;
    }

        @GetMapping("/storage")
    public Map<String, Object> storage() {
        return fileStorageService.storageStats();
    }
}


//import com.projects.sharefiles.service.FileStorageService;
//import com.sun.management.OperatingSystemMXBean;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.File;
//import java.lang.management.ManagementFactory;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/admin/health")
//public class AdminHealthController {
//
//    @Autowired
//    private FileStorageService  fileStorageService;
//
//    OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//
//    @GetMapping
//    public Map<String, Object> health() {
//
//        Map<String, Object> data = new LinkedHashMap<>();
//
//        // 1. SYSTEM CPU & RAM (Matches Task Manager)
//        // We cast to the specific "com.sun.management" interface to get System-level stats
//
//        // System RAM
//        long totalRam = os.getTotalMemorySize();
//        long freeRam = os.getFreeMemorySize();
//        long usedRam = totalRam - freeRam;
//
//        data.put("systemMemory", Map.of(
//                "totalGB", totalRam / 1024 / 1024 / 1024,
//                "usedGB", usedRam / 1024 / 1024 / 1024,
//                "percent", (int) ((double) usedRam / totalRam * 100)
//        ));
//
//        // System CPU
//        double systemCpu = os.getCpuLoad(); // Returns 0.0 to 1.0 (System Load)
//        // If systemCpu is negative (unavailable), fallback to 0
//        int cpuPercent = (int) (Math.max(0, systemCpu) * 100);
//
//        data.put("cpu", Map.of(
//                "load", cpuPercent,
//                "cores", os.getAvailableProcessors()
//        ));
//
//        // 2. DISK STORAGE
//        File disk = new File("."); // Checks drive where app is running
//        long totalSpace = disk.getTotalSpace();
//        long freeSpace = disk.getFreeSpace();
//        long usedSpace = totalSpace - freeSpace;
//
//        data.put("disk", Map.of(
//                "totalGB", totalSpace / 1024 / 1024 / 1024,
//                "usedGB", usedSpace / 1024 / 1024 / 1024,
//                "freeGB", freeSpace / 1024 / 1024 / 1024,
//                "percent", (int) ((double) usedSpace / totalSpace * 100)
//        ));
//
//        // 3. UPTIME
//        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
//        data.put("uptimeMinutes", uptimeMs / 1000 / 60);
//
//        return data;
//
////        Map<String, Object> data = new LinkedHashMap<>();
////
////        // JVM memory
////        Runtime rt = Runtime.getRuntime();
////        long max = rt.maxMemory();
////        long total = rt.totalMemory();
////        long free = rt.freeMemory();
////
////        data.put("jvmMemory", Map.of(
////                "maxMB", max / 1024 / 1024,
////                "usedMB", (total - free) / 1024 / 1024,
////                "freeMB", free / 1024 / 1024
////        ));
////
////        // Disk
////        File disk = new File(".");
////        data.put("disk", Map.of(
////                "totalGB", disk.getTotalSpace() / 1024 / 1024 / 1024,
////                "freeGB", disk.getFreeSpace() / 1024 / 1024 / 1024,
////                "usableGB", disk.getUsableSpace() / 1024 / 1024 / 1024
////        ));
////
////        // CPU
////        OperatingSystemMXBean os =
////                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
////
////        data.put("cpu", Map.of(
////                "load", os.getCpuLoad(),
////                "cores", os.getAvailableProcessors()
////        ));
////
////        // Uptime
////        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
////        data.put("uptimeMinutes", uptimeMs / 1000 / 60);
////
////        return data;
//    }
//
//    @GetMapping("/storage")
//    public Map<String, Object> storage() {
//        return fileStorageService.storageStats();
//    }
//
//}
