/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent;
/*
 * 修订记录:
 * guqiu@yiji.com 2016-10-12 13:48 创建
 */

import com.falcon.suitagent.config.AgentConfiguration;
import com.falcon.suitagent.util.FileUtil;
import com.falcon.suitagent.util.StringUtils;
import com.falcon.suitagent.util.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.ho.yaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author guqiu@yiji.com
 */
@Slf4j
public class Update {

    private static final String downloadUrl = AgentConfiguration.INSTANCE.getAgentUpdateUrl();

    private static final String updateDownloadFilePath = FileUtils.getTempDirectoryPath() + File.separator + "suitAgent" + File.separator + "update.zip";
    private static final String updateFilesTempDir = FileUtils.getTempDirectoryPath() + File.separator +  "suitAgentUpdate";

    private static final String agentHome = AgentConfiguration.INSTANCE.getAgentHomeDir();

    private static final String log4UpdateStart = "---- update log ---- : ";

    /**
     * 升级
     * @throws IOException
     */
    public void update() throws IOException {
        log.info("----------------------------------------------------------------------");
        log.info("---------------------------Update Start-------------------------------");
        log.info("----------------------------------------------------------------------");
        log.info("{}now version : {}",log4UpdateStart, AgentConfiguration.VERSION);
        if(StringUtils.isEmpty(downloadUrl)){
            log.error("{}update download url is undefined.please config agent.update.pack.url on agent.properties <FAILED>",log4UpdateStart);
            return;
        }
        if(downloadUpdatePack()){
//            String baseDir = updateFilesTempDir + File.separator + "SuitAgent-Update-master";
            String baseDir = updateFilesTempDir + File.separator;
            File tempDir = new File(baseDir);
            if (tempDir.exists() && tempDir.list() != null && tempDir.list().length > 0){
                baseDir += tempDir.list()[0];
            }else {
                log.error("{}no update data found",log4UpdateStart);
                return;
            }

            boolean update = false;
            File baseDirFile = new File(baseDir);
            String[] ss = baseDirFile.list();
            if(baseDirFile.exists() && ss != null){
                List<Float> versions = new ArrayList<>();
                for (String s : ss) {
                    if(NumberUtils.isNumber(s)){
                        versions.add(NumberUtils.toFloat(s));
                    }
                }
                Collections.sort(versions);
                for (Float version : versions) {
                    if(version >= AgentConfiguration.VERSION){
                        log.info("--------------------------------------");
                        log.info("{}Update base from {}",log4UpdateStart,version);
                        String updateDir = baseDir + File.separator + version;
                        String updateListConfFile = updateDir + File.separator + "updateList.yml";
                        Map<String,Map<String,Object>> updateConf = Yaml.loadType(new FileInputStream(updateListConfFile),HashMap.class);

                        Map<String,Object> fileAddConf = updateConf.get("file.add");
                        Map<String,Object> fileReplaceConf = updateConf.get("file.replace");
                        Map<String,Object> propertiesModifyConf = updateConf.get("conf.properties.modify");

                        updateOfFileAdd(fileAddConf,updateDir);
                        updateOfFileReplace(fileReplaceConf,updateDir);
                        updateOfPropertiesModify(propertiesModifyConf, String.valueOf(version));
                        log.info("--------------------------------------");
                        update = true;
                    }
                }
            }
            if(!update){
                log.info("{}Congratulation ! you suit agent is up to date",log4UpdateStart);
            }

            //删除临时目录
            File f = new File(updateFilesTempDir);
            FileUtils.deleteDirectory(f);
            log.info("{}deleted temp dir {}",log4UpdateStart,updateFilesTempDir);
        }
        log.info("----------------------------------------------------------------------");
        log.info("---------------------------Update End-------------------------------");
        log.info("----------------------------------------------------------------------");

    }

