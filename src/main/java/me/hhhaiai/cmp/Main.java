package me.hhhaiai.cmp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Copyright © 2021 sanbo Inc. All rights reserved.
 * @Description: TODO
 * @Version: 1.0
 * @Create: 2021/11/23 11:07 上午
 * @author: sanbo
 */
public class Main {
    public static void main(String[] args) throws Throwable {
        prepareAndRun();
    }

    private static void prepareAndRun() throws JSONException {
        String baseDirPath = "Git_result";
        String parserDirPath = baseDirPath + "/info";
        List<String> noRunDirNames = Arrays.asList(new String[]{
                ".gitkeep", "1.0", "1.1", ".DS_Store"
        });
        // 1. 确保解析文件存在
        makesureDirExists(baseDirPath);
        // 2. 生成解析树
        Set<String> paths = new HashSet<String>();
        iteraDir(paths, parserDirPath, noRunDirNames);
        System.out.println("遍历后的个数：" + paths.size());

        JSONObject cmpResult = new JSONObject();
        // 3.获取单个文件内容
        for (String path : paths) {
            JSONObject json = parserOneFile(path);
            if (json.length() > 0) {
                cmpResult.put(path, json);
            }
        }

        if (cmpResult.length() > 0) {
            FileUtils.saveTextToFile(FileUtils.getDestopFilePath("result.json"), cmpResult.toString(4), false
            );
        }
    }

    private static JSONObject parserOneFile(String path) {
//        System.out.println(path);
        String s = FileUtils.readContent(path);
        try {
            JSONObject obj = new JSONObject(s);
            if (obj.length() == 3) {
                JSONObject cmpBody = obj.optJSONObject("对比");
                return cmp4501and4403(path, cmpBody.optJSONObject("4501info"), cmpBody.optJSONObject("4403info"));

            }
        } catch (Throwable e) {
            System.err.println(path + "\r\n" + s);
        }
        return new JSONObject();
    }

    private static JSONObject cmp4501and4403(String path, JSONObject info4501, JSONObject info4403) throws JSONException {
        JSONObject finalResult = new JSONObject();

        JSONArray result4403 = info4403.optJSONArray("4403-result");
        Map<String, JSONObject> mapResult4403 = parserToMap(result4403);

        JSONArray scanReslt4403 = info4403.optJSONArray("403-scanReslt");
        Map<String, JSONObject> mapScanReslt4403 = parserToMap(scanReslt4403);

        JSONArray result4501 = info4501.optJSONArray("4501-result");
        Map<String, JSONObject> mapResult4501 = parserToMap(result4501);

        JSONArray scanReslt4501 = info4501.optJSONArray("4501-scanReslt");
        Map<String, JSONObject> mapScanReslt4501 = parserToMap(scanReslt4501);

        //对比自己
        Map<String, JSONObject> c4403Self = cmp4403Self(path, mapResult4403, mapScanReslt4403);
        Map<String, JSONObject> c4501Self = cmp4501Self(path, mapResult4501, mapScanReslt4501);
//        System.out.println("对比自己结果  4403:" + c4403Self.size() + "----4501:" + c4501Self.size());

        if (c4403Self.size() > 0) {
            finalResult.put("4403自身对比", new JSONObject(c4403Self));
        }
        if (c4501Self.size() > 0) {
            finalResult.put("4501自身对比", new JSONObject(c4501Self));
        }
        // 跨版本对比

        Map<String, Map<String, JSONObject>> cmpScanResult = cmpScan(path, mapScanReslt4403, mapScanReslt4501);
        Map<String, Map<String, JSONObject>> cmpResult = cmpResult(path, mapResult4403, mapResult4501);

        if (cmpScanResult.size() > 0) {
            finalResult.put("【扫描结果对比】4501--4403", new JSONObject(cmpScanResult));
        }
        if (cmpResult.size() > 0) {
            finalResult.put("【结果对比】4501--4403", new JSONObject(cmpResult));
        }

        return finalResult;
    }

    private static Map<String, Map<String, JSONObject>> cmpResult(String path
            , Map<String, JSONObject> mapResult4403
            , Map<String, JSONObject> mapResult4501) {

        Map<String, Map<String, JSONObject>> finalResult = new HashMap<String, Map<String, JSONObject>>();

        if (mapResult4403.size() == mapResult4501.size()
                && mapResult4403.keySet().containsAll(mapResult4501.entrySet())
                && mapResult4403.keySet().containsAll(mapResult4501.entrySet())
        ) {
//            System.out.println(path + " 4403结果一样");
            return finalResult;
        }
        Map<String, JSONObject> r4403Has4501NotHas = new HashMap<String, JSONObject>();

        // 4403有，4501没有
        for (String key : mapResult4403.keySet()) {
            if (!mapResult4501.containsKey(key)) {
                r4403Has4501NotHas.put(key, mapResult4403.get(key));
            }
        }
        Map<String, JSONObject> r4501Has4403NotHas = new HashMap<String, JSONObject>();
        // 4501有，4403没有
        for (String key : mapResult4501.keySet()) {
            if (!mapResult4403.containsKey(key)) {
                r4501Has4403NotHas.put(key, mapResult4501.get(key));
            }
        }
        if (r4403Has4501NotHas.size() > 0) {
            finalResult.put("【结果】4403包含4501没有", r4403Has4501NotHas);
        }
        if (r4501Has4403NotHas.size() > 0) {
            finalResult.put("【结果】4501包含4403没有", r4501Has4403NotHas);
        }
        return finalResult;
    }

