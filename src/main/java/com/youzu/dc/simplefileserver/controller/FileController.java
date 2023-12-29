package com.youzu.dc.simplefileserver.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class FileController {

    private static final ConcurrentHashMap<String, AtomicInteger> uploadProgressMap = new ConcurrentHashMap<>();

    @GetMapping("/")
    public String index(@RequestParam(required = false, defaultValue = "/") String path, Model model)
            throws IOException {
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        var allFile = getAllFilesInDirectory(path);
        model.addAttribute("title", "简单文件服务器");
        model.addAttribute("fileInfos", allFile);
        model.addAttribute("directory", path);
        return "index";
    }

    public List<FileInfo> getAllFilesInDirectory(String directoryPath) throws IOException {
        Path dir = Paths.get(directoryPath);
        List<FileInfo> fileList = Files.list(dir)
                .map(path -> {
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setName(path.getFileName().toString());
                    fileInfo.setAbsolutePath(path.toAbsolutePath().toString());
                    try {
                        fileInfo.setSize(Files.size(path));
                        fileInfo.setLastModified(Files.getLastModifiedTime(path).toInstant());
                        fileInfo.isFile = Files.isRegularFile(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return fileInfo;
                })
                .collect(Collectors.toList());
        return fileList;
    }

    @GetMapping("/download/")
    public ResponseEntity<Resource> download(@RequestParam String filename) {
        filename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        Path path = Paths.get(filename);
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(MultipartHttpServletRequest request)
            throws ExecutionException {
        Map<String, Object> response = new HashMap<>();
        Path dir = Paths.get(request.getParameter("path")).toAbsolutePath();

        Iterator<String> iterator = request.getFileNames();
        MultipartFile file = request.getFile(iterator.next());
        if (file == null) {
            response.put("message", "文件不存在！");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        String uuid = request.getParameter("uuid");
        log.info(uuid + "开始上传文件: " + dir + "/" + file.getOriginalFilename());

        try {
            Path path = dir.resolve(file.getOriginalFilename());
            byte[] bytes = file.getBytes();
            // 使用前端传来的UUID作为键，初始化进度值为0
            AtomicInteger progress = new AtomicInteger(0);
            uploadProgressMap.put(uuid, progress);

            try (OutputStream os = Files.newOutputStream(path)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);

                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    int currentProgress = (int) (totalBytesRead * 100 / file.getSize());
                    progress.set(currentProgress); // 更新进度值
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.error("文件上传失败", e);
            }
            response.put("message", "文件上传成功！");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("message", "文件上传失败，请查看日志！");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/upload-progress/{uuid}")
    public ResponseEntity<Integer> getUploadProgress(@PathVariable("uuid") String uuid) {
        AtomicInteger progress = uploadProgressMap.get(uuid);
        if (progress == null) {
            return new ResponseEntity<>(0, HttpStatus.OK);
        }
        if (progress.get() == 100) {
            uploadProgressMap.remove(uuid);
            log.info(uuid + "上传进度值 100");
            return new ResponseEntity<>(100, HttpStatus.OK);
        }
        log.info(uuid + "上传进度值 " + progress);
        return new ResponseEntity<>(progress.get(), HttpStatus.OK);

    }

    @GetMapping("/upload-result")
    public String uploadResult(@RequestParam(required = false, defaultValue = "/") String path, Model model) throws IOException {
        model.addAttribute("path", path);
        model.addAttribute("message", "文件上传成功！");
        return "upload-result";
    }

    @Getter
    @Setter
    public class FileInfo {
        private String name;
        private String size;
        private Instant lastModified;
        private Boolean isFile;
        private String absolutePath;

        public void setSize(long size) {
            this.size = readableFileSize(size);
        }

        public void setAbsolutePath(String absolutePath) {
            this.absolutePath = URLEncoder.encode(absolutePath, StandardCharsets.UTF_8);
        }
    }

    public String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
