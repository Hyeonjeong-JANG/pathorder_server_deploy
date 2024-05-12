package shop.project.pathorderserver._core.utils;

import org.springframework.web.multipart.MultipartFile;
import shop.project.pathorderserver._core.DefaultFile;
import shop.project.pathorderserver._core.MimeType;
import shop.project.pathorderserver._core.errors.exception.Exception400;
import shop.project.pathorderserver._core.errors.exception.Exception500;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class FileUtil {

    public static String uploadFile(MultipartFile file) {
        // UUID_파일 이름.확장자
        try {
            String newFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path newFilePath = Paths.get("./upload/" + newFilename);

            Files.write(newFilePath, file.getBytes());
            return newFilename; // 생성된 파일 이름 return
        } catch (Exception e) {
            throw new Exception400(e.getMessage());
        }
    }

    public static String uploadBase64(String encodedData, String filename) {
        // UUID_파일 이름.확장자
        try {
            String mimeType = encodedData.substring(encodedData.indexOf(":") + 1, encodedData.indexOf(";"));
            MimeType type = MimeType.findByMimeType(mimeType);
            if (type == null) { // 미확인 확장자 -> null return.
                return null;
            }
            String extension = type.getExtension();
            String newFilename = UUID.randomUUID() + "_" + filename + extension;
            Path newFilePath = Paths.get("./upload/" + newFilename);

            byte[] decodedByte = Base64.getDecoder().decode(encodedData.split(",")[1]);

            Files.write(newFilePath, decodedByte);
            return newFilename; // 생성된 파일 이름 return
        } catch (Exception e) {
            throw new Exception400(e.getMessage());
        }
    }

    public static String uploadBase64Jpg(String encodedData, String filename) {
        // UUID_파일 이름.확장자 (* '.jpg'로 고정)
        String newFilename = UUID.randomUUID() + "_" + filename + MimeType.JPEG.getExtension();
        Path newFilePath = Paths.get("./upload/" + newFilename);
        try {
            byte[] decodedByte = Base64.getDecoder().decode(encodedData);

            Files.write(newFilePath, decodedByte);
            return newFilename; // 생성된 파일 이름 return
        } catch (Exception e) {
            throw new Exception400(e.getMessage());
        }
    }

    public static void deleteFile(String filename) {
        if (!DefaultFile.contains(filename)) { // 기존 파일이 아닌 경우에만 삭제
            try {
                Path filePath = Paths.get("." + getFilePath(filename));
                Files.delete(filePath);
            } catch (Exception e) {
                throw new Exception500(e.getMessage());
            }
        }
    }

    public static String getFilePath(String filename) {
        return "/upload/" + filename; // 파일 경로 return
    }
}