    private static Map<String, Map<String, JSONObject>> cmpScan(String path
            , Map<String, JSONObject> mapScanReslt4403
            , Map<String, JSONObject> mapScanReslt4501
    ) {

        Map<String, Map<String, JSONObject>> finalResult = new HashMap<String, Map<String, JSONObject>>();

        if (mapScanReslt4403.size() == mapScanReslt4501.size()
                && mapScanReslt4403.keySet().containsAll(mapScanReslt4501.entrySet())
                && mapScanReslt4403.keySet().containsAll(mapScanReslt4501.entrySet())
        ) {
//            System.out.println(path + " 4403结果一样");
            return finalResult;
        }
        Map<String, JSONObject> r4403Has4501NotHas = new HashMap<String, JSONObject>();

        // 4403有，4501没有
        for (String key : mapScanReslt4403.keySet()) {
            if (!mapScanReslt4501.containsKey(key)) {
                r4403Has4501NotHas.put(key, mapScanReslt4403.get(key));
            }
        }
        Map<String, JSONObject> r4501Has4403NotHas = new HashMap<String, JSONObject>();
        // 4501有，4403没有
        for (String key : mapScanReslt4501.keySet()) {
            if (!mapScanReslt4403.containsKey(key)) {
                r4501Has4403NotHas.put(key, mapScanReslt4501.get(key));
            }
        }
        if (r4403Has4501NotHas.size() > 0) {
            finalResult.put("【扫描结果】4403包含4501没有", r4403Has4501NotHas);
        }
        if (r4501Has4403NotHas.size() > 0) {
            finalResult.put("【扫描结果】4501包含4403没有", r4501Has4403NotHas);
        }

        return finalResult;

    }


    private static Map<String, JSONObject> cmp4403Self(String path, Map<String, JSONObject> result4403, Map<String, JSONObject> scanReslt4403) {
        Map<String, JSONObject> r4403 = new HashMap<String, JSONObject>();

        if (result4403.size() == scanReslt4403.size()
                && result4403.keySet().containsAll(scanReslt4403.entrySet())
                && scanReslt4403.keySet().containsAll(result4403.entrySet())
        ) {
//            System.out.println(path + " 4403结果一样");
            return r4403;
        }

//        // 获取结果有,扫描结果没有值
//        for (String key:result4403.keySet()) {
//            if (!scanReslt4403.containsKey(key)){
//                System.out.println(key+"----4403------>"+result4403.get(key));
//            }
//        }
        // 获取扫描结果有,结果没有值
        for (String key : scanReslt4403.keySet()) {
            if (!result4403.containsKey(key)) {
//                System.out.println("【"+path+"】"+key+"----------->"+scanReslt4403.get(key) );
                r4403.put(key, scanReslt4403.get(key));
            }
        }
        return r4403;
    }

    private static Map<String, JSONObject> cmp4501Self(String path, Map<String, JSONObject> result4501, Map<String, JSONObject> scanReslt4501) {
        Map<String, JSONObject> r4501 = new HashMap<String, JSONObject>();

        if (result4501.size() == scanReslt4501.size()
                && result4501.keySet().containsAll(scanReslt4501.entrySet())
                && scanReslt4501.keySet().containsAll(result4501.entrySet())
        ) {
            //System.out.println(path + " 4501结果一样");
            return r4501;
        }
//        // 获取结果有,扫描结果没有值
//        for (String key:result4501.keySet()) {
//            if (!scanReslt4501.containsKey(key)){
//                System.out.println(key+"-----4501----->"+result4501.get(key));
//            }
//        }

        // 获取扫描结果有,结果没有值
        for (String key : scanReslt4501.keySet()) {
            if (!result4501.containsKey(key)) {
                r4501.put(key, scanReslt4501.get(key));
            }
        }
        return r4501;
    }

    /**
     * 将json转换为map {pkg:selfJSON}
     *
     * @param json
     * @return
     */
    private static Map<String, JSONObject> parserToMap(JSONArray json) {
        Map<String, JSONObject> result = new HashMap<>();
        if (json == null || json.length() <= 0) {
            return result;
        }
        for (int i = 0; i < json.length(); i++) {
            JSONObject js = json.optJSONObject(i);
            String pkg = js.optString("package_bane", "");
            if ("".equals(pkg) || pkg.length() < 1) {
                continue;
            }
            result.put(pkg, js);
        }
        return result;
    }

    /**
     * 遍历文件夹
     *
     * @param paths
     * @param parserDirPath
     * @param noRunDirNames
     */
    private static void iteraDir(Set<String> paths, String parserDirPath, List<String> noRunDirNames) {
        File parserDir = new File(parserDirPath);
        File[] listFiles = parserDir.listFiles();
        for (File f : listFiles) {
            if (!noRunDirNames.contains(f.getName())) {
                if (f.isDirectory()) {
                    iteraDir(paths, f.getAbsolutePath(), noRunDirNames);
                } else {
                    paths.add(f.getAbsolutePath());
                }
            }
        }

    }

    /**
     * 确保文件夹可用
     *
     * @param baseDirPath
     */
    private static void makesureDirExists(String baseDirPath) {
        if (!new File(baseDirPath).exists()) {
            try {
                Runtime.getRuntime().exec("git clone https://github.com/hhhaiai/Git_result.git");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
