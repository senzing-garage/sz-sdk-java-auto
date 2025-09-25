package com.senzing.sdk.core.perpetual;

import com.senzing.sdk.*;
import com.senzing.sdk.core.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class WindowsAccessViolation {
    private static final Integer CONCURRENCY = 4;


    private static final Duration DURATION = Duration.ofMillis(510);

    public static void main(String[] args) {
        SzEnvironment env = null;

        if (args.length < 1) {
            System.err.println("Must specify the settings path");
            System.exit(1);
        }
        File senzingDir = new File(args[0]);
        if (!senzingDir.exists() || !senzingDir.isDirectory()) {
            System.err.println("Senzing path is invalid: " + senzingDir);
            System.exit(1);
        }
        File erDir = new File(senzingDir, "er");
        if (!erDir.exists() || !erDir.isDirectory()) {
            System.err.println("Senzing path is invalid (er directory not found): " + senzingDir);
            System.exit(1);
        }
        File configPath = new File(erDir, "etc");
        File supportDir = new File(senzingDir, "data");
        File resourceDir = new File(erDir, "resources");

        File templateDir = new File(resourceDir, "templates");
        File dbTemplateFile = new File(templateDir, "G2C.db");

        try {

            File mainDatabaseFile = File.createTempFile("G2C-", ".db");
            File resDatabaseFile = File.createTempFile("G2-", "-RES.db");
            File featDatabaseFile = File.createTempFile("G2-", "-LIB_FEAT.db");
            Files.copy(dbTemplateFile.toPath(), 
                       mainDatabaseFile.toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
            Files.copy(dbTemplateFile.toPath(), 
                       resDatabaseFile.toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
            Files.copy(dbTemplateFile.toPath(), 
                       featDatabaseFile.toPath(),
                       StandardCopyOption.REPLACE_EXISTING);

            String settings = """
                    {
                        "PIPELINE": {
                            "SUPPORTPATH": "%s",
                            "RESOURCEPATH": "%s",
                            "CONFIGPATH": "%s"
                        },
                        "SQL": {
                            "BACKEND": "HYBRID",
                            "CONNECTION": "sqlite3://na:na@/%s"
                        },
                        "HYBRID": {
                            "RES_FEAT": "C1",
                            "RES_FEAT_EKEY": "C1",
                            "RES_FEAT_LKEY": "C1",
                            "RES_FEAT_STAT": "C1",
                            "LIB_FEAT": "C2",
                            "LIB_FEAT_HKEY": "C2"
                        },
                        "C1": {
                            "CLUSTER_SIZE": "1",
                            "DB_1": "sqlite3://na:na@/%s"
                        },
                        "C2": {
                            "CLUSTER_SIZE": "1",
                            "DB_1": "sqlite3://na:na@/%s"
                        }

                    }
                    """.formatted(supportDir.getCanonicalPath().replace('\\', '/'),
                    resourceDir.getCanonicalPath().replace('\\', '/'),
                    configPath.getCanonicalPath().replace('\\', '/'),
                    mainDatabaseFile.getCanonicalPath().replace('\\', '/'),
                    resDatabaseFile.getCanonicalPath().replace('\\', '/'),
                    featDatabaseFile.getCanonicalPath().replace('\\', '/'));

    	    System.out.println();
            System.out.println(settings);
            System.out.println();

            env = SzCoreEnvironment.newBuilder().settings(settings).verboseLogging(false).build();

            SzConfigManager configMgr = env.getConfigManager();
            SzConfig config = configMgr.createConfig();
            configMgr.setDefaultConfig(config.export());

            SzEngine engine = env.getEngine();
            String testRecord = """
                    {
                        "DATA_SOURCE": "TEST",
                        "RECORD_ID": "ABC123",
                        "NAME_FULL": "Joe Schmoe",
                        "EMAIL_ADDRESS": "joeschmoe@nowhere.com",
                        "PHONE_NUMBER": "702-555-1212"
                    }
                    """;
            SzRecordKey recordKey = SzRecordKey.of("TEST", "ABC123");
            engine.addRecord(recordKey, testRecord);
            // process the redo records
            for (String redo = engine.getRedoRecord();
                    redo != null;
                    redo = engine.getRedoRecord()) 
            {
                engine.processRedoRecord(redo);
            }
            engine.getEntity(recordKey);
            env.destroy();
            env = null;

            env = SzPerpetualCoreEnvironment.newPerpetualBuilder()
                    .settings(settings).verboseLogging(false).concurrency(CONCURRENCY)
                    .configRefreshPeriod(DURATION).build();
            
            for (int index = 0; index < 100; index++) {
                SzDiagnostic diagnostic = env.getDiagnostic();
            
                diagnostic.getRepositoryInfo();
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ignore) {
                    // do nothing
                }
            }


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (env != null) {
                env.destroy();
            }
        }
    }
}