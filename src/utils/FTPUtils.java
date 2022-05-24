package utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.Properties;

public class FTPUtils {
    private String host;/*FTP服务器ip*/
    private int port;/*FTP服务器端口*/
    private String username;/*FTP登录账号*/
    private String password;/*FTP登录密码*/
    private String basePath;/*FTP服务器基础目录*/
    private String filePath;/*FTP服务器下级目录*/

    private static Log logger = LogFactory.getLog(FTPUtils.class);

    public FTPUtils(){
        Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File("src/ftp.properties"));
            properties.load(fileInputStream);
            String host = properties.getProperty("ftp.host");
            int port = Integer.parseInt(properties.get("ftp.port").toString());
            String username = properties.getProperty("ftp.username");
            String password = properties.getProperty("ftp.password");
            String basePath = properties.getProperty("ftp.base-path");
            String filePath = properties.getProperty("ftp.file-path");
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.basePath = basePath;
            this.filePath = filePath;
            logger.info("host:"+host+",port:"+port+",username:"+username+",password:"+password+",basePath:"+basePath+",filePath:"+filePath);
            logger.info("初始化ftp数据成功！");
        } catch (FileNotFoundException e) {
            logger.error("获取ftp配置文件失败！");
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("加载Properties失败，请检查ftp.properties文件内容是否正确！");
            e.printStackTrace();
        }
    }
    /**
     * 向FTP服务器上传文件
     * @param localFilePath  上传到FTP服务器上的文件路径
     * @param uploadFilePath FTP服务器文件存放路径。例如分日期存放：/2015/01/01。文件的路径为uploadBasePath+uploadFilePath
     * @return 成功返回true，否则返回false
     */
    public boolean uploadFile(String localFilePath, String uploadFilePath) {
        boolean result = false;
        FTPClient ftp = new FTPClient();
        try {
            //本地要上传的文件的 输入流
            InputStream input = new FileInputStream(new File(localFilePath));
            int reply;
            ftp.connect(host,port);// 连接FTP服务器
            // 如果采用默认端口，可以使用ftp.connect(uploadHost)的方式直接连接FTP服务器
            ftp.login(username, password);// 登录
            logger.info(host+":服务器连接成功！");
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return result;
            }
            ftp.enterLocalPassiveMode();
            //切换到上传目录
            try {
                if (!ftp.changeWorkingDirectory(basePath + uploadFilePath)) {
                    //如果目录不存在创建目录
                    String[] dirs = uploadFilePath.split("/");
                    String tempPath = basePath;
                    for (String dir : dirs) {
                        if (null == dir || "".equals(dir))
                            continue;
                        tempPath += "/" + dir;
                        if (!ftp.changeWorkingDirectory(tempPath)) {
                            if (!ftp.makeDirectory(tempPath)) {
                                logger.error("创建目录："+tempPath+"失败！");
                                return result;
                            } else {
                                ftp.changeWorkingDirectory(tempPath);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //设置上传文件的类型为二进制类型
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.setBufferSize(1024);
            ftp.setControlEncoding("UTF-8");
            //上传文件
            String filename =  "null.xlsx";
            if (localFilePath.contains("/")) {
                filename = localFilePath.substring(localFilePath.lastIndexOf("/") + 1);
            }
            if (localFilePath.contains("\\")) {
                filename = localFilePath.substring(localFilePath.lastIndexOf("\\") + 1);
            }
            if (!ftp.storeFile(filename, input)) {
                return result;
            }
            input.close();
            ftp.logout();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 从FTP服务器下载文件
     * @param remotePath FTP服务器上的相对路径
     * @param localPath  下载后保存到本地的路径
     * @return
     */
    public boolean downloadFile(String remotePath,String localPath) {
        remotePath = remotePath.replaceAll("\\\\","/").replaceAll("//","/");
        localPath = localPath.replaceAll("\\\\","/").replaceAll("//","/");
        boolean result = false;
        String fileName = null;
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(host, port);
            // 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
            ftp.login(username, password);// 登录
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return result;
            }
            if(remotePath.lastIndexOf("/")>-1){
                String dir = remotePath.substring(0,remotePath.lastIndexOf("/"));
                boolean stat = ftp.changeWorkingDirectory(dir);
                if(!stat){
                    throw new Exception("此文件或文件夹[" + dir + "]有误或不存在!");
                }else{
                    fileName = remotePath.substring(remotePath.lastIndexOf("/")+1);
                    logger.info("filename:"+fileName);
                }
            }else{
                throw new Exception("此文件或文件夹[" + remotePath + "]有误或不存在!");
            }
            FTPFile[] fs = ftp.listFiles();
            for (FTPFile ff : fs) {
                if (ff.getName().equals(fileName)) {
                    logger.info("ff.getName():"+ff.getName());
                    File localFile = new File(localPath + "/" + ff.getName());
                    OutputStream is = new FileOutputStream(localFile);
                    ftp.retrieveFile(ff.getName(), is);
                    is.close();
                }
            }

            ftp.logout();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return result;
    }

}