    /**
     * 新增文件的升级操作
     * @param conf
     */
    private void updateOfFileAdd(Map<String,Object> conf, String updateDir) throws IOException {
        if(conf != null){
            Set<String> keys = conf.keySet();//key是更新服务器上的文件
            for (String key : keys) {
                String targetDir = String.valueOf(conf.get(key));//value是需要被添加的目录
                if(!StringUtils.isEmpty(targetDir)){
                    File updateFile = new File(updateDir + File.separator + key);
                    File destDir = new File(agentHome + File.separator + targetDir);
                    boolean update = true;
                    if(!updateFile.exists()){
                        log.error("{}update file '{}' is not exist",log4UpdateStart,updateFile.getAbsolutePath());
                        update = false;
                    }
                    if(!destDir.exists()){
                        log.error("{}update dir '{}' is not exist",log4UpdateStart,destDir.getAbsolutePath());
                        update = false;
                    }
                    if(update){
                        FileUtils.copyFileToDirectory(updateFile,destDir);
                        log.info("{}add file '{}' to dir '{}' <SUCCESS>",log4UpdateStart,updateFile.toPath().getFileName(),destDir.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * 文件替换的升级操作
     * @param conf
     */
    private void updateOfFileReplace(Map<String,Object> conf,String updateDir) throws IOException {
        if(conf != null){
            Set<String> keys = conf.keySet();//key是更新服务器上的文件
            for (String key : keys) {
                File updateFile = new File(updateDir + File.separator + key);
                String targetFile = String.valueOf(conf.get(key));//value是需要替换的目标文件
                targetFile = agentHome + File.separator + targetFile;

                //删除需要替换的文件
                Path targetPath = Paths.get(targetFile);
                try(DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath.getParent(),targetPath.getFileName().toString())){
                    for (Path path : stream) {
                        if(!path.toFile().delete()){
                            log.error("{}old file '{}' deleted <FAILED>. update <FAILED>",log4UpdateStart,path);
                            return;
                        }
                    }
                }catch (Exception e){
                    log.error("",e);
                }

                FileUtils.copyFileToDirectory(updateFile,targetPath.getParent().toFile());
                log.info("{}replace file '{}' to dir '{}' <SUCCESS>",log4UpdateStart,updateFile.toPath().getFileName(),targetPath.getParent());
            }
        }
    }

    /**
     * properties配置文件内容修改的升级操作
     * @param conf
     */
    private void updateOfPropertiesModify(Map<String,Object> conf,String version) throws IOException {
        if(conf != null){
            Map<String,Object> disableKey = (Map<String, Object>) conf.get("disableKey");
            updateOfPropertiesModify4DisableKey(disableKey, version);
            Map<String,Object> addKeyConf = (Map<String, Object>) conf.get("addKey");
            updateOfPropertiesModify4AddKey(addKeyConf, version);
            Map<String,Object> modifyKeyConf = (Map<String, Object>) conf.get("modifyKey");
            updateOfPropertiesModify4ModifyKey(modifyKeyConf, version);
        }
    }

    private void updateOfPropertiesModify4DisableKey(Map<String,Object> conf,String version) throws IOException {
        help4UpdateOfPropertiesModify2ModifyOrDisabled(conf,"disabled", version);
    }

    private void updateOfPropertiesModify4ModifyKey(Map<String,Object> conf,String version) throws IOException {
        help4UpdateOfPropertiesModify2ModifyOrDisabled(conf,"modify", version);
    }

    private void help4UpdateOfPropertiesModify2ModifyOrDisabled(Map<String,Object> conf,String type,String version) throws IOException {
        if(conf != null){
            Set<String> keys = conf.keySet();
            for (String key : keys) {
                //key是待处理key的Properties文件
                String targetPropertiesFilePath;
                if("modify".equals(type)){
                    if(key.contains("->")){
                        targetPropertiesFilePath = agentHome + File.separator + key.split("->")[0].trim();
                    }else{
                        log.error("{}modify properties key's update config format illegal (propertiesFile->key) : {}",log4UpdateStart,key);
                        continue;
                    }
                }else{
                    targetPropertiesFilePath = agentHome + File.separator + key;
                }
                File targetPropertiesFile = new File(targetPropertiesFilePath);
                //需要处理的配置key
                String targetKeyName;
                if ("modify".equals(type)){
                    if(key.contains("->")){
                        targetKeyName = key.split("->")[1].trim();
                    }else{
                        log.error("{}modify properties key's update config format illegal (propertiesFile->key) : {}",log4UpdateStart,key);
                        continue;
                    }
                }else{
                    targetKeyName = String.valueOf(conf.get(key));
                }
                if(!targetPropertiesFile.exists()){
                    log.error("{}{} '{}' properties key's file '{}' is not exist <FAILED>",log4UpdateStart,type,targetKeyName,targetPropertiesFilePath);
                    continue;
                }
                File backup = new File(targetPropertiesFilePath + ".updateBak");
                FileUtils.copyFile(targetPropertiesFile,backup);
                String content = FileUtil.getTextFileContent(targetPropertiesFilePath);
                StringBuilder finalContent = new StringBuilder();
                StringTokenizer st = new StringTokenizer(content,"\n",true);
                boolean add = true;//判断是否指定新key的添加操作
                String newPropertiesKey = "";//需要新增的Key
                if(targetKeyName.contains("=")){//如果是add操作，targetKeyName可能会是key = value的字符串
                    newPropertiesKey = targetKeyName.split("=")[0].trim();
                }else{
                    newPropertiesKey = targetKeyName;
                }
                while( st.hasMoreElements() ){
                    String line = st.nextToken();
                    String lineTrim = line.trim();
                    if("add".equals(type)){
                        if(!lineTrim.startsWith("#") && lineTrim.startsWith(newPropertiesKey)){
                            //如果已存在key，否定新key的添加操作
                            add = false;
                        }
                        finalContent.append(line);
                    }else{
                        //非add操作，否定key的添加操作
                        add = false;
                        if(lineTrim.startsWith(targetKeyName)){
                            if("modify".equals(type)){
                                finalContent.append("#### modify by update version ").append(version).append(" ").append(new SimpleDateFormat().format(new Date())).append(" ####\n");
                                finalContent.append(targetKeyName).append(" = ").append(String.valueOf(conf.get(key)));
                            }else{
                                finalContent.append("#### disabled by update ").append(version).append(" ").append(new SimpleDateFormat().format(new Date())).append(" ####\n");
                                finalContent.append("#").append(line);

                            }
                        }else{
                            finalContent.append(line);
                        }
                    }
                }

                if(add){
                    finalContent.append("\n#### add by update ").append(version).append(" ").append(new SimpleDateFormat().format(new Date())).append(" ####\n");
                    finalContent.append(targetKeyName).append("\n");
                }

                //写入最后的结果
                if(FileUtil.writeTextToTextFile(finalContent.toString(),targetPropertiesFile,false)){
                    log.info("{}{} '{}' properties key from '{}' <SUCCESS>",log4UpdateStart,type,targetKeyName,key);
                }else{
                    log.info("{}{} '{}' properties key from '{}' <FAILED>",log4UpdateStart,type,targetKeyName,key);
                    //恢复备份的文件
                    FileUtils.copyFile(backup,targetPropertiesFile);
                }

                //删除备份文件
                if(!backup.delete()){
                    log.warn("{}backup file '{}' deleted <FAILED>",log4UpdateStart,backup.getAbsolutePath());
                }
            }
        }
    }

    private void updateOfPropertiesModify4AddKey(Map<String,Object> conf,String version) throws IOException {
        help4UpdateOfPropertiesModify2ModifyOrDisabled(conf,"add",version);
    }

    /**
     * 下载更新文件
     * @return
     */
    private boolean downloadUpdatePack(){
        try {
            URL url = new URL(downloadUrl);

            File f = new File(updateDownloadFilePath);
            log.info("{}downloading update file from {} ...",log4UpdateStart,downloadUrl);
            FileUtils.copyURLToFile(url, f);
            log.info("{}update file download completed!",log4UpdateStart);
            File zipDownloadFile = new File(updateFilesTempDir);
            if(zipDownloadFile.exists()){
                FileUtils.deleteDirectory(zipDownloadFile);
            }
            log.info("{}unpack update file to {}",log4UpdateStart,updateFilesTempDir);
            ZipUtil.unzip(updateDownloadFilePath, updateFilesTempDir);

            if(f.delete()){
                log.info("{}delete temp file {}",log4UpdateStart,updateDownloadFilePath);
            }
        } catch (Exception e) {
            log.info("{}download update file <FAILED>",log4UpdateStart,e);
            return false;
        }
        return true;
    }

}
