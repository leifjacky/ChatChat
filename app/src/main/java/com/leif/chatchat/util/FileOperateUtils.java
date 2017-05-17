package com.leif.chatchat.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileOperateUtils {
      
    /**  
     *   
     * @param fromFile 被复制的文件  
     * @param toFile 复制的目录文件  
     * @param rewrite 是否重新创建文件  
     *   
     * <p>文件的复制操作方法  
     */  
    public static void copyfile(File fromFile, File toFile, Boolean rewrite ){
          
        if(!fromFile.exists()){  
            return;  
        }  
          
        if(!fromFile.isFile()){  
            return;  
        }  
        if(!fromFile.canRead()){  
            return;  
        }  
        if(!toFile.getParentFile().exists()){  
            toFile.getParentFile().mkdirs();  
        }  
        if(toFile.exists() && rewrite){  
            toFile.delete();  
        }  
          
          
        try {  
            FileInputStream fosfrom = new FileInputStream(fromFile);
            FileOutputStream fosto = new FileOutputStream(toFile);
              
            byte[] bt = new byte[1024];  
            int c;  
            while((c=fosfrom.read(bt)) > 0){  
                fosto.write(bt,0,c);  
            }  
            //关闭输入、输出流  
            fosfrom.close();  
            fosto.close();  
              
              
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        } catch (IOException e) {
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
          
    }  
  
}  