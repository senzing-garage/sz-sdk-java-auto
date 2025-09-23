package com.senzing.sdk.core.perpetual;

import com.senzing.sdk.*;
import com.senzing.sdk.core.*;

import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_ALL_FEATURES;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_ENTITY_NAME;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_INTERNAL_FEATURES;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_DATA;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_FEATURES;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_JSON_DATA;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_MATCHING_INFO;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_TYPES;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_UNMAPPED_DATA;
import static com.senzing.sdk.SzFlag.SZ_INCLUDE_FEATURE_SCORES;
import static com.senzing.sdk.SzFlag.SZ_INCLUDE_MATCH_KEY_DETAILS;
import static com.senzing.sdk.SzFlag.SZ_ENTITY_INCLUDE_RECORD_SUMMARY;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.EnumSet;

public class WindowsAccessViolation {
    private static final Integer CONCURRENCY = 4;


    private static final Duration DURATION = Duration.ofMillis(500);

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
            String entityJson = engine.getEntity(
                recordKey, EnumSet.of(SZ_ENTITY_INCLUDE_ALL_FEATURES,
                                      SZ_ENTITY_INCLUDE_ENTITY_NAME,
                                      SZ_ENTITY_INCLUDE_RECORD_SUMMARY,
                                      SZ_ENTITY_INCLUDE_RECORD_TYPES,
                                      SZ_ENTITY_INCLUDE_RECORD_DATA,
                                      SZ_ENTITY_INCLUDE_RECORD_JSON_DATA,
                                      SZ_ENTITY_INCLUDE_RECORD_MATCHING_INFO,
                                      SZ_ENTITY_INCLUDE_RECORD_UNMAPPED_DATA,
                                      SZ_ENTITY_INCLUDE_RECORD_FEATURES,
                                      SZ_ENTITY_INCLUDE_INTERNAL_FEATURES,
                                      SZ_INCLUDE_MATCH_KEY_DETAILS,
                                      SZ_INCLUDE_FEATURE_SCORES));
            env.destroy();
            env = null;

            env = SzPerpetualCoreEnvironment.newPerpetualBuilder()
                    .settings(settings).verboseLogging(false).concurrency(CONCURRENCY)
                    .configRefreshPeriod(DURATION).build();
            
            SzDiagnostic diagnostic = env.getDiagnostic()
            ;
            System.out.println(diagnostic.getRepositoryInfo());


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (env != null) {
                env.destroy();
            }
        }
    }
}