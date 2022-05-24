import utils.FTPUtils;

public class mainApp {

    public static void main(String[] args) {
        FTPUtils ftpUtils = new FTPUtils();
        String localFilePath = "src/11111.png";
        String uploadFilePath = "test";
//        boolean result = ftpUtils.uploadFile(localFilePath,uploadFilePath);
        boolean result = ftpUtils.downloadFile("app/test/11111.png","temp");
        System.out.println(result);
    }
}
