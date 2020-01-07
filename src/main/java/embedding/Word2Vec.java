package embedding;

import config.CmdConfig;
import config.PathConfig;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tool.Tool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author cary.shi on 2019/11/29
 */
public class Word2Vec {
    private final static Logger logger = LogManager.getLogger(Word2Vec.class);

    // generate ast corpus, save all ast content file in corpus file
    public static File generateAstCorpus(String astContentFolderPath, String astContentFilePath) {
        File[] astContentFiles = new File(astContentFolderPath).listFiles();
        List<String> contentList = new ArrayList<>();
        assert astContentFiles != null;
        for (File astContentFile : astContentFiles) {
            try {
                String content = FileUtils.readFileToString(astContentFile, StandardCharsets.UTF_8);
                contentList.add(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File astContentFile = new File(astContentFilePath);
        if (astContentFile.exists()) {
            astContentFile.delete();
        }
        try {
            FileUtils.writeLines(astContentFile, contentList, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return astContentFile;
    }

    // generate word2vec out file
    public static File generateWord2vecFile(File corpusFile, String word2vecOutPath, String word2vecCmdPath, int length) {
        File word2vecOutFile = new File(word2vecOutPath);
        if (word2vecOutFile.exists()) {
            word2vecOutFile.delete();
        }
        String cmd = CmdConfig.WORD2VEC_SHELL_PATH + " " + corpusFile.getParentFile().getAbsolutePath() + " " +
                corpusFile.getParentFile().getAbsolutePath() + " " + word2vecCmdPath + " " + length;

        Tool.executeCmdAndSaveLog(cmd, logger);
        if (!word2vecOutFile.exists()) {
            return null;
        }
        return word2vecOutFile;
    }

    // generate syntax feature
    public static void generateSyntaxFeatureFiles(File word2vecFile, File dot2astFile) {
//        String word2vecPath = "/mnt/share/FCDetector/AutoenCODE/out/word2vec/word2vec.out";
//        File word2vecFile = new File(word2vecPath);
        Map<String, List<Double>> identifier2Vec = Tool.getIdentifier2Vec(word2vecFile);

        // ast
        Map<String, List<Double>> src2AstVec = new HashMap<>();
//        File dot2astFile = new File("/mnt/share/CloneData/data/dot2ast.txt");
        List<String> dot2astList = null;
        try {
            dot2astList = FileUtils.readLines(dot2astFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, List<Double>> subPath2AstVec = new HashMap<>();
        assert dot2astList != null;
        for (String line : dot2astList) {
            File dotFile = new File(line.split(" ")[0]);
            File astFile = new File(line.split(" ")[1]);
            String subPath = Tool.getSrcPath(dotFile);

            String astIdentifiers = null;
            try {
                astIdentifiers = FileUtils.readFileToString(astFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert astIdentifiers != null;
            List<Double> astVec = Tool.getAstVecForFeature(astIdentifiers, identifier2Vec);
            subPath2AstVec.put(subPath, astVec);
        }

        for (Map.Entry<String, List<Double>> entry : subPath2AstVec.entrySet()) {
            File syntaxFeatureFolder = new File(PathConfig.SYNTAX_FEATURE_FOLDER_PATH + File.separator + entry.getKey());
            if (!syntaxFeatureFolder.exists()) {
                syntaxFeatureFolder.mkdirs();
            }
            File syntaxFeatureFile = new File(syntaxFeatureFolder.getAbsolutePath() + File.separator + "syntax.txt");
            if (syntaxFeatureFile.exists()) {
                syntaxFeatureFile.delete();
            }
            try {
                FileUtils.write(syntaxFeatureFile, entry.getValue().toString(), StandardCharsets.UTF_8, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static HashMap<String, List<Double>> getAllVec(File word2vecOutFile) {
        HashMap<String, List<Double>> map = new HashMap<>();
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(word2vecOutFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert lines != null;
        for (String line : lines) {
            String[] cols = line.split(" ");
            List<Double> valueList = new ArrayList<>();
            for (int i = 1; i < cols.length; i++) {
                valueList.add(Double.parseDouble(cols[i]));
            }
            map.put(cols[0], valueList);
        }
        return map;
    }


    private static List<File> getFeatureVecFileListBySourceFile(File sourceFile) {
        List<File> res = new ArrayList<>();

        String folderAndFilePath = Tool.getFolderAndFilePath(sourceFile);
        File vecFolder = new File(PathConfig.EMBEDDING_FEATURE_WORD2VEC_PATH + File.separator + folderAndFilePath);
        if (!vecFolder.exists()) {
            return null;
        }
        File[] files = vecFolder.listFiles();
        assert files != null;
        Collections.addAll(res, files);
        return res;
    }

    private static List<File> getFuncVecFileListBySourceFile(File sourceFile) {
        List<File> res = new ArrayList<>();

        String folderAndFilePath = Tool.getFolderAndFilePath(sourceFile);
        File[] files = new File(PathConfig.EMBEDDING_FUNC_WORD2VEC_PATH + File.separator + folderAndFilePath).listFiles();
        assert files != null;
        Collections.addAll(res, files);
        return res;
    }

    public static List<File> getEmbeddingFileListBySourceFile(File sourceFile) {
        String folderAndFilePath = Tool.getFolderAndFilePath(sourceFile);
        File[] embedFiles = new File(PathConfig.IDENT_EMBED_PATH + File.separator + folderAndFilePath).listFiles();
        List<File> res = new ArrayList<>();
        assert embedFiles != null;
        Collections.addAll(res, embedFiles);
        return res;
    }

    public static List<File> getWord2VecBySourceFile(File sourceFile) {
        List<File> featureList = getFeatureVecFileListBySourceFile(sourceFile);
        if (featureList != null) {
            return featureList;
        }
        return getFuncVecFileListBySourceFile(sourceFile);
    }

    public static List<Double> getVecFromEmbeddingFile(File embeddingFile) {
        List<Double> res = new ArrayList<>();
        String s = null;
        try {
            s = FileUtils.readFileToString(embeddingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert s != null;
        String[] cols = s.split(" ");
        for (String col : cols) {
            if (col.trim().isEmpty()) {
                continue;
            }
            res.add(Double.parseDouble(col));
        }

        return res;
    }

}
