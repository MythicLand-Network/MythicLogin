package cn.mythicland.mythiclogin.bootstrap;

import java.util.List;
import java.util.Set;

record RuntimeLibrary(
        String coordinate,
        String fileName,
        List<String> downloadUrls,
        String sha256,
        Set<String> platforms
) {
}
