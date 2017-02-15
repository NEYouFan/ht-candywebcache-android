package com.netease.hearttouch

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.util.regex.Pattern

class PresetResourcePlugin implements Plugin<Project> {
    private String PROTOCOL_VERSION = "0.1";

    @Override
    void apply(Project project) {
        project.extensions.add("presetExt", PresetExt)

        project.task('presetResourceTask') {
            //task的group
            group 'hearttouch'

            doLast {
                PresetExt ext = project.presetExt
                if (!ext.needDownload) {
                    println "user setting: No need to redownload..."
                    return
                }
                //清除旧文件，如果下载失败等原因，下次重新打包的时候会再清理的
                File projectDir = project.getProjectDir()
                String targetPath = "src/main/assets/webapps"
                if (ext.downloadPath != null && ext.downloadPath != "") {
                    targetPath = ext.downloadPath + "/webapps";
                }
                File dirFile = new File(projectDir, targetPath)
                if (dirFile.exists()) {
                    dirFile.deleteDir()
                }
                println dirFile.getAbsolutePath()
                dirFile.mkdirs()

                def json = new JsonBuilder()
                json {
                    version PROTOCOL_VERSION
                    appID ext.appID
                    appVersion ext.appVersion
                    platform "android"
                    autoFill true
                    isDiff false
                }
                println "request data:" + json

                def connection = new URL(ext.url).openConnection()
                connection.setRequestMethod('POST')
                connection.useCaches = false
                connection.doOutput = true
                connection.readTimeout = 10000
                connection.connectTimeout = 10000
                connection.setRequestProperty("Content-Type", "application/json")

                def writer = new OutputStreamWriter(connection.outputStream)


                writer.write(json.toString())
                writer.flush()
                writer.close()
                connection.connect()
                def responseText = connection.content.text
                println "response data:" + responseText

                def jsonSlurper = new JsonSlurper()
                def responseData = jsonSlurper.parseText(responseText)
                if (responseData.code != 200) {
                    throw new RuntimeException("code in data return from server is " + responseData.code + "! errMsg:" + responseData.errMsg)
                }
                //下面发生找不到字段的错误让他自己报错就行了
                def resInfos = responseData.data.resInfos
                File xmlFile = new File(dirFile, "webapps.xml")
                def xmlWriter = new FileWriter(xmlFile)

                def xmlBuilder = new MarkupBuilder(xmlWriter)
                xmlBuilder.getMkp().pi(xml: [version: '1.0', encoding: 'UTF-8'])

                RandomAccessFile raFile = null
                BufferedInputStream bis = null
                try {
                    xmlBuilder.webappinfos {
                        resInfos.each {
//                            if (it.state != 1 || it.state != 3) {
//                                throw new RuntimeException("state is " + it.state + " for " + it.resID);
//                            }
                            if(it.userData != null && !it.userData.equals("")){
                                def userData = jsonSlurper.parseText(it.userData)
                                def domains = userData.domains
                                webapp(name: it.resID, md5: it.fullMd5, version: it.resVersion) {
                                    for (d in domains) {
                                        domain d
                                    }
                                }
                            }
                            def downloadConnection = new URL(it.fullUrl).openConnection()
                            downloadConnection.setRequestMethod('GET')
                            downloadConnection.useCaches = false
                            downloadConnection.doOutput = true
                            downloadConnection.readTimeout = 10000
                            downloadConnection.connectTimeout = 10000

                            def extension = getFileExtensionFromUrl(it.fullUrl);
                            if (extension == null || extension.equals("")) {
                                extension = "zip"
                            }
                            extension = "." + extension
                            println "downloading " + it.resID + extension

                            File file = new File(dirFile, it.resID + extension);
                            raFile = new RandomAccessFile(file, "rw")

                            byte[] buf = new byte[8192];
                            bis = new BufferedInputStream(downloadConnection.inputStream)
                            int size = 0
                            while ((size = bis.read(buf)) != -1) {
                                raFile.write(buf, 0, size);
                            }
                            bis.close()
                            raFile.close()
                        }
                        println "download completed, find files in " + dirFile.getAbsolutePath()
                    }
                } catch (Exception e) {
                    if (bis != null) {
                        bis.close()
                    }
                    if (raFile != null) {
                        bis.close()
                    }
                    if (xmlWriter != null) {
                        xmlWriter.close()
                    }
                    dirFile.deleteDir()

                    //丢出去
                    throw e
                }
            }
        }


        project.afterEvaluate {
            project.tasks.findByName("assembleRelease")
        }

        project.afterEvaluate {
            for (Task task : project.tasks) {
                if(task.name.startsWith("assemble") && task.name.endsWith("Release")) {
                    task.dependsOn 'presetResourceTask'
                }
            }
        }
    }

    private String getFileExtensionFromUrl(String url) {
        if (url != null && !url.equals("")) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() &&
                    Pattern.matches("[a-zA-Z_0-9\\.\\-\\(\\)\\%]+", filename)) {
                int dotPos = filename.lastIndexOf('.');
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1);
                }
            }
        }

        return "";
    }
}